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

import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import be.nabu.eai.developer.managers.base.JAXBArtifactMerger;
import be.nabu.eai.repository.api.Repository;
import be.nabu.libs.types.api.ComplexContent;

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
		// if the target exist, take its configuration for the environment-specific parts
		if (target != null) {
			for (String typeId : source.getFragmentConfigurations().keySet()) {
				for (String regex : source.getFragmentConfigurations().get(typeId).keySet()) {
					if (source.getEnvironmentSpecificConfigurations().contains(source.getFragmentConfigurations().get(typeId).get(regex))) {
						ComplexContent targetContent = target.getFragmentConfigurations().get(typeId) == null ? null : target.getFragmentConfigurations().get(typeId).get(regex);
						if (targetContent != null) {
							source.getFragmentConfigurations().get(typeId).put(regex, targetContent);
						}
					}
				}
			}
		}
		WebApplicationGUIManager.displayPartConfiguration(source, vbox, source.getFragmentConfigurations(), source.getEnvironmentSpecificConfigurations(), false);
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
