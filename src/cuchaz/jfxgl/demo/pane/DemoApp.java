package cuchaz.jfxgl.demo.pane;

import java.io.IOException;

import cuchaz.jfxgl.CalledByEventsThread;
import cuchaz.jfxgl.CalledByMainThread;
import cuchaz.jfxgl.JFXGL;
import cuchaz.jfxgl.JFXGLApplication;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class DemoApp extends Application implements JFXGLApplication {
		
	public MainController controller;
	public Scene scene;
	
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
		scene = new Scene(loader.load());
		stage.setScene(scene);
		
		// the window is actually already showing, but JavaFX doesn't know that yet
		// so make JavaFX catch up by "showing" the window
		stage.show();
	
		controller = loader.getController();
	}
	
	@Override
	@CalledByMainThread
	public void initJFXGL(JFXGL jfxgl) {
		
		// since we're using an OpenGLPane, make sure our scene always gets repainted
		jfxgl.alwaysRepaintScenes.add(scene);
	}
}
