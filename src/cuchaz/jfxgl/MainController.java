package cuchaz.jfxgl;

import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class MainController {

	@FXML
	private TextField text;
	
	@FXML
	public void initialize() {
		
		text.focusedProperty().addListener((ObservableValue<? extends Boolean> obs, Boolean oldVal, Boolean newVal) -> {
			Log.log("APP: text focus: %b -> %b", oldVal, newVal);
		});
		
		Log.log("APP: init controller");
	}
	
}
