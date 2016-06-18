package be.nabu.eai.module.web.application;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.managers.base.BaseConfigurationGUIManager;
import be.nabu.eai.developer.managers.base.BaseJAXBGUIManager;
import be.nabu.eai.developer.managers.util.SimplePropertyUpdater;
import be.nabu.eai.developer.util.Confirm;
import be.nabu.eai.developer.util.Confirm.ConfirmType;
import be.nabu.eai.developer.util.ElementTreeItem;
import be.nabu.eai.module.web.application.AceEditor.Action;
import be.nabu.eai.module.web.application.WebConfiguration.WebConfigurationPart;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.jfx.control.tree.Tree;
import be.nabu.jfx.control.tree.TreeCell;
import be.nabu.jfx.control.tree.TreeItem;
import be.nabu.jfx.control.tree.TreeUtils;
import be.nabu.jfx.control.tree.TreeUtils.TreeItemCreator;
import be.nabu.libs.converter.ConverterFactory;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.resources.VirtualContainer;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.base.ComplexElementImpl;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;

public class WebApplicationGUIManager extends BaseJAXBGUIManager<WebApplicationConfiguration, WebApplication> {

	public WebApplicationGUIManager() {
		super("Web Application", WebApplication.class, new WebApplicationManager(), WebApplicationConfiguration.class);
	}

	@Override
	protected List<Property<?>> getCreateProperties() {
		return null;
	}

	@Override
	protected WebApplication newInstance(MainController controller, RepositoryEntry entry, Value<?>...values) throws IOException {
		return new WebApplication(entry.getId(), entry.getContainer(), entry.getRepository());
	}

	@Override
	public String getCategory() {
		return "Web";
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void display(MainController controller, AnchorPane pane, final WebApplication artifact) {
		VBox vbox = new VBox();
		AnchorPane anchor = new AnchorPane();
		display(artifact, anchor);
		vbox.getChildren().addAll(anchor);
		
		// add the configuration per fragment
		try {
			WebConfiguration webConfiguration = artifact.getFragmentConfiguration();
			Set<String> allPaths = new HashSet<String>();
			Set<String> allTypes = new HashSet<String>();
			// saves a lookup later on
			Map<String, ComplexType> typeDefinitions = new HashMap<String, ComplexType>();
			if (artifact.getConfiguration().getWebFragments() != null) {
				for (WebFragment fragment : artifact.getConfiguration().getWebFragments()) {
					for (final WebFragmentConfiguration fragmentConfiguration : fragment.getFragmentConfiguration()) {
						String typeId = ((DefinedType) fragmentConfiguration.getType()).getId();
						typeDefinitions.put(typeId, fragmentConfiguration.getType());
						String path = fragmentConfiguration.getPath();
						if (artifact.getConfiguration().getPath() != null && !artifact.getConfiguration().getPath().isEmpty() && !"/".equals(artifact.getConfiguration().getPath())) {
							path = artifact.getConfiguration().getPath().replaceFirst("[/]+$", "") + "/" + path.replaceFirst("^[/]+", "");
							if (!path.startsWith("/")) {
								path = "/" + path;
							}
						}
						allPaths.add(path);
						allTypes.add(typeId);
						boolean found = false;
						WebConfigurationPart typeMatch = null;
						for (WebConfigurationPart configuration : webConfiguration.getParts()) {
							if (configuration.getType().equals(typeId)) {
								typeMatch = configuration;
								if (configuration.getPaths().contains(path)) {
									found = true;
									break;
								}
							}
						}
						// did not find configuration for this fragment
						if (!found) {
							// found a similar type configuration, use that
							if (typeMatch != null) {
								typeMatch.getPaths().add(path);
							}
							// add a new one
							else {
								WebConfigurationPart newPart = new WebConfigurationPart();
								newPart.setType(typeId);
								newPart.getPaths().add(path);
								webConfiguration.getParts().add(newPart);
							}
						}
					}
				}
				// remove unused paths and/or types
				Iterator<WebConfigurationPart> iterator = webConfiguration.getParts().iterator();
				while (iterator.hasNext()) {
					WebConfigurationPart next = iterator.next();
					if (!allTypes.contains(next.getType())) {
						iterator.remove();
					}
					else {
						next.getPaths().retainAll(allPaths);
					}
				}
				
				for (final WebConfigurationPart configuration : webConfiguration.getParts()) {
					VBox box = new VBox();
					Label label = new Label(configuration.getType());
					StringBuilder pathBuilder = new StringBuilder();
					for (String path : configuration.getPaths()) {
						if (!pathBuilder.toString().isEmpty()) {
							pathBuilder.append(", ");
						}
						pathBuilder.append(path);
					}
					label.setTooltip(new Tooltip(pathBuilder.toString()));
					box.getChildren().addAll(new Separator(Orientation.HORIZONTAL), label);
					List<Property<?>> properties = BaseConfigurationGUIManager.createProperty(new ComplexElementImpl(typeDefinitions.get(configuration.getType()), null));
					List<Value<?>> values = new ArrayList<Value<?>>();
					for (Property<?> property : properties) {
						if (configuration.getConfiguration().containsKey(property.getName())) {
							values.add(new ValueImpl(property, ConverterFactory.getInstance().getConverter().convert(configuration.getConfiguration().get(property.getName()), property.getValueClass())));
						}
					}
					AnchorPane childPane = new AnchorPane();
					SimplePropertyUpdater updater = new SimplePropertyUpdater(true, new LinkedHashSet<Property<?>>(properties), values.toArray(new Value[values.size()]));
					updater.valuesProperty().addListener(new ListChangeListener<Value<?>>() {
						@Override
						public void onChanged(ListChangeListener.Change<? extends Value<?>> change) {
							while (change.next()) {
								if (change.wasRemoved()) {
									for (Value<?> value : change.getRemoved()) {
										configuration.getConfiguration().remove(value.getProperty().getName());
									}
								}
								if (change.wasAdded()) {
									for (Value value : change.getAddedSubList()) {
										configuration.getConfiguration().put(value.getProperty().getName(), ConverterFactory.getInstance().getConverter().convert(value.getValue(), String.class));
									}
								}
								if (change.wasUpdated() || change.wasReplaced()) {
									for (Value value : change.getList()) {
										configuration.getConfiguration().put(value.getProperty().getName(), ConverterFactory.getInstance().getConverter().convert(value.getValue(), String.class));
									}
								}
							}
						}
					});
					MainController.getInstance().showProperties(
						updater, 
						childPane, 
						true, 
						artifact.getRepository()
					);
					box.getChildren().add(childPane);
					vbox.getChildren().add(box);
				}
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		ScrollPane scroll = new ScrollPane();
		scroll.setContent(vbox);
		vbox.prefWidthProperty().bind(scroll.widthProperty().subtract(100));
		
		TabPane tabs = new TabPane();
		tabs.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		tabs.setSide(Side.RIGHT);
		Tab tab = new Tab("Configuration");
		tab.setContent(scroll);
		tab.setClosable(false);
		tabs.getTabs().add(tab);
		try {
			tabs.getTabs().add(buildEditingTab(artifact));
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		AnchorPane.setLeftAnchor(tabs, 0d);
		AnchorPane.setRightAnchor(tabs, 0d);
		AnchorPane.setTopAnchor(tabs, 0d);
		AnchorPane.setBottomAnchor(tabs, 0d);
		
		pane.getChildren().add(tabs);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Tab buildEditingTab(final WebApplication artifact) throws URISyntaxException, IOException {
		Tab tab = new Tab("Resources");
		Tree<Resource> tree = new Tree<Resource>();
		
		ResourceContainer<?> publicDirectory = (ResourceContainer<?>) artifact.getDirectory().getChild(EAIResourceRepository.PUBLIC);
		if (publicDirectory == null && artifact.getDirectory() instanceof ManageableContainer) {
			publicDirectory = (ResourceContainer<?>) ((ManageableContainer<?>) artifact.getDirectory()).create(EAIResourceRepository.PUBLIC, Resource.CONTENT_TYPE_DIRECTORY);
		}
		ResourceContainer<?> privateDirectory = (ResourceContainer<?>) artifact.getDirectory().getChild(EAIResourceRepository.PRIVATE);
		if (privateDirectory == null && artifact.getDirectory() instanceof ManageableContainer) {
			privateDirectory = (ResourceContainer<?>) ((ManageableContainer<?>) artifact.getDirectory()).create(EAIResourceRepository.PRIVATE, Resource.CONTENT_TYPE_DIRECTORY);
		}
		
		VirtualContainer container = new VirtualContainer(null, "web");
		if (publicDirectory != null) {
			container.addChild(publicDirectory.getName(), publicDirectory);
		}
		if (privateDirectory != null) {
			container.addChild(privateDirectory.getName(), privateDirectory);
		}
		
		tree.rootProperty().set(new ResourceTreeItem(null, container));

		final TabPane editors = new TabPane();
		editors.setTabClosingPolicy(TabClosingPolicy.ALL_TABS);
		editors.setSide(Side.BOTTOM);
		
		tree.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeCell<Resource>>() {
			@Override
			public void changed(ObservableValue<? extends TreeCell<Resource>> observable, TreeCell<Resource> oldValue, TreeCell<Resource> newValue) {
				// TODO: load data from resource
				if (newValue != null) {
					String path = TreeUtils.getPath(newValue.getItem());
					boolean found = false;
					for (Tab tab : editors.getTabs()) {
						if (tab.getId().equals(path)) {
							editors.getSelectionModel().select(tab);
							found = true;
							break;
						}
					}
					if (!found) {
						final Resource resource = newValue.getItem().itemProperty().get();
						if (resource instanceof ReadableResource) {
							final Tab tab = new Tab(path);
							tab.setId(path);
							editors.getTabs().add(tab);
							final AceEditor aceEditor = new AceEditor(MainController.getInstance().getStage());
							try {
								ReadableContainer<ByteBuffer> readable = ((ReadableResource) resource).getReadable();
								try {
									byte[] bytes = IOUtils.toBytes(readable);
									aceEditor.setContent(resource.getContentType(), new String(bytes, "UTF-8"));
								}
								finally {
									readable.close();
								}
							}
							catch (IOException e) {
								e.printStackTrace();
							}
							tab.setContent(aceEditor.getWebView());
							editors.getSelectionModel().select(tab);
							aceEditor.subscribe(Action.CLOSE, new EventHandler<Event>() {
								@Override
								public void handle(Event event) {
									if (tab.getText().endsWith("*")) {
										Confirm.confirm(ConfirmType.QUESTION, "Unsaved Changes", "Do you want to discard all pending changes?", new EventHandler<ActionEvent>() {
											@Override
											public void handle(ActionEvent arg0) {
												editors.getTabs().remove(tab);
											}
										});
									}
									else {
										editors.getTabs().remove(tab);
									}
								}
							});
							aceEditor.subscribe(Action.CHANGE, new EventHandler<Event>() {
								@Override
								public void handle(Event arg0) {
									if (!tab.getText().endsWith("*")) {
										tab.setText(tab.getText() + " *");
									}
								}
							});
							aceEditor.subscribe(Action.SAVE, new EventHandler<Event>() {
								@Override
								public void handle(Event arg0) {
									if (resource instanceof WritableResource) {
										try {
											WritableContainer<ByteBuffer> writable = ((WritableResource) resource).getWritable();
											try {
												writable.write(IOUtils.wrap(aceEditor.getContent().getBytes("UTF-8"), true));
											}
											finally {
												writable.close();
											}
										}
										catch (IOException e) {
											throw new RuntimeException(e);
										}
										// remove trailing *
										if (tab.getText().endsWith("*")) {
											tab.setText(tab.getText().replace("[\\s]*\\*$", ""));
										}
									}
								}
							});
						}
					}
				}
			}
		});
		
		SplitPane split = new SplitPane();
		split.setOrientation(Orientation.HORIZONTAL);
		split.getItems().add(tree);
		split.getItems().add(editors);
		SplitPane.setResizableWithParent(tree, false);
		
		tree.setPrefWidth(250);
		tree.maxWidthProperty().bind(split.widthProperty());
		
		AnchorPane.setLeftAnchor(split, 0d);
		AnchorPane.setRightAnchor(split, 0d);
		AnchorPane.setTopAnchor(split, 0d);
		AnchorPane.setBottomAnchor(split, 0d);
		
		tab.setContent(split);
		return tab;
	}
	
	public static class ResourceTreeItem implements TreeItem<Resource> {

		private BooleanProperty editableProperty = new SimpleBooleanProperty(false);
		private ObjectProperty<Resource> itemProperty = new SimpleObjectProperty<Resource>();
		private ObjectProperty<Node> graphicProperty = new SimpleObjectProperty<Node>();
		private BooleanProperty leafProperty = new SimpleBooleanProperty(false);
		private ObservableList<TreeItem<Resource>> children = FXCollections.observableArrayList();
		private TreeItem<Resource> parent;

		public ResourceTreeItem(TreeItem<Resource> parent, Resource resource) {
			this.parent = parent;
			if (resource instanceof ResourceContainer) {
				graphicProperty.set(MainController.loadGraphic("folder.png"));
			}
			else {
				graphicProperty.set(MainController.loadGraphic(resource.getContentType() == null ? "mime/text-plain.png" : "mime/" + resource.getContentType().replaceAll("[^\\w]+", "-") + ".png"));
			}
			leafProperty.set(!(resource instanceof ResourceContainer));
			editableProperty.set(parent instanceof ManageableContainer);
			itemProperty.set(resource);
			refresh(true);
		}
		
		@Override
		public void refresh() {
			refresh(true);			
		}
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		private void refresh(boolean includeChildren) {
			if (!leafProperty.get() && includeChildren) {
				Collection<Resource> collection = toCollection(((ResourceContainer) itemProperty.get()).iterator());
				TreeUtils.refreshChildren(new TreeItemCreator<Resource>() {
					@Override
					public TreeItem<Resource> create(TreeItem<Resource> parent, Resource child) {
						System.out.println("Creating tree item for child: " + child);
						return new ResourceTreeItem(parent, child);
					}
				}, this, collection);
			}
		}
		
		public static <T> Collection<T> toCollection(Iterator<T> iterator) {
			List<T> list = new ArrayList<T>();
			while (iterator.hasNext()) {
				list.add(iterator.next());
			}
			return list;
		}

		@Override
		public BooleanProperty editableProperty() {
			return editableProperty;
		}

		@Override
		public BooleanProperty leafProperty() {
			return leafProperty;
		}

		@Override
		public ObjectProperty<Resource> itemProperty() {
			return itemProperty;
		}

		@Override
		public ObjectProperty<Node> graphicProperty() {
			return graphicProperty;
		}

		@Override
		public ObservableList<TreeItem<Resource>> getChildren() {
			return children;
		}

		@Override
		public TreeItem<Resource> getParent() {
			return parent;
		}

		@Override
		public String getName() {
			return itemProperty.get().getName();
		}
		
	}
}
