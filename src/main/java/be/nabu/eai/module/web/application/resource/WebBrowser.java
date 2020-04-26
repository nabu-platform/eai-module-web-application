package be.nabu.eai.module.web.application.resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.w3c.dom.Document;

import be.nabu.eai.developer.Main;
import be.nabu.eai.developer.MainController;
import be.nabu.eai.module.http.server.HTTPServerArtifact;
import be.nabu.eai.module.http.virtual.VirtualHostArtifact;
import be.nabu.eai.module.web.application.WebApplication;
import be.nabu.utils.io.IOUtils;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Border;
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
			Tab newTab = MainController.getInstance().newTab("Browsing " + application.getId());
			AnchorPane pane = asPane();
			newTab.setContent(pane);
		}
	}

	public AnchorPane asPane() {
		try {
			VBox box = new VBox();
			
			HBox buttons = new HBox();
			Button reload = new Button("Reload");
			reload.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent arg0) {
					reload();
				}
			});
			
			buttons.getChildren().add(reload);
			box.getChildren().add(buttons);
			box.getChildren().add(new Separator(Orientation.HORIZONTAL));
			
			buttons.setPadding(new Insets(10));
			buttons.setAlignment(Pos.CENTER);
			
			webView = new WebView();
			addKeyHandlers(webView);
			
			new WebViewWithFileDragEvents(webView);
			box.getChildren().add(webView);
			VBox.setVgrow(webView, Priority.ALWAYS);
			
			load();
			
			Button inject = new Button("Inject Firebug");
			inject.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent arg0) {
					enableFirebug(webView.getEngine());
				}
			});
//			buttons.getChildren().add(inject);
			
			Button browser = new Button("Open in browser");
			browser.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent arg0) {
					try {
						Main.getInstance().getHostServices().showDocument(getExternalUrl());
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			buttons.getChildren().add(browser);
	
			AnchorPane pane = new AnchorPane();
			pane.getChildren().add(box);
			AnchorPane.setBottomAnchor(box, 0d);
			AnchorPane.setLeftAnchor(box, 0d);
			AnchorPane.setRightAnchor(box, 0d);
			AnchorPane.setTopAnchor(box, 0d);
			return pane;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private boolean firebugInjected = false;
	
	private void reload() {
		firebugInjected = false;
		webView.getEngine().reload();
	}
	
	private void addKeyHandlers(WebView webView) {
		webView.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if (event.getCode() == KeyCode.F12) {
					if (!firebugInjected) {
						firebugInjected = true;
						enableFirebug(webView.getEngine());
					}
					else {
//						webView.getEngine().executeScript("Firebug.chrome.toggle(true, true);");
					}
				}
				else if (event.getCode() == KeyCode.F5) {
					reload();
				}
			}
		});
	}

	private void load() throws IOException {
		WebEngine webEngine = webView.getEngine();
		String url = getExternalUrl();
//		String url = getInternalUrl();

		System.out.println("loading url: " + url);
		
		webEngine.load(url);
//		webEngine.documentProperty().addListener(new ChangeListener<Document>() {
//			@Override public void changed(ObservableValue<? extends Document> prop, Document oldDoc, Document newDoc) {
//				System.out.println("Document loaded, automatically enabling firebug: " + newDoc);
//				try {
////					enableFirebug(webEngine);
//				}
//				catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		});
	}
	
	public String getExternalUrl() throws IOException {
		VirtualHostArtifact host = application.getConfig().getVirtualHost();
		HTTPServerArtifact server = host.getConfig().getServer();
		
		String url = (server.isSecure() ? "https" : "http") + "://" + (host.getConfig().getHost() == null ? "localhost" : host.getConfig().getHost());
		if (!server.getConfig().isProxied() && server.getConfig().getPort() != null) {
			url += ":" + server.getConfig().getPort();
		}
		url += application.getServerPath();
		return url;
	}

	private static void enableFirebug(final WebEngine engine) {
		try {
//			String externalForm = WebBrowser.class.getResource("/browser/firebug-1.4-lite.js").toExternalForm();
//			System.out.println("Found editor: " + externalForm);
	//		engine.executeScript("var script = document.createElement('script'); script.setAttribute('src', 'https://cdnjs.cloudflare.com/ajax/libs/firebug-lite/1.4.0/firebug-lite.js'); document.body.appendChild(script);");
			// this one works!
			engine.executeScript("if (!document.getElementById('FirebugLite')){E = document['createElement' + 'NS'] && document.documentElement.namespaceURI;E = E ? document['createElement' + 'NS'](E, 'script') : document['createElement']('script');E['setAttribute']('id', 'FirebugLite');E['setAttribute']('src', 'https://cdnjs.cloudflare.com' + '/ajax/libs/firebug-lite/1.4.0/firebug-lite.js' + '#startOpened');E['setAttribute']('FirebugLite', '4');(document['getElementsByTagName']('head')[0] || document['getElementsByTagName']('body')[0]).appendChild(E);}"); 
//			engine.executeScript("if (!document.getElementById('FirebugLite')){E = document['createElement' + 'NS'] && document.documentElement.namespaceURI;E = E ? document['createElement' + 'NS'](E, 'script') : document['createElement']('script');E['setAttribute']('id', 'FirebugLite');E['setAttribute']('src', 'https://cdnjs.cloudflare.com' + '/ajax/libs/firebug-lite/1.4.0/firebug-lite.js');E['setAttribute']('FirebugLite', '4');(document['getElementsByTagName']('head')[0] || document['getElementsByTagName']('body')[0]).appendChild(E);}"); 
	//		engine.executeScript("var script = document.createElement('script'); script.setAttribute('src', '" + externalForm + "'); document.body.appendChild(script);");
			
	//		int indexOf = externalForm.indexOf("/browser");
	//		String server = externalForm.substring(0, indexOf);
	//		String path = externalForm.substring(indexOf);
	//		engine.executeScript("if (!document.getElementById('FirebugLite')){E = document['createElement' + 'NS'] && document.documentElement.namespaceURI;E = E ? document['createElement' + 'NS'](E, 'script') : document['createElement']('script');E['setAttribute']('id', 'FirebugLite');E['setAttribute']('src', '" + server + "' + '" + path + "' + '#startOpened');E['setAttribute']('FirebugLite', '4');(document['getElementsByTagName']('head')[0] || document['getElementsByTagName']('body')[0]).appendChild(E);}");
			
			// does not work, presumably it is loading some shizzles afterwards...
//			InputStream stream = WebBrowser.class.getResourceAsStream("/browser/firebug-1.4-lite.js");
//			try {
//				byte[] bytes = IOUtils.toBytes(IOUtils.wrap(stream));
//				String string = new String(bytes, Charset.forName("UTF-8"));
//				engine.executeScript(string);
//			}
//			finally {
//				stream.close();
//			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public boolean canShow() {
		return application.getConfig().getVirtualHost() != null && application.getConfig().getVirtualHost().getConfig().getServer() != null;
	}
	
	
	public class WebViewWithFileDragEvents {
	    private final WebView webview;

	    public WebViewWithFileDragEvents(WebView webview) {
	        this.webview = webview;
	        this.webview.getEngine().setJavaScriptEnabled(true);
	        this.webview.setOnDragOver(new EventHandler<DragEvent>() {
				@Override
				public void handle(DragEvent e) {
					Dragboard db = e.getDragboard();
					e.acceptTransferModes(TransferMode.COPY);
					injectDragOverEvent(e);
					e.consume();
				}
	        });
	        this.webview.setOnDragDropped(new EventHandler<DragEvent>() {
				@Override
				public void handle(DragEvent e) {
					boolean success = false;
					success = true;
					injectDropEvent(e);
					e.setDropCompleted(success);
					e.consume();
				}
	        });
	    }

	    private void injectDragOverEvent(DragEvent e) {
	        inject(join("",
	                "{",
	                "  var newElement=document.elementFromPoint(%d,%d);",
	                "  if (window.lastInjectedEvent && window.lastInjectedEvent != newElement) {",
	                "     //fire dragout",
	                "     window.lastInjectedEvent.dispatchEvent(%s)",
	                "  }",
	                "  window.lastInjectedEvent = newElement",
	                "  newElement.dispatchEvent(%s);",
	                "}"),
	                (int) e.getX(), (int) e.getY(), dragLeaveEvent(e), dragOverEvent(e));
	    }

	    private String join(String... lines) {
	        return String.join("\n", Arrays.asList(lines));
	    }

	    private void inject(String text, Object... args) {
//	    	System.out.println("Injecting: " + text + " \n" + Arrays.asList(args));
	        webview.getEngine().executeScript(String.format(text, args));
	    }

	    private String dragLeaveEvent(DragEvent e) {
	        return String.format(join("",
	                "function() {",
	                "  var e = new Event('dragleave',{bubbles:true});",
	                "  e.dataTransfer={ types: %s };",
	                "	e.clientX = %d;",
	                "	e.clientY = %d;",
	                "  return e;",
	                "}()"), stringify(e), (int) e.getX(), (int) e.getY());
	    }

	    private String dragOverEvent(DragEvent e) {
	    	String string = stringify(e);
	        return String.format(join("",
	                "function() {",
	                "  var e = new Event('dragover',{bubbles:true});",
	                "  e.dataTransfer={ types: %s };",
	                "	e.clientX = %d;",
	                "	e.clientY = %d;",
	                "  return e;",
	                "}()"), string, (int) e.getX(), (int) e.getY());
	    }

	    private String dropEvent(DragEvent e) {
	    	String string = stringify(e);
	        return String.format(join("",
	                "function() {",
	                "  var e = new Event('drop',{bubbles:true});",
	                " e.dataTransfer={ types: %s };",
	                "	e.clientX = %d;",
	                "	e.clientY = %d;",
	                "  return e;",
	                "}()"),
	        		string, (int) e.getX(), (int) e.getY());

	    }

		private String stringify(DragEvent e) {
			StringBuilder builder = new StringBuilder();
	    	for (DataFormat format : e.getDragboard().getContentTypes()) {
	    		if (!builder.toString().isEmpty()) {
	    			builder.append(", ");
	    		}
	    		builder.append("'").append(format.toString()).append("'");
	    	}
	    	return "[" + builder.toString().replace("[", "").replace("]", "") + "]";
		}

	    private void injectDropEvent(DragEvent e) {
	        inject(join("",
	                "{",
	                "  var newElement=document.elementFromPoint(%d,%d);",
	                "  newElement.dispatchEvent(%s);",
	                "}"),
	                (int) e.getX(), (int) e.getY(), dropEvent(e));

	    }
	}
}
