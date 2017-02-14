package cuchaz.jfxgl;

import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.Callback;
import org.lwjgl.system.MemoryUtil;

import cuchaz.jfxgl.FXTools.Fxml;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Main {
	
	public static void main(String[] args)
	throws Exception {
		
		// init GLFW
		GLFWErrorCallback.createPrint(System.err).set();
		if (!GLFW.glfwInit()) {
			throw new Error("Can't initialize GLFW");
		}
		
		// create the window
		long hwnd = GLFW.glfwCreateWindow(300, 300, "JFXGL Prototype", MemoryUtil.NULL, MemoryUtil.NULL);
		if (hwnd <= 0) {
			throw new Error("Can't create GLFW window");
		}

		// init opengl
		GLFW.glfwMakeContextCurrent(hwnd);
		GL.createCapabilities();
		Callback debugProc = LWJGLDebug.enableDebugging();
		
		// disable frame limiters (like vsync)
		GLFW.glfwSwapInterval(0);
		
		GL11.glClearColor(0.6f, 0.8f, 0.6f, 1.0f);
		
		JFXGL jfxgl = new JFXGL();
		//FrameTimer timer = new FrameTimer();
		try {
			jfxgl.start(hwnd, args, () -> new MyJavaFXApp());
			
			// render loop
			Log.log("render loop...");
			while (!GLFW.glfwWindowShouldClose(hwnd)) {
				
				// TODO: get JavaFX to render to an off-screen FBO, then render to main FBO
				// clear the framebuf
				//GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

				// do JavaFX stuff
				jfxgl.render();
				
				GLFW.glfwSwapBuffers(hwnd);
				GLFW.glfwPollEvents();
				
				// TEMP: do a little frame limiting
				Thread.sleep(16); // ~60 fps
				//timer.update();
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
	
	public static void demo(String[] args)
	throws Exception {
		
		// make a window using GLFW
		GLFWErrorCallback.createPrint(System.err).set();
		long hwnd = GLFW.glfwCreateWindow(300, 300, "JFXGL Prototype", MemoryUtil.NULL, MemoryUtil.NULL);

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

	public static class MyJavaFXApp extends Application {
		
		@Override
		public void start(Stage stage) {
			
			// load the main fxml
			Fxml<BorderPane,MainController> main = Fxml.load(getClass().getResource("Main.fxml"), BorderPane.class, MainController.class);
			main.controller.stage = stage;
			Scene scene = new Scene(main.node);
			
			// show the window
			stage.setTitle("OpenJFX Test");
			stage.setScene(scene);
			stage.show();
		}
	}
}