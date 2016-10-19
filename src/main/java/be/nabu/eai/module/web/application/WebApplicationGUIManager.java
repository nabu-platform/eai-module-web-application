package be.nabu.eai.module.web.application;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import be.nabu.eai.authentication.api.PasswordAuthenticator;
import be.nabu.eai.authentication.api.SecretAuthenticator;
import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.managers.base.BaseConfigurationGUIManager;
import be.nabu.eai.developer.managers.base.BaseJAXBGUIManager;
import be.nabu.eai.developer.managers.util.SimpleProperty;
import be.nabu.eai.developer.managers.util.SimplePropertyUpdater;
import be.nabu.eai.developer.util.Confirm;
import be.nabu.eai.developer.util.Confirm.ConfirmType;
import be.nabu.eai.developer.util.EAIDeveloperUtils;
import be.nabu.eai.developer.util.Find;
import be.nabu.eai.module.web.application.WebConfiguration.WebConfigurationPart;
import be.nabu.eai.repository.EAIRepositoryUtils;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.Translator;
import be.nabu.eai.repository.api.UserLanguageProvider;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.jfx.control.tree.Marshallable;
import be.nabu.jfx.control.tree.RemovableTreeItem;
import be.nabu.jfx.control.tree.Tree;
import be.nabu.jfx.control.tree.TreeCell;
import be.nabu.jfx.control.tree.TreeItem;
import be.nabu.jfx.control.tree.TreeUtils;
import be.nabu.jfx.control.tree.TreeUtils.TreeItemCreator;
import be.nabu.jfx.control.tree.Updateable;
import be.nabu.libs.authentication.api.DeviceValidator;
import be.nabu.libs.authentication.api.PermissionHandler;
import be.nabu.libs.authentication.api.RoleHandler;
import be.nabu.libs.authentication.api.TokenValidator;
import be.nabu.libs.converter.ConverterFactory;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.resources.ResourceUtils;
import be.nabu.libs.resources.VirtualContainer;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.libs.resources.api.features.CacheableResource;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.base.ComplexElementImpl;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.validator.api.ValidationMessage;
import be.nabu.libs.validator.api.ValidationMessage.Severity;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.Container;
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
					if (fragment != null) {
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
				}
			}
			
			Map<Method, List<Element<?>>> extensions = getExtensions(artifact);
			for (Method method : extensions.keySet()) {
				for (Element<?> element : extensions.get(method)) {
					if (element.getType() instanceof ComplexType && element.getType() instanceof DefinedType) {
						String typeId = ((DefinedType) element.getType()).getId();
						typeDefinitions.put(typeId, (ComplexType) element.getType());
						String path = "$" + method.toString();
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
	
	private Map<Method, List<Element<?>>> getExtensions(WebApplication artifact) {
		Map<Method, List<Element<?>>> extensions = new HashMap<Method, List<Element<?>>>();
		
		// password authenticator
		if (artifact.getConfig().getPasswordAuthenticationService() != null) {
			Method method = WebApplication.getMethod(PasswordAuthenticator.class, "authenticate");
			extensions.put(method, EAIRepositoryUtils.getInputExtensions(artifact.getConfig().getPasswordAuthenticationService(), method));
		}
		
		// secret authenticator
		if (artifact.getConfig().getSecretAuthenticationService() != null) {
			Method method = WebApplication.getMethod(SecretAuthenticator.class, "authenticate");
			extensions.put(method, EAIRepositoryUtils.getInputExtensions(artifact.getConfig().getSecretAuthenticationService(), method));
		}
		
		// token validator
		if (artifact.getConfig().getTokenValidatorService() != null) {
			Method method = WebApplication.getMethod(TokenValidator.class, "isValid");
			extensions.put(method, EAIRepositoryUtils.getInputExtensions(artifact.getConfig().getTokenValidatorService(), method));
		}
		
		// language provider
		if (artifact.getConfig().getLanguageProviderService() != null) {
			Method method = WebApplication.getMethod(UserLanguageProvider.class, "getLanguage");
			extensions.put(method, EAIRepositoryUtils.getInputExtensions(artifact.getConfig().getLanguageProviderService(), method));
		}
		
		// role handler
		if (artifact.getConfig().getRoleService() != null) {
			Method method = WebApplication.getMethod(RoleHandler.class, "hasRole");
			extensions.put(method, EAIRepositoryUtils.getInputExtensions(artifact.getConfig().getRoleService(), method));
		}
		
		// permission handler
		if (artifact.getConfig().getPermissionService() != null) {
			Method method = WebApplication.getMethod(PermissionHandler.class, "hasPermission");
			extensions.put(method, EAIRepositoryUtils.getInputExtensions(artifact.getConfig().getPermissionService(), method));
		}
		
		// device validator
		if (artifact.getConfig().getDeviceValidatorService() != null) {
			Method method = WebApplication.getMethod(DeviceValidator.class, "isAllowed");
			extensions.put(method, EAIRepositoryUtils.getInputExtensions(artifact.getConfig().getDeviceValidatorService(), method));
		}
		
		// translator
		if (artifact.getConfig().getTranslationService() != null) {
			Method method = WebApplication.getMethod(Translator.class, "translate");
			extensions.put(method, EAIRepositoryUtils.getInputExtensions(artifact.getConfig().getTranslationService(), method));
		}
		
		return extensions;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Tab buildEditingTab(final WebApplication artifact) throws IOException, URISyntaxException {
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
		return buildEditingTab(container);
	}
	
	@SuppressWarnings({ "rawtypes" })
	public static Tab buildEditingTab(ResourceContainer<?> container) throws URISyntaxException, IOException {
		Tab tab = new Tab("Resources");
		Tree<Resource> tree = new Tree<Resource>(new Marshallable<Resource>() {
			@Override
			public String marshal(Resource instance) {
				return instance == null ? null : instance.getName();
			}
		}, new Updateable<Resource>() {
			@Override
			public Resource update(TreeCell<Resource> treeCell, String text) {
				try {
					Resource renamed = ResourceUtils.rename(treeCell.getItem().itemProperty().get(), text);
					treeCell.getParent().refresh();
					return renamed;
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});
		tree.rootProperty().set(new ResourceTreeItem(null, container));

		final TabPane editors = new TabPane();
		editors.setTabClosingPolicy(TabClosingPolicy.ALL_TABS);
		editors.setSide(Side.TOP);
		
		tree.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if (event.getClickCount() == 2) {
					if (tree.getSelectionModel().getSelectedItem() != null) {
						open(editors, tree.getSelectionModel().getSelectedItem());
					}
				}
				else if (event.getButton().equals(MouseButton.SECONDARY)) {
					TreeCell<Resource> selectedItem = tree.getSelectionModel().getSelectedItem();
					if (selectedItem != null) {
						ContextMenu contextMenu = new ContextMenu();
						Resource resource = selectedItem.getItem().itemProperty().get();
						if (resource instanceof ManageableContainer) {
							Menu menu = new Menu("Create");
							MenuItem createDirectory = new MenuItem("Folder");
							createDirectory.setGraphic(MainController.loadFixedSizeGraphic("folder.png"));
							createDirectory.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
								@Override
								public void handle(ActionEvent arg0) {
									SimplePropertyUpdater updater = new SimplePropertyUpdater(true, new LinkedHashSet<Property<?>>(Arrays.asList(
											new SimpleProperty<String>("Name", String.class, true)
											)));
									EAIDeveloperUtils.buildPopup(MainController.getInstance(), updater, "Create New Folder", new EventHandler<ActionEvent>() {
										@Override
										public void handle(ActionEvent arg0) {
											String name = updater.getValue("Name");
											try {
												((ManageableContainer) resource).create(name, Resource.CONTENT_TYPE_DIRECTORY);
												selectedItem.getParent().refresh();
											}
											catch (IOException e) {
												MainController.getInstance().notify(new ValidationMessage(Severity.ERROR, "Cannot create a directory by the name of '" + name + "': " + e.getMessage()));
											}
										}
									});
								}
							});
							menu.getItems().addAll(createDirectory);
							
							MenuItem createFile = new MenuItem("File");
							createFile.setGraphic(MainController.loadFixedSizeGraphic("mime/text-plain.png"));
							createFile.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
								@Override
								public void handle(ActionEvent arg0) {
									SimplePropertyUpdater updater = new SimplePropertyUpdater(true, new LinkedHashSet<Property<?>>(Arrays.asList(
										new SimpleProperty<String>("Name", String.class, true)
									)));
									EAIDeveloperUtils.buildPopup(MainController.getInstance(), updater, "Create New File", new EventHandler<ActionEvent>() {
										@Override
										public void handle(ActionEvent arg0) {
											String name = updater.getValue("Name");
											try {
												((ManageableContainer) resource).create(name, URLConnection.guessContentTypeFromName(name));
												selectedItem.getParent().refresh();
											}
											catch (IOException e) {
												MainController.getInstance().notify(new ValidationMessage(Severity.ERROR, "Cannot create a file by the name of '" + name + "': " + e.getMessage()));
											}
										}
									});
								}
							});
							
							MenuItem uploadFile = new MenuItem("Upload File");
							uploadFile.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
								@Override
								public void handle(ActionEvent arg0) {
									SimpleProperty<File> fileProperty = new SimpleProperty<File>("File", File.class, true);
									fileProperty.setInput(true);
									SimplePropertyUpdater updater = new SimplePropertyUpdater(true, new LinkedHashSet<Property<?>>(Arrays.asList(
										fileProperty,
										new SimpleProperty<String>("Name", String.class, false)
									)));
									EAIDeveloperUtils.buildPopup(MainController.getInstance(), updater, "Create New File", new EventHandler<ActionEvent>() {
										@Override
										public void handle(ActionEvent arg0) {
											File file = updater.getValue("File");
											if (file != null) {
												String name = updater.getValue("Name");
												if (name == null) {
													name = file.getName();
												}
												try {
													Resource create = ((ManageableContainer) resource).create(name, URLConnection.guessContentTypeFromName(name));
													WritableContainer<ByteBuffer> writable = ((WritableResource) create).getWritable();
													try {
														Container<ByteBuffer> wrap = IOUtils.wrap(file);
														try {
															IOUtils.copyBytes(wrap, writable);
														}
														finally {
															wrap.close();
														}
													}
													finally {
														writable.close();
													}
													selectedItem.getParent().refresh();
												}
												catch (IOException e) {
													MainController.getInstance().notify(new ValidationMessage(Severity.ERROR, "Cannot create a file by the name of '" + name + "': " + e.getMessage()));
												}
												
											}
										}
									});
								}
							});
							menu.getItems().addAll(createDirectory, createFile);
							contextMenu.getItems().addAll(menu, uploadFile);
						}
						if (selectedItem.getItem().editableProperty().get()) {
							MenuItem item = new MenuItem("Delete");
							item.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
								@Override
								public void handle(ActionEvent arg0) {
									try {
										((ManageableContainer) resource.getParent()).delete(resource.getName());
										selectedItem.getParent().refresh();
									}
									catch (IOException e) {
										MainController.getInstance().notify(new ValidationMessage(Severity.ERROR, "Cannot delete resource '" + resource.getName() + "': " + e.getMessage()));
									}
								}
							});
							contextMenu.getItems().add(item);
						}
						if (!contextMenu.getItems().isEmpty()) {
							tree.setContextMenu(contextMenu);
							tree.getContextMenu().show(MainController.getInstance().getStage(), event.getScreenX(), event.getScreenY());
							// need to actually _remove_ the context menu on action
							// otherwise by default (even if not in this if), the context menu will be shown if you right click
							// this means if you select a folder, right click, you get this menu, you then select a non-folder and right click, you don't enter this code but still see the context menu!
							tree.getContextMenu().addEventHandler(ActionEvent.ACTION, new EventHandler<ActionEvent>() {
								@Override
								public void handle(ActionEvent arg0) {
									tree.setContextMenu(null);
								}
							});
						}
					}
				}
			}
		});
		tree.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if (event.getCode() == KeyCode.ENTER) {
					if (tree.getSelectionModel().getSelectedItem() != null) {
						open(editors, tree.getSelectionModel().getSelectedItem());
					}
				}
				else if (event.getCode() == KeyCode.F && event.isControlDown()) {
					Find<TreeItem<Resource>> find = new Find<TreeItem<Resource>>(new Marshallable<TreeItem<Resource>>() {
						@Override
						public String marshal(TreeItem<Resource> instance) {
							return TreeUtils.getPath(instance).replaceFirst("^[/]+", "").replace("/", ".");
						}
					});
					find.selectedItemProperty().addListener(new ChangeListener<TreeItem<Resource>>() {
						@Override
						public void changed(ObservableValue<? extends TreeItem<Resource>> observable, TreeItem<Resource> oldValue, TreeItem<Resource> newValue) {
							TreeCell<Resource> treeCell = tree.getTreeCell(newValue);
							treeCell.select();
						}
					});
					TreeCell<Resource> selectedItem = tree.getSelectionModel().getSelectedItem();
					find.show(selectedItem == null ? getResources(tree.rootProperty().get()) : getResources(selectedItem.getItem()));
					event.consume();
				}
			}
		});
		
		SplitPane split = new SplitPane();
		split.setOrientation(Orientation.HORIZONTAL);
		
		ScrollPane scroll = new ScrollPane();
		AnchorPane anchorPane = new AnchorPane();
		scroll.setContent(anchorPane);
		anchorPane.getChildren().add(tree);
		
		anchorPane.prefWidthProperty().bind(split.widthProperty());
		AnchorPane.setLeftAnchor(tree, 0d);
		AnchorPane.setRightAnchor(tree, 0d);
		AnchorPane.setTopAnchor(tree, 0d);
		AnchorPane.setBottomAnchor(tree, 0d);
		
		SplitPane.setResizableWithParent(scroll, false);
		tree.setPrefWidth(100);
		tree.maxWidthProperty().bind(split.widthProperty());

		scroll.minWidthProperty().set(0);
		editors.minWidthProperty().set(0);
		split.getItems().add(scroll);
		split.getItems().add(editors);
		
		AnchorPane.setLeftAnchor(split, 0d);
		AnchorPane.setRightAnchor(split, 0d);
		AnchorPane.setTopAnchor(split, 0d);
		AnchorPane.setBottomAnchor(split, 0d);
		
		tab.setContent(split);
		return tab;
	}
	
	private static Collection<TreeItem<Resource>> getResources(TreeItem<Resource> resource) {
		List<TreeItem<Resource>> resources = new ArrayList<TreeItem<Resource>>();
		resources.add(resource);
		for (TreeItem<Resource> child : resource.getChildren()) {
			resources.addAll(getResources(child));
		}
		return resources;
	}
	
	public static class ResourceTreeItem implements TreeItem<Resource>, RemovableTreeItem<Resource> {

		private BooleanProperty editableProperty = new SimpleBooleanProperty(false);
		private ObjectProperty<Resource> itemProperty = new SimpleObjectProperty<Resource>();
		private ObjectProperty<Node> graphicProperty = new SimpleObjectProperty<Node>();
		private BooleanProperty leafProperty = new SimpleBooleanProperty(false);
		private ObservableList<TreeItem<Resource>> children = FXCollections.observableArrayList();
		private TreeItem<Resource> parent;

		public ResourceTreeItem(TreeItem<Resource> parent, Resource resource) {
			this.parent = parent;
			if (resource instanceof ResourceContainer) {
				graphicProperty.set(MainController.loadFixedSizeGraphic("folder.png"));
			}
			else {
				graphicProperty.set(MainController.loadFixedSizeGraphic(resource.getContentType() == null ? "mime/text-plain.png" : "mime/" + resource.getContentType().replaceAll("[^\\w]+", "-") + ".png"));
			}
			leafProperty.set(!(resource instanceof ResourceContainer));
			String parentPath = TreeUtils.getPath(parent);
			editableProperty.set(parent != null && parent.itemProperty().get() instanceof ManageableContainer && !"web".equals(parentPath));
			itemProperty.set(resource);
			refresh(true);
		}
		
		@Override
		public void refresh() {
			refresh(true);			
		}
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		private void refresh(boolean includeChildren) {
			if (itemProperty.get() instanceof CacheableResource) {
				try {
					((CacheableResource) itemProperty.get()).resetCache();
				}
				catch (IOException e) {
					// best effort
				}
			}
			if (!leafProperty.get() && includeChildren) {
				List<Resource> collection = toList(((ResourceContainer) itemProperty.get()).iterator());
				Collections.sort(collection, new Comparator<Resource>() {
					@Override
					public int compare(Resource o1, Resource o2) {
						if (o1 instanceof ResourceContainer) {
							if (o2 instanceof ResourceContainer) {
								return o1.getName().compareTo(o2.getName());
							}
							else {
								return -1;
							}
						}
						else if (o2 instanceof ResourceContainer) {
							return 1;
						}
						else {
							return o1.getName().compareTo(o2.getName());
						}
					}
				});
				TreeUtils.refreshChildren(new TreeItemCreator<Resource>() {
					@Override
					public TreeItem<Resource> create(TreeItem<Resource> parent, Resource child) {
						return new ResourceTreeItem(parent, child);
					}
				}, this, collection);
			}
		}
		
		public static <T> List<T> toList(Iterator<T> iterator) {
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

		@Override
		public boolean remove() {
			if (editableProperty.get()) {
				try {
					((ManageableContainer<?>) parent.itemProperty().get()).delete(getName());
					return true;
				}
				catch (IOException e) {
					return false;
				}
			}
			return false;
		}
		
	}
	
	private static void open(final TabPane editors, TreeCell<Resource> newValue) {
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
				byte[] bytes;
				try {
					ReadableContainer<ByteBuffer> readable = ((ReadableResource) resource).getReadable();
					try {
						bytes = IOUtils.toBytes(readable);
					}
					finally {
						readable.close();
					}
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
				if (resource.getContentType() != null && resource.getContentType().startsWith("image")) {
					ImageView image = new ImageView(new Image(new ByteArrayInputStream(bytes)));
					ScrollPane scroll = new ScrollPane();
					scroll.setContent(image);
					tab.setContent(scroll);
					// intercept close for these tabs as well
					scroll.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
						@Override
						public void handle(KeyEvent event) {
							if (!event.isConsumed() && event.getCode() == KeyCode.W && event.isControlDown()) {
								editors.getTabs().remove(tab);
								// put focus on remaining tab (if any)
								if (editors.getSelectionModel().getSelectedItem() != null) {
									editors.getSelectionModel().getSelectedItem().getContent().requestFocus();
								}
								event.consume();
							}
						}
					});
				}
				else {
					final AceEditor aceEditor = new AceEditor();
					aceEditor.setContent(resource.getContentType(), new String(bytes, Charset.forName("UTF-8")));
					tab.setContent(aceEditor.getWebView());
					aceEditor.subscribe(AceEditor.CLOSE_ALL, new EventHandler<Event>() {
						@Override
						public void handle(Event arg0) {
							final Iterator<Tab> iterator = editors.getTabs().iterator();
							while(iterator.hasNext()) {
								Tab child = iterator.next();
								if (child.getText().endsWith("*")) {
									Confirm.confirm(ConfirmType.QUESTION, "Unsaved Changes", "Do you want to discard all pending changes?", new EventHandler<ActionEvent>() {
										@Override
										public void handle(ActionEvent arg0) {
											iterator.remove();
										}
									});
								}
								else {
									iterator.remove();
								}
							}
						}
					});
					aceEditor.subscribe(AceEditor.CLOSE, new EventHandler<Event>() {
						@Override
						public void handle(Event event) {
							if (tab.getText().endsWith("*")) {
								Confirm.confirm(ConfirmType.QUESTION, "Unsaved Changes", "Do you want to discard all pending changes?", new EventHandler<ActionEvent>() {
									@Override
									public void handle(ActionEvent arg0) {
										editors.getTabs().remove(tab);
										// put focus on remaining tab (if any)
										if (editors.getSelectionModel().getSelectedItem() != null) {
											editors.getSelectionModel().getSelectedItem().getContent().requestFocus();
										}
									}
								});
							}
							else {
								editors.getTabs().remove(tab);
								// put focus on remaining tab (if any)
								if (editors.getSelectionModel().getSelectedItem() != null) {
									editors.getSelectionModel().getSelectedItem().getContent().requestFocus();
								}
							}
						}
					});
					aceEditor.subscribe(AceEditor.CHANGE, new EventHandler<Event>() {
						@Override
						public void handle(Event arg0) {
							if (!tab.getText().endsWith("*")) {
								tab.setText(tab.getText() + " *");
							}
						}
					});
					aceEditor.subscribe(AceEditor.SAVE, new EventHandler<Event>() {
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
									tab.setText(tab.getText().replace(" *", ""));
								}
							}
						}
					});
				}
				editors.getSelectionModel().select(tab);
				tab.getContent().requestFocus();
			}
		}
	}
}
