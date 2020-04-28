package be.nabu.eai.module.web.application.resource;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import org.w3c.dom.Document;

import be.nabu.eai.developer.Main;
import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.components.RepositoryBrowser;
import be.nabu.eai.developer.util.Confirm;
import be.nabu.eai.developer.util.Confirm.ConfirmType;
import be.nabu.eai.module.http.server.HTTPServerArtifact;
import be.nabu.eai.module.http.virtual.VirtualHostArtifact;
import be.nabu.eai.module.web.application.WebApplication;
import be.nabu.eai.module.web.application.WebApplicationManager;
import be.nabu.eai.module.web.application.WebFragment;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.jfx.control.tree.drag.TreeDragDrop;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.services.api.DefinedService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import nabu.web.application.Services;

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
			box = new VBox();
			
			HBox buttons = new HBox();
			Button reload = new Button("Reload");
			reload.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent arg0) {
					reload();
				}
			});
			
			Button home = new Button("Home");
			home.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent arg0) {
					try {
						goHome();
					}
					catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
			
			buttons.getChildren().addAll(home, reload);
			box.getChildren().add(buttons);
			box.getChildren().add(new Separator(Orientation.HORIZONTAL));
			
			buttons.setPadding(new Insets(10));
			buttons.setAlignment(Pos.CENTER);
			
			webView = new WebView();
			spinner = new ProgressIndicator();
			VBox.setMargin(spinner, new Insets(20, 0, 0, 0));
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
	private VBox box;
	private ProgressIndicator spinner;
	
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

	private void toggleLoading(boolean loading) {
		if (loading) {
			box.getChildren().remove(webView);
			box.getChildren().add(spinner);
		}
		else {
			box.getChildren().remove(spinner);
			box.getChildren().add(webView);
		}
	}
	
	private void goHome() throws IOException {
		firebugInjected = false;
		String url = getExternalUrl();
		webView.getEngine().load(url);
	}
	
	private void load() throws IOException {
		WebEngine webEngine = webView.getEngine();
		String url = getExternalUrl();
//		String url = getInternalUrl();

		toggleLoading(true);
		System.out.println("loading url: " + url);
		
		webEngine.load(url);
		webEngine.getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {
			@Override
			public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue) {
				System.out.println(new Date() + " - new state: " + newValue);
				if (newValue == State.SUCCEEDED) {
					toggleLoading(false);
				}
				else if (newValue == State.SCHEDULED) {
					toggleLoading(true);
				}
			}
		});
		webEngine.documentProperty().addListener(new ChangeListener<Document>() {
			@Override public void changed(ObservableValue<? extends Document> prop, Document oldDoc, Document newDoc) {
				System.out.println(new Date() + " - Document loaded");
				try {
//					enableFirebug(webEngine);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
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
					Object content = db.getContent(TreeDragDrop.getDataFormat(RepositoryBrowser.getDataType(DefinedService.class)));
					e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
					injectDragOverEvent(e);
					e.consume();
				}
	        });
	        this.webview.setOnDragDropped(new EventHandler<DragEvent>() {
				@Override
				public void handle(DragEvent e) {
					Dragboard db = e.getDragboard();
					Object content = db.getContent(TreeDragDrop.getDataFormat(RepositoryBrowser.getDataType(DefinedService.class)));
					boolean success = false;
					success = true;
					injectDropEvent(e);
					e.setDropCompleted(success);
					e.consume();
				}
	        });
//	        this.webview.setOnDragDone(new EventHandler<DragEvent>() {
//				@Override
//				public void handle(DragEvent e) {
//					System.out.println("---------> drag done: " + e);
//					Dragboard db = e.getDragboard();
//					Object content = db.getContent(TreeDragDrop.getDataFormat(RepositoryBrowser.getDataType(DefinedService.class)));
//					System.out.println("\tcontent: " + content);
//				}
//	        });
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
	                "  e.dataTransfer=%s;",
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
	                "  e.dataTransfer=%s;",
	                "	e.clientX = %d;",
	                "	e.clientY = %d;",
	                "  return e;",
	                "}()"), string, (int) e.getX(), (int) e.getY());
	    }

	    private String dropEvent(DragEvent e) {
	    	String string = stringify(e);
	    	Dragboard db = e.getDragboard();
			Object serviceName = db.getContent(TreeDragDrop.getDataFormat(RepositoryBrowser.getDataType(DefinedService.class)));
			// cleanup the name...
			if (serviceName != null) {
				serviceName = serviceName.toString().replaceAll("^[/]+", "").replace('/', '.');
				// if we don't yet have the service in the application, add it if we have the lock
				if (!Services.hasFragment(application, serviceName.toString(), null)) {
					BooleanProperty locked = MainController.getInstance().hasLock(application.getId());
					if (locked.get()) {
						Artifact resolve = EAIResourceRepository.getInstance().resolve(serviceName.toString());
						if (resolve instanceof WebFragment) {
							application.getConfig().getWebFragments().add((WebFragment) resolve);
							Entry entry = EAIResourceRepository.getInstance().getEntry(application.getId());
							if (entry instanceof ResourceEntry) {
								try {
									new WebApplicationManager().save((ResourceEntry) entry, application);
									// do a synchronous reload of target server before we drop the event
									MainController.getInstance().getServer().getRemote().reload(application.getId());
									// also refresh the application in developer so you are not looking at a stale version
									MainController.getInstance().refresh(application.getId());
								}
								catch (Exception e1) {
									MainController.getInstance().notify(e1);
									Confirm.confirm(ConfirmType.WARNING, "Can't add service " + serviceName, "Failed to add the service to the web application", null, null);
								}
							}
							else {
								Confirm.confirm(ConfirmType.WARNING, "Can't add service " + serviceName, "The application is not editable", null, null);
							}
						}
						else {
							Confirm.confirm(ConfirmType.WARNING, "Can't add service " + serviceName, "This operation can not be exposed directly through a web application", null, null);
						}
					}
					else {
						Confirm.confirm(ConfirmType.WARNING, "Can't add service " + serviceName, "This service is not yet available in the web application and you don't have the lock to add it", null, null);
					}
				}
			}
	        return String.format(join("",
	                "function() {",
	                "  var e = new Event('drop',{bubbles:true});",
	                " e.dataTransfer=%s;",
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
	    	String result = "[" + builder.toString().replace("[", "").replace("]", "") + "]";
	    	Dragboard db = e.getDragboard();
			Object serviceName = db.getContent(TreeDragDrop.getDataFormat(RepositoryBrowser.getDataType(DefinedService.class)));
			// cleanup the name...
			if (serviceName != null) {
				serviceName = serviceName.toString().replaceAll("^[/]+", "").replace('/', '.');
			}
			result = "{ types: " + result + ", data: {'service': " + (serviceName == null ? "null" : "'" + serviceName + "'") + " }}";
			return result;
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
