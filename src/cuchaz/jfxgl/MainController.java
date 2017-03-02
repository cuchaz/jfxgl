package cuchaz.jfxgl;

import com.sun.javafx.application.PlatformImpl;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;

public class MainController {
	
	@FXML private CheckBox spinCheck;
	@FXML private Slider rotationSlider;
	@FXML private Label fpsLabel;
	
	public volatile boolean isSpinning;
	public volatile float rotationRadians;

	@FXML
	@CalledByEventsThread
	public void initialize() {
		
		// init defaults
		isSpinning = true;
		spinCheck.selectedProperty().set(isSpinning);
		
		// listen for events
		spinCheck.selectedProperty().addListener((observed, oldVal, newVal) -> {
			isSpinning = newVal;
			rotationSlider.disableProperty().set(isSpinning);
		});
		rotationSlider.valueProperty().addListener((observed, oldVal, newVal) -> {
			rotationRadians = (float)Math.toRadians(newVal.doubleValue());
		});
	}
	
	@CalledByMainThread
	public float update(float elapsedS, float fps) {
		
		if (isSpinning) {
			rotationRadians = (float)(elapsedS*Math.PI);
		}
		
		PlatformImpl.runLater(() -> {
			
			// sync the slider
			float degrees = (float)Math.toDegrees(rotationRadians);
			while (degrees < -180) {
				degrees += 360;
			}
			while (degrees > 180) {
				degrees -= 360;
			}
			rotationSlider.valueProperty().set(degrees);
			
			// update fps
			fpsLabel.textProperty().set(String.format("FPS: %.1f", fps));
		});
		
		return rotationRadians;
	}
}
