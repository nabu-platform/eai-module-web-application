package be.nabu.eai.module.web.application;

import java.io.IOException;

import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import be.nabu.eai.developer.managers.base.JAXBArtifactMerger;
import be.nabu.eai.repository.api.Repository;

public class WebApplicationMerger extends JAXBArtifactMerger<WebApplication> {

	public WebApplicationMerger() {
		super(new WebApplicationGUIManager());
	}

	@Override
	public boolean merge(WebApplication source, WebApplication target, AnchorPane pane, Repository targetRepository) {
		super.merge(source, target, pane, targetRepository);
		ScrollPane scroll = new ScrollPane();
		VBox vbox = new VBox();
		vbox.getChildren().addAll(pane.getChildren());
		pane.getChildren().clear();
		try {
			// if the target exist, take its configuration
			if (target != null) {
				source.setFragmentConfiguration(target.getFragmentConfiguration());
			}
			WebApplicationGUIManager.displayPartConfiguration(source, vbox);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		scroll.setContent(vbox);
		pane.getChildren().add(scroll);
		AnchorPane.setBottomAnchor(scroll, 0.0);
		AnchorPane.setRightAnchor(scroll, 0.0);
		AnchorPane.setLeftAnchor(scroll, 0.0);
		AnchorPane.setTopAnchor(scroll, 0.0);
		return true;
	}

	@Override
	public Class<WebApplication> getArtifactClass() {
		return WebApplication.class;
	}

}
