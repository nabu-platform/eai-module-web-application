/*
* Copyright (C) 2016 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

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
import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.api.ArtifactGUIInstanceWithChildren;
import be.nabu.eai.developer.api.CRUDArtifactGUIInstance;
import be.nabu.eai.developer.managers.base.BaseArtifactGUIInstance;
import be.nabu.eai.developer.managers.base.BaseGUIManager;
import be.nabu.eai.module.web.application.WebApplicationGUIManager.EditingTab;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.jfx.control.ace.AceEditor;
import be.nabu.jfx.control.tree.TreeItem;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.resources.ResourceUtils;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
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
		// there is a very tiny window where you can get NPE while the editing tab is loading
		// in some cases this can take a while for reasons as of yet unknown
		if (editingTab.get() != null) {
			resetCache(getParentPath(id));
		}
		else {
			reloadRoot();
		}
	}
	
	private void reloadRoot() {
		if (getEntry() instanceof ResourceEntry) {
			ResourceContainer<?> container = ((ResourceEntry) getEntry()).getContainer();
			if (container instanceof CacheableResource) {
				try {
					((CacheableResource) container).resetCache();
				}
				catch (IOException e) {
					logger.warn("Could not reload cache", e);
				}
			}
		}
	}

	private Resource getResource(String path) {
		// all paths will begin with a "web/" because that has been updated (_if_ we are using developer which creates a virtual web container)
		try {
			if (path.startsWith("web/")) {
				path = path.substring("web/".length());
			}
			return ResourceUtils.resolve(editingTab.get().getRoot(), path);
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
		if (path.startsWith("web/")) {
			path = path.substring("web/".length());
		}
		// skip the root, we are resolving in it
		TreeItem<Resource> resolve = editingTab.get().getTree().resolve(path, false);
		if (resolve != null) {
			Platform.runLater(new Runnable() {
				public void run() {
					editingTab.get().getTree().getTreeCell(resolve).refresh();
				}
			});
		}
		// if we can't find the target, do the root...
		// it seems if you haven't opened the resources tab yet and someone else triggers a refresh, you then go to the tab, it might fail
		else {
			Platform.runLater(new Runnable() {
				public void run() {
					editingTab.get().getTree().getRootCell().refresh();
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
		if (editingTab.get() != null) {
			String path = getPath(id);
			Resource resetCache = resetCache(path);
			// now check if you have a tab open with that content
			for (Tab tab : editingTab.get().getEditors().getTabs()) {
				if (tab.getId().equals(path) || (!path.startsWith("web/") && tab.getId().equals("web/" + path))) {
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
		else {
			reloadRoot();
		}
	}

	@Override
	public void deleted(String id, String message) {
		if (editingTab.get() != null) {
			String path = getPath(id);
			resetCache(getParentPath(id));
			for (Tab tab : editingTab.get().getEditors().getTabs()) {
				if (tab.getId().equals(path) || (!path.startsWith("web/") && tab.getId().equals("web/" + path))) {
					Platform.runLater(new Runnable() {
						public void run() {
							editingTab.get().getEditors().getTabs().remove(tab);
						}
					});
					break;
				}
			}
		}
		else {
			reloadRoot();
		}
	}

//	@Override
//	public void refresh(AnchorPane pane) {
//		// currently we assume nothing can actually change to the files except changes you have done yourself
//		// this means we do not need to reset the cache of the filesystem
//		// external changes should be synced
//		// note that the configuration tab will not update but very few things are susceptible to external change apart from configuration at the bottom
//		// and that is already a bit wonky anyway...
//		// to be revisited perhaps
//	}

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
