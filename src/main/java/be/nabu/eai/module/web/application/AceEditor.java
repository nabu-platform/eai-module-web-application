package be.nabu.eai.module.web.application;

import netscape.javascript.JSObject;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
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
	private WebView webview;
	private String originalContent;
	private Stage stage;
	private boolean loaded;

	public AceEditor(Stage stage) {
		this.stage = stage;
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
								initializeHTML();
							}
						}
					});
					// webview.setContextMenuEnabled(false);

					String externalForm = AceEditor.class.getResource("/ace/editor.html").toExternalForm();
					System.out.println("Found editor: " + externalForm);
					webview.getEngine().load(externalForm);

					// Copy & Paste Clipboard support
					final KeyCombination theCombinationCopy = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN);
					final KeyCombination theCombinationPaste = new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN);
					stage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
						@Override
						public void handle(KeyEvent event) {
							if (theCombinationCopy.match(event)) {
								onCopy();
							}
							if (theCombinationPaste.match(event)) {
								onPaste();
							}
						}
					});
					return webview;
				}
			}
		}
		return webview;
	}
	public WebView build(Stage stage, String originalContent) {
		this.originalContent = originalContent;
		webview = new WebView();
		webview.getEngine().setJavaScriptEnabled(true);

		webview.getEngine().getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {
			@Override
			public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue) {
				if (newValue == State.SUCCEEDED) {
					initializeHTML();
				}
			}
		});
		// webview.setContextMenuEnabled(false);

		String externalForm = AceEditor.class.getResource("/ace/editor.html").toExternalForm();
		System.out.println("Found editor: " + externalForm);
		webview.getEngine().load(externalForm);

		// Copy & Paste Clipboard support
		final KeyCombination theCombinationCopy = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN);
		final KeyCombination theCombinationPaste = new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN);
		stage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if (theCombinationCopy.match(event)) {
					onCopy();
				}
				if (theCombinationPaste.match(event)) {
					onPaste();
				}
			}
		});
		return webview;
	}

	private void onCopy() {
		// Get the selected content from the editor
		// We to a Java2JavaScript downcall here
		// For details, take a look at the function declaration in editor.html
		String theContentAsText = (String) webview.getEngine().executeScript("copyselection()");

		// And put it to the clipboard
		Clipboard theClipboard = Clipboard.getSystemClipboard();
		ClipboardContent theContent = new ClipboardContent();
		theContent.putString(theContentAsText);
		theClipboard.setContent(theContent);
	}

	private void onPaste() {
		// Get the content from the clipboard
		Clipboard theClipboard = Clipboard.getSystemClipboard();
		String theContent = (String) theClipboard.getContent(DataFormat.PLAIN_TEXT);
		if (theContent != null) {
			// And put it in the editor
			// We do a Java2JavaScript downcall here
			// For details, take a look at the function declaration in
			// editor.html
			JSObject theWindow = (JSObject) webview.getEngine().executeScript("window");
			theWindow.call("pastevalue", theContent);
		}
	}

	private void initializeHTML() {
		// Initialize the editor
		// and fill it with the LUA script taken from our editing action
		Document theDocument = webview.getEngine().getDocument();
		Element theEditorElement = theDocument.getElementById("editor");
		theEditorElement.setTextContent(originalContent);
		webview.getEngine().executeScript("initeditor()");
	}
}
