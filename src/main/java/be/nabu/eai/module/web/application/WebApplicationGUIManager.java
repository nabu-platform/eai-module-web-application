package be.nabu.eai.module.web.application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.collections.ListChangeListener;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.managers.base.BaseConfigurationGUIManager;
import be.nabu.eai.developer.managers.base.BaseJAXBGUIManager;
import be.nabu.eai.developer.managers.util.SimplePropertyUpdater;
import be.nabu.eai.module.web.application.WebConfiguration.WebConfigurationPart;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.converter.ConverterFactory;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.base.ComplexElementImpl;
import be.nabu.libs.types.base.ValueImpl;

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
		
		AnchorPane.setLeftAnchor(scroll, 0d);
		AnchorPane.setRightAnchor(scroll, 0d);
		AnchorPane.setTopAnchor(scroll, 0d);
		AnchorPane.setBottomAnchor(scroll, 0d);
		pane.getChildren().add(scroll);
	}
	
}
