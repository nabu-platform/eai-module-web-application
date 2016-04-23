package be.nabu.eai.module.web.application;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;

import javafx.collections.ListChangeListener;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.managers.base.BaseConfigurationGUIManager;
import be.nabu.eai.developer.managers.base.BaseJAXBGUIManager;
import be.nabu.eai.developer.managers.util.SimplePropertyUpdater;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.converter.ConverterFactory;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.types.base.ComplexElementImpl;

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
	
	@Override
	public void display(MainController controller, AnchorPane pane, final WebApplication artifact) {
		VBox vbox = new VBox();
		AnchorPane anchor = new AnchorPane();
		display(artifact, anchor);
		vbox.getChildren().addAll(anchor);
		
		// add the configuration per fragment
		try {
			for (WebFragment fragment : artifact.getConfiguration().getWebFragments()) {
				for (final WebFragmentConfiguration configuration : fragment.getFragmentConfiguration()) {
					VBox box = new VBox();
					box.getChildren().addAll(new Separator(Orientation.HORIZONTAL), new Label(configuration.getPath()));
					List<Property<?>> properties = BaseConfigurationGUIManager.createProperty(new ComplexElementImpl(configuration.getType(), null));
					List<Value<?>> values = artifact.getValuesFor(configuration.getPath(), properties);
					AnchorPane childPane = new AnchorPane();
					SimplePropertyUpdater updater = new SimplePropertyUpdater(true, new LinkedHashSet<Property<?>>(properties), values.toArray(new Value[values.size()]));
					updater.valuesProperty().addListener(new ListChangeListener<Value<?>>() {
						@SuppressWarnings("rawtypes")
						@Override
						public void onChanged(ListChangeListener.Change<? extends Value<?>> change) {
							while (change.next()) {
								try {
									if (change.wasRemoved()) {
										for (Value<?> value : change.getRemoved()) {
											artifact.setConfigurationFor(configuration.getPath(), value.getProperty().getName(), null);
										}
									}
									if (change.wasAdded()) {
										for (Value value : change.getAddedSubList()) {
											artifact.setConfigurationFor(configuration.getPath(), value.getProperty().getName(), ConverterFactory.getInstance().getConverter().convert(value.getValue(), String.class));
										}
									}
									if (change.wasUpdated() || change.wasReplaced()) {
										for (Value value : change.getList()) {
											artifact.setConfigurationFor(configuration.getPath(), value.getProperty().getName(), ConverterFactory.getInstance().getConverter().convert(value.getValue(), String.class));
										}
									}
								}
								catch (IOException e) {
									throw new RuntimeException(e);
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
