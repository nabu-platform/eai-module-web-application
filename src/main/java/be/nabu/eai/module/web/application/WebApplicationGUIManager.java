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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;
import be.nabu.eai.authentication.api.PasswordAuthenticator;
import be.nabu.eai.authentication.api.SecretAuthenticator;
import be.nabu.eai.developer.ComplexContentEditor;
import be.nabu.eai.developer.ComplexContentEditor.ValueWrapper;
import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.managers.base.BaseJAXBGUIManager;
import be.nabu.eai.developer.managers.util.SimpleProperty;
import be.nabu.eai.developer.managers.util.SimplePropertyUpdater;
import be.nabu.eai.developer.util.Confirm;
import be.nabu.eai.developer.util.Confirm.ConfirmType;
import be.nabu.eai.developer.util.EAIDeveloperUtils;
import be.nabu.eai.developer.util.Find;
import be.nabu.eai.module.web.application.api.RateLimitSettingsProvider;
import be.nabu.eai.module.web.application.api.RequestSubscriber;
import be.nabu.eai.repository.EAIRepositoryUtils;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.Translator;
import be.nabu.eai.repository.api.UserLanguageProvider;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.jfx.control.ace.AceEditor;
import be.nabu.jfx.control.tree.Marshallable;
import be.nabu.jfx.control.tree.RemovableTreeItem;
import be.nabu.jfx.control.tree.Tree;
import be.nabu.jfx.control.tree.TreeCell;
import be.nabu.jfx.control.tree.TreeItem;
import be.nabu.jfx.control.tree.TreeUtils;
import be.nabu.jfx.control.tree.TreeUtils.TreeItemCreator;
import be.nabu.jfx.control.tree.drag.TreeDragDrop;
import be.nabu.jfx.control.tree.drag.TreeDragListener;
import be.nabu.jfx.control.tree.drag.TreeDropListener;
import be.nabu.jfx.control.tree.Updateable;
import be.nabu.libs.authentication.api.DeviceValidator;
import be.nabu.libs.authentication.api.PermissionHandler;
import be.nabu.libs.authentication.api.PotentialPermissionHandler;
import be.nabu.libs.authentication.api.RoleHandler;
import be.nabu.libs.authentication.api.TokenValidator;
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
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.Element;
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
		MainController.registerStyleSheet("webapplication.css");
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
	
	@Override
	public void display(MainController controller, AnchorPane pane, final WebApplication artifact) {
		VBox vbox = new VBox();
		AnchorPane anchor = new AnchorPane();
		display(artifact, anchor);
		vbox.getChildren().addAll(anchor);
		
		displayPartConfiguration(artifact, vbox);
		
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

	public static void displayPartConfiguration(final WebApplication artifact, VBox vbox) {
		displayPartConfiguration(artifact, vbox, artifact.getFragmentConfigurations(), artifact.getEnvironmentSpecificConfigurations(), true);
	}
	
	public static void displayPartConfiguration(final WebApplication artifact, VBox vbox, Map<String, Map<String, ComplexContent>> fragmentConfigurations, List<ComplexContent> environmentSpecificConfigurations, boolean includeCheckbox) {
		// add the configuration per fragment
		try {
			// keeps track of the contents to see which are in use
			List<ComplexContent> used = new ArrayList<ComplexContent>();
			if (artifact.getConfiguration().getWebFragments() != null) {
				for (WebFragment fragment : artifact.getConfiguration().getWebFragments()) {
					List<WebFragmentConfiguration> configurations = fragment == null ? null : fragment.getFragmentConfiguration();
					if (configurations != null) {
						for (final WebFragmentConfiguration fragmentConfiguration : configurations) {
							String typeId = ((DefinedType) fragmentConfiguration.getType()).getId();
							// get the path claimed by the configuration
							String path = fragmentConfiguration.getPath();
							// append anything that the web application itself adds (if it is not on the root)
							if (artifact.getConfiguration().getPath() != null && !artifact.getConfiguration().getPath().isEmpty() && !"/".equals(artifact.getConfiguration().getPath())) {
								path = artifact.getConfiguration().getPath().replaceFirst("[/]+$", "") + "/" + path.replaceFirst("^[/]+", "");
								if (!path.startsWith("/")) {
									path = "/" + path;
								}
							}
							reusableMethodWithNoName(artifact, used, typeId, path, fragmentConfiguration.getType(), fragmentConfigurations);
						}
					}
				}
			}
			
			Map<Method, List<Element<?>>> extensions = getExtensions(artifact);
			for (Method method : extensions.keySet()) {
				for (Element<?> element : extensions.get(method)) {
					if (element.getType() instanceof ComplexType && element.getType() instanceof DefinedType) {
						String typeId = ((DefinedType) element.getType()).getId();
						String path = "$" + method.toString();
						reusableMethodWithNoName(artifact, used, typeId, path, (ComplexType) element.getType(), fragmentConfigurations);
					}
				}
			}
			
			// could autoremove unused configurations or just flag them with a button to delete
			// you might have accidently removed something you didn't mean to and lose precious configuration settings
			for (String typeId : fragmentConfigurations.keySet()) {
				for (String regex : fragmentConfigurations.get(typeId).keySet()) {
					ComplexContent content = fragmentConfigurations.get(typeId).get(regex);
					VBox box = new VBox();
					box.getStyleClass().add("web-fragment-configuration");
					HBox hbox = new HBox();
					hbox.getStyleClass().add("title");
					hbox.getChildren().add(new Label(typeId + " applicable to: " + (regex == null ? ".*" : regex)));
					if (!used.contains(content)) {
						Button delete = new Button("Remove Unused");
						delete.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
							@Override
							public void handle(ActionEvent arg0) {
								fragmentConfigurations.get(typeId).remove(regex);
								if (fragmentConfigurations.get(typeId).isEmpty()) {
									fragmentConfigurations.remove(typeId);
								}
								vbox.getChildren().remove(box);
								MainController.getInstance().setChanged();
							}
						});
						hbox.getChildren().add(delete);
						box.getStyleClass().add("web-fragment-configuration-unused");
					}
					Separator separator = new Separator(Orientation.HORIZONTAL);
					separator.getStyleClass().add("separator");
					box.getChildren().addAll(hbox);
					if (includeCheckbox) {
						CheckBox checkbox = new CheckBox("Environment specific");
						checkbox.getStyleClass().add("environmentSpecific");
						checkbox.setSelected(environmentSpecificConfigurations.contains(content));
						checkbox.selectedProperty().addListener(new ChangeListener<Boolean>() {
							@Override
							public void changed(ObservableValue<? extends Boolean> arg0, Boolean arg1, Boolean arg2) {
								if (!arg2) {
									environmentSpecificConfigurations.remove(content);
								}
								else if (!environmentSpecificConfigurations.contains(content)) {
									environmentSpecificConfigurations.add(content);
								}
								MainController.getInstance().setChanged();
							}
						});
						box.getChildren().add(checkbox);
					}
					Tree<ValueWrapper> tree = new ComplexContentEditor(content, true, artifact.getRepository()).getTree();
					tree.getStyleClass().add("tree");
					tree.prefWidthProperty().bind(box.widthProperty());
					box.getChildren().addAll(tree, separator);
					vbox.getChildren().add(box);
				}
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void reusableMethodWithNoName(final WebApplication artifact, List<ComplexContent> used, String typeId, String path, ComplexType type, Map<String, Map<String, ComplexContent>> fragmentConfigurations) throws IOException {
		// check if there is a configuration for this item
		ComplexContent currentConfiguration = artifact.getConfigurationFor(path, type, fragmentConfigurations);
		// if not, create one with a root path regex
		if (currentConfiguration == null) {
			if (!fragmentConfigurations.containsKey(typeId)) {
				fragmentConfigurations.put(typeId, new HashMap<String, ComplexContent>());
			}
			currentConfiguration = type.newInstance();
			fragmentConfigurations.get(typeId).put(null, currentConfiguration);
		}
		used.add(currentConfiguration);
	}
	
	private static Map<Method, List<Element<?>>> getExtensions(WebApplication artifact) {
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
		
		// potential permission handler
		if (artifact.getConfig().getPotentialPermissionService() != null) {
			Method method = WebApplication.getMethod(PotentialPermissionHandler.class, "hasPotentialPermission");
			extensions.put(method, EAIRepositoryUtils.getInputExtensions(artifact.getConfig().getPotentialPermissionService(), method));
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
		
		// request subscriber
		if (artifact.getConfig().getRequestSubscriber() != null) {
			Method method = WebApplication.getMethod(RequestSubscriber.class, "handle");
			extensions.put(method, EAIRepositoryUtils.getInputExtensions(artifact.getConfig().getRequestSubscriber(), method));
		}
		
		if (artifact.getConfig().getRateLimiter() != null) {
			Method method = WebApplication.getMethod(RateLimitSettingsProvider.class, "settings");
			extensions.put(method, EAIRepositoryUtils.getInputExtensions(artifact.getConfig().getRateLimiter(), method));
		}
		
		return extensions;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Tab buildEditingTab(final WebApplication artifact) throws IOException, URISyntaxException {
		ResourceContainer<?> publicDirectory = (ResourceContainer<?>) artifact.getDirectory().getChild(EAIResourceRepository.PUBLIC);
		if (publicDirectory == null && artifact.getDirectory() instanceof ManageableContainer) {
			publicDirectory = (ResourceContainer<?>) ((ManageableContainer<?>) artifact.getDirectory()).create(EAIResourceRepository.PUBLIC, Resource.CONTENT_TYPE_DIRECTORY);
		}
		ResourceUtils.mkdirs(publicDirectory, "pages");
		ResourceUtils.mkdirs(publicDirectory, "resources");
		ResourceUtils.mkdirs(publicDirectory, "artifacts");
		ResourceContainer<?> privateDirectory = (ResourceContainer<?>) artifact.getDirectory().getChild(EAIResourceRepository.PRIVATE);
		if (privateDirectory == null && artifact.getDirectory() instanceof ManageableContainer) {
			privateDirectory = (ResourceContainer<?>) ((ManageableContainer<?>) artifact.getDirectory()).create(EAIResourceRepository.PRIVATE, Resource.CONTENT_TYPE_DIRECTORY);
		}
		ResourceUtils.mkdirs(privateDirectory, "scripts");
		ResourceUtils.mkdirs(privateDirectory, "meta");
		ResourceUtils.mkdirs(privateDirectory, "provided/artifacts");
		ResourceUtils.mkdirs(privateDirectory, "provided/resources");
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
					// only rename if nothing exists yet by that name
					if (text != null && !text.trim().isEmpty() && treeCell.getItem().itemProperty().get().getParent() != null && treeCell.getItem().itemProperty().get().getParent().getChild(text) == null) {
						Resource renamed = ResourceUtils.rename(treeCell.getItem().itemProperty().get(), text);
						treeCell.getParent().refresh();
						return renamed;
					}
					else {
						return treeCell.getItem().itemProperty().get();
					}
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});
		tree.rootProperty().set(new ResourceTreeItem(null, container));

		TreeDragDrop.makeDraggable(tree, new TreeDragListener<Resource>() {
			@Override
			public TransferMode getTransferMode() {
				return TransferMode.MOVE;
			}
			@Override
			public boolean canDrag(TreeCell<Resource> item) {
				String path = TreeUtils.getPath(item.getItem());
				return item.getItem().getParent().itemProperty().get() instanceof ManageableContainer && !path.equals("web") && !path.equals("web/public") && !path.equals("web/private");
			}
			@Override
			public String getDataType(TreeCell<Resource> item) {
				return "resource";
			}
			@Override
			public void drag(TreeCell<Resource> cell) {
				// do nothing
			}
			@Override
			public void stopDrag(TreeCell<Resource> cell, boolean droppedSuccessfully) {
				// do nothing
			}
		});
		TreeDragDrop.makeDroppable(tree, new TreeDropListener<Resource>() {
			@Override
			public boolean canDrop(String dataType, TreeCell<Resource> target, TreeCell<?> dragged, TransferMode transferMode) {
				return !dragged.equals(target)
						&& dragged.getItem().itemProperty().get() instanceof Resource
						&& "resource".equals(dataType)
						// can not be in the root of the resources, it's a virtual container
						&& target.getParent() != null 
						&& target.getItem().itemProperty().get() instanceof ManageableContainer
						// nothing with that name must exist in the target
						&& ((ResourceContainer) target.getItem().itemProperty().get()).getChild(((Resource) dragged.getItem().itemProperty().get()).getName()) == null
						&& transferMode == TransferMode.MOVE;
			}
			@Override
			public void drop(String dataType, TreeCell<Resource> target, TreeCell<?> dragged, TransferMode transferMode) {
				Resource source = (Resource) dragged.getItem().itemProperty().get();
				try {
					ResourceUtils.copy(source, ((ManageableContainer) target.getItem().itemProperty().get()));
					if (TransferMode.MOVE == transferMode) {
						((ManageableContainer) source.getParent()).delete(source.getName());
					}
					target.getParent().refresh();
					dragged.getParent().refresh();
				}
				catch (IOException e) {
					MainController.getInstance().notify(e);
				}
			}
		});
		
		final TabPane editors = new TabPane();
		editors.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
			@Override
			public void changed(ObservableValue<? extends Tab> arg0, Tab arg1, Tab arg2) {
				if (arg2 != null) {
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							arg2.getContent().requestFocus();
						}
					});
				}
			}
		});
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
						if (TreeUtils.getPath(selectedItem.getItem()).startsWith("web/public/pages")) {
							MenuItem item = new MenuItem("Add Page");
							item.setGraphic(MainController.loadFixedSizeGraphic("text-x-eglue.png"));
							item.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
								@Override
								public void handle(ActionEvent arg0) {
									SimplePropertyUpdater updater = new SimplePropertyUpdater(true, new LinkedHashSet<Property<?>>(Arrays.asList(
										new SimpleProperty<String>("Name", String.class, true)
									)));
									EAIDeveloperUtils.buildPopup(MainController.getInstance(), updater, "Add Page", new EventHandler<ActionEvent>() {
										@Override
										public void handle(ActionEvent arg0) {
											String name = updater.getValue("Name");
											try {
												if (((ResourceContainer) selectedItem.getItem().itemProperty().get()).getChild(name + ".eglue") == null) {
													Resource create = ((ManageableContainer<?>) selectedItem.getItem().itemProperty().get()).create(name + ".eglue", "text/x-eglue");
													WritableContainer<ByteBuffer> writable = ((WritableResource) create).getWritable();
													try {
														writable.write(IOUtils.wrap(("<html>\n"
															+ "\t<head>\n"
															+ "\t\t<title>My Page</title>\n"
															+ "\t</head>\n"
															+ "\t<body>\n"
															+ "\t\t<p>Hello World!</p>\n"
															+ "\t</body>\n"
															+ "</html>").getBytes("UTF-8"), true));
													}
													finally {
														writable.close();
													}
													selectedItem.getParent().refresh();
												}
											}
											catch (IOException e) {
												MainController.getInstance().notify(e);
											}
										}
									});
								}
							});
							contextMenu.getItems().add(item);
						}
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
							tree.getContextMenu().setOnHiding(new EventHandler<WindowEvent>() {
								@Override
								public void handle(WindowEvent arg0) {
									tree.setContextMenu(null);
								}
							});
						}
					}
				}
			}
		});
		
		final List<String> searchableContentTypes = Arrays.asList(new String [] { "text/plain", "application/javascript", "text/xml", "application/xml", "text/css",
				"text/x-glue", "text/x-gcss", "text/x-eglue", "text/x-markdown" });
		
		tree.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if (event.getCode() == KeyCode.ENTER) {
					if (tree.getSelectionModel().getSelectedItem() != null) {
						open(editors, tree.getSelectionModel().getSelectedItem());
					}
				}
				else if (event.getCode() == KeyCode.F && event.isControlDown() && !event.isShiftDown()) {
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
							treeCell.show();
						}
					});
					TreeCell<Resource> selectedItem = tree.getSelectionModel().getSelectedItem();
					find.show(selectedItem == null ? getResources(tree.rootProperty().get()) : getResources(selectedItem.getItem()));
					event.consume();
				}
				// need to add a lucene searcher for resources and use it here
//				else if (event.getCode() == KeyCode.F && event.isControlDown() && event.isShiftDown()) {
//					Find<TreeItem<Resource>> find = new Find<TreeItem<Resource>>(new Marshallable<TreeItem<Resource>>() {
//						@Override
//						public String marshal(TreeItem<Resource> instance) {
//							return TreeUtils.getPath(instance).replaceFirst("^[/]+", "").replace("/", ".");
//						}
//					}) {
//						@Override
//						public void filterList(List<TreeItem<Resource>> list, String newValue) {
//							Iterator<TreeItem<Resource>> iterator = list.iterator();
//							while (iterator.hasNext()) {
//								TreeItem<Resource> next = iterator.next();
//								if (searchableContentTypes.contains(next.itemProperty().get().getContentType())) {
//									
//								}
//							}
//						}
//					};
//					find.selectedItemProperty().addListener(new ChangeListener<TreeItem<Resource>>() {
//						@Override
//						public void changed(ObservableValue<? extends TreeItem<Resource>> observable, TreeItem<Resource> oldValue, TreeItem<Resource> newValue) {
//							TreeCell<Resource> treeCell = tree.getTreeCell(newValue);
//							treeCell.select();
//						}
//					});
//					TreeCell<Resource> selectedItem = tree.getSelectionModel().getSelectedItem();
//					find.show(selectedItem == null ? getResources(tree.rootProperty().get()) : getResources(selectedItem.getItem()));
//					event.consume();
//				}
				else if (event.getCode() == KeyCode.E && event.isControlDown()) {
					if (tree.getSelectionModel().getSelectedItem() != null) {
						if (event.isShiftDown()) {
							tree.getSelectionModel().getSelectedItem().collapseAll();	
						}
						else {
							tree.getSelectionModel().getSelectedItem().expandAll();
						}
					}
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
			refresh(true, false);
		}
		
		@Override
		public void refresh() {
			refresh(true, true);
		}
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		private void refresh(boolean includeChildren, boolean resetCache) {
			if (resetCache && itemProperty.get() instanceof CacheableResource) {
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
				final Tab tab = new Tab(path.replaceAll("^.*/", ""));
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
					aceEditor.subscribe(AceEditor.FULL_SCREEN, new EventHandler<Event>() {
						@Override
						public void handle(Event arg0) {
							toggleFullScreen(editors);
						}
					});
				}
				editors.getSelectionModel().select(tab);
				tab.getContent().requestFocus();
			}
		}
	}
	
	private static List<Node> content;
	private static Scene scene;
	private static void toggleFullScreen(TabPane contentToShow) {
		if (content == null) {
			int index = contentToShow.getTabs().indexOf(contentToShow.getSelectionModel().getSelectedItem());
			content = new ArrayList<Node>(contentToShow.getScene().getRoot().getChildrenUnmodifiable());
			scene = contentToShow.getScene();
			TabPane pane = new TabPane();
			((Pane) scene.getRoot()).getChildren().clear();
			pane.getTabs().addAll(contentToShow.getTabs());
			contentToShow.getTabs().clear();
			pane.getSelectionModel().select(index);
			((Pane) scene.getRoot()).getChildren().add(pane);
			AnchorPane.setBottomAnchor(pane, 0d);
			AnchorPane.setTopAnchor(pane, 0d);
			AnchorPane.setLeftAnchor(pane, 0d);
			AnchorPane.setRightAnchor(pane, 0d);
			pane.prefWidthProperty().bind(scene.widthProperty());
			pane.prefHeightProperty().bind(scene.heightProperty());
		}
		else {
			TabPane pane = (TabPane) ((Pane) scene.getRoot()).getChildren().get(0);
			int index = pane.getTabs().indexOf(pane.getSelectionModel().getSelectedItem());
			((Pane) scene.getRoot()).getChildren().clear();
			((Pane) scene.getRoot()).getChildren().addAll(content);
			content = null;
			contentToShow.getTabs().addAll(pane.getTabs());
			pane.getTabs().clear();
			contentToShow.getSelectionModel().select(index);
		}
	}
}
