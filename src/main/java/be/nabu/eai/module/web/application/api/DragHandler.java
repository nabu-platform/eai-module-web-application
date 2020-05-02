package be.nabu.eai.module.web.application.api;

import be.nabu.eai.module.web.application.WebApplication;
import javafx.scene.input.ClipboardContent;

public interface DragHandler {
	public ClipboardContent drop(WebApplication application);
}
