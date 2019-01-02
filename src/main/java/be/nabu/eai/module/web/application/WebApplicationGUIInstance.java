package be.nabu.eai.module.web.application;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.scene.control.Tab;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.AnchorPane;
import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.api.ArtifactGUIInstanceWithChildren;
import be.nabu.eai.developer.api.CRUDArtifactGUIInstance;
import be.nabu.eai.developer.managers.base.BaseArtifactGUIInstance;
import be.nabu.eai.developer.managers.base.BaseGUIManager;
import be.nabu.eai.module.web.application.WebApplicationGUIManager.EditingTab;
import be.nabu.eai.repository.api.Entry;
import be.nabu.jfx.control.ace.AceEditor;
import be.nabu.jfx.control.tree.TreeItem;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.resources.ResourceUtils;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.features.CacheableResource;
import be.nabu.libs.validator.api.Validation;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;

public class WebApplicationGUIInstance<T extends Artifact> extends BaseArtifactGUIInstance<T> implements CRUDArtifactGUIInstance, ArtifactGUIInstanceWithChildren {

	private Logger logger = LoggerFactory.getLogger(getClass());
	private ObjectProperty<EditingTab> editingTab;

	public WebApplicationGUIInstance(BaseGUIManager<T, ?> baseGuiManager, Entry entry, ObjectProperty<EditingTab> editingTab) {
		super(baseGuiManager, entry);
		this.editingTab = editingTab;
	}

	// if something has been created, we want to refresh the parent location in the tree
	@Override
	public void created(String id, String message, String content) {
		resetCache(getParentPath(id));
	}

	private Resource getResource(String path) {
		// all paths will begin with a "web/" because that has been updated
		try {
			return ResourceUtils.resolve(editingTab.get().getRoot(), path.substring("web/".length()));
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private Resource resetCache(String path) {
		Resource resource = getResource(path);
		if (resource instanceof CacheableResource) {
			try {
				((CacheableResource) resource).resetCache();
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		// skip the root, we are resolving in it
		TreeItem<Resource> resolve = editingTab.get().getTree().resolve(path.substring("web/".length()));
		if (resolve != null) {
			Platform.runLater(new Runnable() {
				public void run() {
					editingTab.get().getTree().getTreeCell(resolve).refresh();
				}
			});
		}
		return resource;
	}
	
	private String getParentPath(String id) {
		// skip the last bit, that is the one that was created
		return id.replaceAll("/[^/]+$", "");
	}
	
	private String getPath(String id) {
		return id;
	}

	// if something was updated, we need to reset the cache of the filesystem (if any) to pick up the changes
	// and then refresh any tabs that were dealing with it
	@Override
	public void updated(String id, String message, String content) {
		String path = getPath(id);
		Resource resetCache = resetCache(path);
		// now check if you have a tab open with that content
		for (Tab tab : editingTab.get().getEditors().getTabs()) {
			if (tab.getId().equals(path)) {
				Platform.runLater(new Runnable() {
					public void run() {
						try {
							AceEditor editor = (AceEditor) tab.getUserData();
							ReadableContainer<ByteBuffer> readable = ((ReadableResource) resetCache).getReadable();
							try {
								editor.setContent(resetCache.getContentType(), new String(IOUtils.toBytes(readable), Charset.forName("UTF-8")));
							}
							finally {
								readable.close();
							}
						}
						catch (Exception e) {
							logger.warn("Could not update tab for: " + path);
						}
					}
				});
			}
		}
	}

	@Override
	public void deleted(String id, String message) {
		String path = getPath(id);
		resetCache(getParentPath(id));
		for (Tab tab : editingTab.get().getEditors().getTabs()) {
			if (tab.getId().equals(path)) {
				Platform.runLater(new Runnable() {
					public void run() {
						editingTab.get().getEditors().getTabs().remove(tab);
					}
				});
				break;
			}
		}
	}

	@Override
	public void refresh(AnchorPane pane) {
		// currently we assume nothing can actually change to the files except changes you have done yourself
		// this means we do not need to reset the cache of the filesystem
		// external changes should be synced
		// note that the configuration tab will not update but very few things are susceptible to external change apart from configuration at the bottom
		// and that is already a bit wonky anyway...
		// to be revisited perhaps
	}

	@Override
	public List<Validation<?>> saveChildren() throws IOException {
		for (Tab tab : editingTab.get().getEditors().getTabs()) {
			AceEditor editor = (AceEditor) tab.getUserData();
			String lockId = getArtifact().getId() + ":" + tab.getId();
			// trigger a save if we have the lock
			if (MainController.getInstance().hasLock(lockId).get()) {
				editor.trigger(AceEditor.SAVE);
			}
		}
		return new ArrayList<Validation<?>>();
	}

}
