package be.nabu.eai.module.web.application.resource;

import java.io.IOException;
import java.text.ParseException;

import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.api.EntryContextMenuProvider;
import be.nabu.eai.module.web.application.WebApplication;
import be.nabu.eai.repository.api.Entry;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.MenuItem;

public class WebApplicationContextMenu implements EntryContextMenuProvider {

	@Override
	public MenuItem getContext(Entry entry) {
		if (entry.isNode() && WebApplication.class.isAssignableFrom(entry.getNode().getArtifactClass())) {
			MenuItem item = new MenuItem("Open in browser");
			item.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent arg0) {
					try {
						WebApplication application = (WebApplication) entry.getNode().getArtifact();
						new WebBrowser(application).open();
					}
					catch (IOException | ParseException e) {
						MainController.getInstance().notify(e);
					}
				}
			});
			return item;
		}
		return null;
	}

}
