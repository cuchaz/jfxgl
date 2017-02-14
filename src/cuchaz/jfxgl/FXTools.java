package cuchaz.jfxgl;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;

public class FXTools {
	
	public static class Fxml<NT,CT> {

		public final FXMLLoader loader;
		public final NT node;
		public final CT controller;
		
		private Fxml(FXMLLoader loader, NT node, CT controller) {
			this.loader = loader;
			this.node = node;
			this.controller = controller;
		}
		
		public static <NT,CT> Fxml<NT,CT> load(URL url, Class<NT> nodeType) {
			return load(url, nodeType, null);
		}
		
		public static <NT,CT> Fxml<NT,CT> load(URL url, Class<NT> nodeType, Class<CT> controllerType) {
			try {
				
				FXMLLoader loader = new FXMLLoader();
				loader.setLocation(url);
				
				// get the node
				NT node = checkType(loader.load(), nodeType);
				
				// get the controller
				CT controller = null;
				if (controllerType != null) {
					if (loader.getController() == null) {
						throw new Error("FXML doesn't have a controller:\n\t" + url);
					}
					controller = checkType(loader.getController(), controllerType);
				}
				
				return new Fxml<>(loader, node, controller);
				
			} catch (IOException ex) {
				throw new Error(ex);
			}
		}
		
		private static <T> T checkType(Object obj, Class<T> type) {
			
			if (!type.isAssignableFrom(obj.getClass())) {
				throw new ClassCastException("expected a " + type.getName() + ", not a " + obj.getClass().getName());
			}
			
			@SuppressWarnings("unchecked")
			T t = (T)obj;
			
			return t;
		}
	}

	public static <T> void updateListItem(ObservableList<T> list, T item) {
		int i = list.indexOf(item);
		if (i >= 0) {
			list.set(i, item);
		}
	}
	
	public static void schedule(Duration delay, Runnable task) {
		new Timeline(new KeyFrame(delay, (event) -> {
			task.run();
		})).play();
	}
	
	public static interface RepeatingCanceler {
		void cancel();
	}
	
	public static RepeatingCanceler scheduleRepeating(Duration delay, Runnable task) {
		Timeline timeline = new Timeline(new KeyFrame(delay, (event) -> {
			task.run();
		}));
		timeline.cycleCountProperty().set(Timeline.INDEFINITE);
		timeline.play();
		return () -> {
			timeline.stop();
		};
	}
	
	public static class ListChanges<T> {
		
		public final List<T> removals;
		public final List<T> additions;
		public final List<T> updates;
		
		public static interface Listener<T> {
			void onChanges(ListChanges<T> changes);
		}
		
		public static <T> ListChangeListener<? super T> makeListener(Listener<T> listener) {
			return (ListChangeListener.Change<? extends T> change) -> {
				listener.onChanges(new ListChanges<T>(change));
			};
		}
		
		public ListChanges(ListChangeListener.Change<? extends T> change) {
			
			removals = new ArrayList<>();
			additions = new ArrayList<>();
			updates = new ArrayList<>();
			
			while (change.next()) {
				removals.addAll(change.getRemoved());
				additions.addAll(change.getAddedSubList());
			}
			
			// find things in both lists and move to updates
			// this isn't fast, but it should work for small updates
			Iterator<T> iter = removals.iterator();
			while (iter.hasNext()) {
				T thing = iter.next();
				if (additions.contains(thing)) {
					iter.remove();
					additions.remove(thing);
					updates.add(thing);
				}
			}
		}
	}
	
	public static interface SimpleStringConverter<T> {
		String convert(T thing);
	}
	
	public static <T> StringConverter<T> makeStringConverter(SimpleStringConverter<T> converter) {
		return new StringConverter<T>() {

			@Override
			public String toString(T thing) {
				return converter.convert(thing);
			}

			@Override
			public T fromString(String string) {
				// don't need to convert back
				return null;
			}
		};
	}
	
	public static Alert makeAlertInfo(String title, String message) {
		
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setHeaderText("We thought you should know...");
		alert.setContentText(message);
		
		return fixAlert(alert);
	}
	
	public static Alert makeAlertError(String title, String message) {
		return makeAlertError(title, message, null);
	}
	
	public static Alert makeAlertError(String title, String message, Throwable t) {
		
		StringWriter buf = null;
		if (t != null) {
			
			// append the exception message
			message += "\n" + t.getMessage();
			
			// get the exception report
			buf = new StringWriter();
			t.printStackTrace(new PrintWriter(buf));
		}
		
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle(title);
		alert.setHeaderText("An error has occurred.");
		alert.setContentText(message);
		
		if (buf != null) {
			
			TextArea textArea = new TextArea(buf.toString());
			textArea.editableProperty().set(false);
			textArea.wrapTextProperty().set(false);
			textArea.maxWidthProperty().set(Double.MAX_VALUE);
			textArea.maxHeightProperty().set(Double.MAX_VALUE);

			VBox box = new VBox();
			box.getChildren().add(new Label("The exception stacktrace was:"));
			box.getChildren().add(textArea);
			VBox.setVgrow(textArea, Priority.ALWAYS);

			alert.dialogPaneProperty().get().expandableContentProperty().set(box);
			
			// resize the dialog after expansion
			alert.dialogPaneProperty().get().expandedProperty().addListener((pane, oldVal, newVal) -> {
				Platform.runLater(() -> {
					box.requestLayout();
					box.getScene().getWindow().sizeToScene();
				});
			});
		}
		
		return fixAlert(alert);
	}
	
	private static Alert fixAlert(Alert alert) {
		// HACKHACK: resize the alert to actually show the text
		// this is apparently a bug in JavaFX? see:
		// http://stackoverflow.com/questions/28937392/javafx-alerts-and-their-size
		alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
		return alert;
	}
}
