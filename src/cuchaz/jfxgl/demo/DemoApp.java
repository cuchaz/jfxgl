package cuchaz.jfxgl.demo;

import java.io.IOException;

import cuchaz.jfxgl.CalledByEventsThread;
import cuchaz.jfxgl.CalledByMainThread;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class DemoApp extends Application {
		
	public MainController controller;
	
	@CalledByMainThread
	public DemoApp() {
		controller = null;
	}
	
	@Override
	@CalledByEventsThread
	public void start(Stage stage)
	throws IOException {
		
		// load the main fxml
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(getClass().getResource("Main.fxml"));
		Scene scene = new Scene(loader.load());
		stage.setScene(scene);
		
		// set transparency for ui overlay
		scene.setFill(null);
		stage.initStyle(StageStyle.TRANSPARENT);
		
		// the window is actually already showing, but JavaFX doesn't know that yet
		// so make JavaFX catch up by "showing" the window
		stage.show();
	
		controller = loader.getController();
	}
}
