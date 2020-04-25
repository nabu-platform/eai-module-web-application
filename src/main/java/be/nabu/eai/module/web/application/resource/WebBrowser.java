package be.nabu.eai.module.web.application.resource;

import java.io.IOException;

import org.w3c.dom.Document;

import be.nabu.eai.developer.MainController;
import be.nabu.eai.module.http.server.HTTPServerArtifact;
import be.nabu.eai.module.http.virtual.VirtualHostArtifact;
import be.nabu.eai.module.web.application.WebApplication;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class WebBrowser {
	private WebApplication application;
	private WebView webView;
	
	public WebBrowser(WebApplication application) {
		this.application = application;
	}
	
	public void open() {
		if (canShow()) {
			try {
				Tab newTab = MainController.getInstance().newTab("Browsing " + application.getId());
				VBox box = new VBox();
				
				HBox buttons = new HBox();
				Button reload = new Button("Reload");
				reload.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent arg0) {
						try {
							load();
						}
						catch (IOException e) {
							MainController.getInstance().notify(e);
						}
					}
				});
				
				buttons.getChildren().add(reload);
				box.getChildren().add(buttons);
				
				webView = new WebView();
				box.getChildren().add(webView);
				VBox.setVgrow(webView, Priority.ALWAYS);
				
				load();

				AnchorPane pane = new AnchorPane();
				newTab.setContent(pane);
				pane.getChildren().add(box);
				AnchorPane.setBottomAnchor(box, 0d);
				AnchorPane.setLeftAnchor(box, 0d);
				AnchorPane.setRightAnchor(box, 0d);
				AnchorPane.setTopAnchor(box, 0d);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	private void load() throws IOException {
		WebEngine webEngine = webView.getEngine();
		VirtualHostArtifact host = application.getConfig().getVirtualHost();
		HTTPServerArtifact server = host.getConfig().getServer();
		
		String url = (server.isSecure() ? "https" : "http") + "://" + (host.getConfig().getHost() == null ? "localhost" : host.getConfig().getHost());
		if (!server.getConfig().isProxied() && server.getConfig().getPort() != null) {
			url += ":" + server.getConfig().getPort();
		}
		url += application.getServerPath();

		System.out.println("loading url: " + url);
		
		webEngine.documentProperty().addListener(new ChangeListener<Document>() {
			@Override public void changed(ObservableValue<? extends Document> prop, Document oldDoc, Document newDoc) {
				enableFirebug(webEngine);
			}
		});
		
		webEngine.load(url);
	}

	private static void enableFirebug(final WebEngine engine) {
		engine.executeScript("if (!document.getElementById('FirebugLite')){ var script = document.createElement('script'); script.setAttribute('id', 'FirebugLite'); script.setAttribute('src', 'https://cdnjs.cloudflare.com/ajax/libs/firebug-lite/1.4.0/firebug-lite.js'); document.head.appendChild(script); }");
	}
	
	public boolean canShow() {
		return application.getConfig().getVirtualHost() != null && application.getConfig().getVirtualHost().getConfig().getServer() != null;
	}
}
