package cuchaz.jfxgl;

import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharModsCallbackI;
import org.lwjgl.glfw.GLFWCursorPosCallbackI;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import org.lwjgl.glfw.GLFWWindowFocusCallbackI;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.Callback;
import org.lwjgl.system.MemoryUtil;

import cuchaz.jfxgl.FXTools.Fxml;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Main {
	
	public static void main(String[] args)
	throws Exception {
		
		// init GLFW
		GLFWErrorCallback.createPrint(System.err).set();
		if (!GLFW.glfwInit()) {
			throw new Error("Can't initialize GLFW");
		}
		
		// create the window
		long hwnd = GLFW.glfwCreateWindow(600, 338, "JFXGL Prototype", MemoryUtil.NULL, MemoryUtil.NULL);
		if (hwnd <= 0) {
			throw new Error("Can't create GLFW window");
		}
		
		// init opengl
		GLFW.glfwMakeContextCurrent(hwnd);
		GL.createCapabilities();
		Callback debugProc = LWJGLDebug.enableDebugging();
		
		// disable frame limiters (like vsync)
		GLFW.glfwSwapInterval(0);
		
		GL11.glClearColor(0f, 0f, 0f, 1.0f);
		
		JFXGL jfxgl = new JFXGL();
		FrameTimer timer = new FrameTimer();
		try {
			
			// start the app
			MyJavaFXApp app = jfxgl.start(hwnd, args, () -> new MyJavaFXApp());
			
			// listen for input events
			// NOTE: always keep a strong reference to callbacks, or they get garbage collected
			GLFWKeyCallbackI keyCallback = (long hwndAgain, int key, int scanCode, int action, int mods) -> {
				jfxgl.key(key, scanCode, action, mods);
			};
			GLFW.glfwSetKeyCallback(hwnd, keyCallback);
			
			GLFWCharModsCallbackI charCallback = (long hwndAgain, int codepoint, int mods) -> {
				jfxgl.keyChar(codepoint, mods);
			};
			GLFW.glfwSetCharModsCallback(hwnd, charCallback);
			
			GLFWCursorPosCallbackI cursorPosCallback = (long hwndAgain, double xpos, double ypos) -> {
				jfxgl.cursorPos(xpos, ypos);
			};
			GLFW.glfwSetCursorPosCallback(hwnd, cursorPosCallback);
			
			GLFWMouseButtonCallbackI mouseButtonCallback = (long hwndAgain, int button, int action, int mods) -> {
				jfxgl.mouseButton(button, action, mods);
			};
			GLFW.glfwSetMouseButtonCallback(hwnd, mouseButtonCallback);
			
			GLFWScrollCallbackI scrollCallback = (long hwndAgain, double dx, double dy) -> {
				jfxgl.scroll(dx, dy);
			};
			GLFW.glfwSetScrollCallback(hwnd, scrollCallback);
			
			GLFWWindowFocusCallbackI focusCallback = (long hwndAgain, boolean isFocused) -> {
				jfxgl.focus(isFocused);
			};
			GLFW.glfwSetWindowFocusCallback(hwnd, focusCallback);
			
			// poll app ui for state
			
			// init triangle rendering
			long startTimeNs = System.nanoTime();
			TriangleRenderer triangle = new TriangleRenderer(jfxgl.getContext());
			
			// render loop
			Log.log("render loop...");
			while (!GLFW.glfwWindowShouldClose(hwnd)) {
				
				// clear the framebuf
				GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
				
				// update the triangle
				long elapsedNs = System.nanoTime() - startTimeNs;
				float elapsedS = (float)elapsedNs/1000/1000/1000;
				float rotationRadians = app.controller.update(elapsedS, timer.fps);
				triangle.render(rotationRadians);

				// do JavaFX stuff
				jfxgl.render();
				
				GLFW.glfwSwapBuffers(hwnd);
				GLFW.glfwPollEvents();
				
				// TEMP: do a little frame limiting
				//Thread.sleep(16); // ~60 fps
				timer.update();
			}
			Log.log("render loop finished");
			
		} finally {
			jfxgl.terminate();
		}

		// cleanup
		debugProc.free();
		Callbacks.glfwFreeCallbacks(hwnd);
		GLFW.glfwDestroyWindow(hwnd);
		GLFW.glfwTerminate();
		GLFW.glfwSetErrorCallback(null).free();
		
		Log.log("done!");
	}
	
	/* TEMP: DEMO
	public static void demo(String[] args)
	throws Exception {
		
		// make a window using GLFW
		GLFWErrorCallback.createPrint(System.err).set();
		long hwnd = GLFW.glfwCreateWindow(500, 300, "JFXGL Prototype", MemoryUtil.NULL, MemoryUtil.NULL);

		// init LWJGL/OpenGL
		GLFW.glfwMakeContextCurrent(hwnd);
		GL.createCapabilities();
		
		JFXGL jfxgl = new JFXGL();
		try {
			
			// start JavaFX renderer
			jfxgl.start(hwnd, args, () -> new MyJavaFXApp());
			
			// main render loop
			while (!GLFW.glfwWindowShouldClose(hwnd)) {
				jfxgl.render();
				GLFW.glfwSwapBuffers(hwnd);
				GLFW.glfwPollEvents();
			}
			
		} finally {
			jfxgl.terminate();
		}

		// cleanup
		Callbacks.glfwFreeCallbacks(hwnd);
		GLFW.glfwDestroyWindow(hwnd);
		GLFW.glfwTerminate();
		GLFW.glfwSetErrorCallback(null).free();
	}
	*/
	
	public static class MyJavaFXApp extends Application {
		
		public MainController controller;
		
		@Override
		public void start(Stage stage) {
			
			// load the main fxml
			Fxml<AnchorPane,MainController> main = Fxml.load(getClass().getResource("Main.fxml"), AnchorPane.class, MainController.class);
			this.controller = main.controller;
			Scene scene = new Scene(main.node);
			stage.setScene(scene);
			
			// set transparency for ui overlay
			scene.setFill(null);
			stage.initStyle(StageStyle.TRANSPARENT);
			
			// the window is actually already showing, but JavaFX doesn't know that yet
			// so make JavaFX catch up by "showing" the window
			stage.show();
		}
	}
}