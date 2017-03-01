package cuchaz.jfxgl;

import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class MainController {

	@FXML private TextField text;
	@FXML private ListView<String> list;
	
	@FXML
	public void initialize() {
		
		text.focusedProperty().addListener((ObservableValue<? extends Boolean> obs, Boolean oldVal, Boolean newVal) -> {
			Log.log("APP: text focus: %b -> %b", oldVal, newVal);
		});
		
		Log.log("APP: init controller");
	
		ObservableList<String> items = list.getItems();
		items.add("Cheese");
		items.add("Green");
		items.add("Cats");
	}
}
