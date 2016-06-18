package be.nabu.eai.module.web.application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import netscape.javascript.JSObject;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

// http://blog.mirkosertic.de/javastuff/javafxluaeditor
public class AceEditor {
	
	public enum Action {
		COPY,
		PASTE,
		SAVE,
		CLOSE,
		CHANGE
	}
	
	private WebView webview;
	private Stage stage;
	private boolean loaded;
	private String contentType;
	private String content;
	private Map<Action, KeyCombination> keys = new HashMap<Action, KeyCombination>();
	private Map<Action, List<EventHandler<Event>>> handlers = new HashMap<Action, List<EventHandler<Event>>>();
	
	public AceEditor(Stage stage) {
		this.stage = stage;
		setKeyCombination(Action.COPY, new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN));
		setKeyCombination(Action.PASTE, new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN));
		setKeyCombination(Action.SAVE, new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
		setKeyCombination(Action.CLOSE, new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN));
		subscribe(Action.COPY, new CopyHandler(this));
		subscribe(Action.PASTE, new PasteHandler(this));
		// make sure it doesn't bubble up to the parent
		subscribe(Action.SAVE, new ConsumeHandler());
		subscribe(Action.CLOSE, new ConsumeHandler());
	}
	
	public void setKeyCombination(Action action, KeyCombination combination) {
		if (action == Action.CHANGE) {
			throw new IllegalArgumentException("Can not set a key combination on change, it is the absence of a key combination");
		}
		if (combination == null) {
			this.keys.remove(action);
		}
		else {
			this.keys.put(action, combination);
		}
	}
	
	public void subscribe(Action action, EventHandler<Event> handler) {
		if (!handlers.containsKey(action)) {
			handlers.put(action, new ArrayList<EventHandler<Event>>());
		}
		handlers.get(action).add(0, handler);
	}
	
	private Action getMatch(KeyEvent event) {
		for (Action action : this.keys.keySet()) {
			if (this.keys.get(action).match(event)) {
				return action;
			}
		}
		return null;
	}
	
	public WebView getWebView() {
		if (this.webview == null) {
			synchronized(this) {
				if (this.webview == null) {
					webview = new WebView();
					webview.getEngine().setJavaScriptEnabled(true);

					webview.getEngine().getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {
						@Override
						public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue) {
							if (newValue == State.SUCCEEDED) {
								loaded = true;
								if (content != null) {
									setContentInWebview(contentType, content);
									contentType = null;
									content = null;
								}
							}
						}
					});
					// we provide external menu stuff, the internal ace menu doesn't work too well anyway because copy/paste etc is restricted within javascript
					webview.setContextMenuEnabled(false);

					String externalForm = AceEditor.class.getResource("/ace/editor.html").toExternalForm();
					System.out.println("Found editor: " + externalForm);
					webview.getEngine().load(externalForm);

					webview.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
						@Override
						public void handle(KeyEvent event) {
							Action match = getMatch(event);
							// if no predefined elements were found, use the change action
							if (match != null) {
								List<EventHandler<Event>> list = handlers.get(match);
								if (list != null && !list.isEmpty()) {
									for (EventHandler<Event> handler : list) {
										handler.handle(event);
										if (event.isConsumed()) {
											break;
										}
									}
								}
							}
						}
					});
					webview.addEventFilter(KeyEvent.KEY_TYPED, new EventHandler<KeyEvent>() {
						@Override
						public void handle(KeyEvent event) {
							if (event.getCharacter() != null && !event.getCharacter().isEmpty()) {
								List<EventHandler<Event>> list = handlers.get(Action.CHANGE);
								if (list != null && !list.isEmpty()) {
									for (EventHandler<Event> handler : list) {
										handler.handle(event);
										if (event.isConsumed()) {
											break;
										}
									}
								}
							}
						}
					});
					return webview;
				}
			}
		}
		return webview;
	}
	
	public String getContent() {
		return (String) webview.getEngine().executeScript("getValue()");
	}
	
	public void setContent(String contentType, String content) {
		if (loaded) {
			setContentInWebview(contentType, content);
		}
		else {
			this.contentType = contentType;
			this.content = content;
		}
	}

	public static class CopyHandler implements EventHandler<Event> {
		
		private AceEditor editor;

		public CopyHandler(AceEditor editor) {
			this.editor = editor;
		}
		
		@Override
		public void handle(Event event) {
			String selection = (String) editor.getWebView().getEngine().executeScript("copySelection()");
			Clipboard clipboard = Clipboard.getSystemClipboard();
			ClipboardContent clipboardContent = new ClipboardContent();
			clipboardContent.putString(selection);
			clipboard.setContent(clipboardContent);
			event.consume();
		}
	}
	
	public static class PasteHandler implements EventHandler<Event> {
		
		private AceEditor editor;

		public PasteHandler(AceEditor editor) {
			this.editor = editor;
		}
		
		@Override
		public void handle(Event event) {
			Clipboard clipboard = Clipboard.getSystemClipboard();
			String content = (String) clipboard.getContent(DataFormat.PLAIN_TEXT);
			if (content != null) {
				JSObject window = (JSObject) editor.getWebView().getEngine().executeScript("window");
				window.call("pasteValue", content);
			}
			event.consume();
		}
	}
	
	public static class ConsumeHandler implements EventHandler<Event> {
		@Override
		public void handle(Event event) {
			event.consume();
		}
	}
	
	private void setContentInWebview(String contentType, String content) {
		webview.getEngine().executeScript("resetDiv()");
		// set the content
		Document document = webview.getEngine().getDocument();
		Element editor = document.getElementById("editor");
		editor.setTextContent(content);
		// initialize editor
		webview.getEngine().executeScript("initEditor()");
		String mode = null;
		if ("application/javascript".equals(contentType) || "application/x-javascript".equals(contentType)) {
			mode = "javascript";
		}
		else if ("text/html".equals(contentType)) {
			mode = "html";
		}
		else if ("text/xml".equals(contentType) || "application/xml".equals(contentType)) {
			mode = "xml";
		}
		else if ("text/x-script.glue".equals(contentType)) {
			mode = "python";
		}
		if (mode != null) {
			webview.getEngine().executeScript("editor.getSession().setMode('ace/mode/" + mode + "');");
		}
	}
}
