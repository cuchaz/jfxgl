package cuchaz.jfxgl.prism;

import java.util.HashMap;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

import cuchaz.jfxgl.controls.OpenGLPane;

public class JFXGLContexts {
	
	private static JFXGLContext current = null;

	public static JFXGLContext app = null;
	public static JFXGLContext javafx = null;
	
	private static Map<OpenGLPane.OpenGLNode,JFXGLContext> panes = new HashMap<>();
	
	public static JFXGLContext makeNewPane(OpenGLPane.OpenGLNode pane) {
		JFXGLContext context = JFXGLContext.makeNewSharedWith(app.hwnd);
		panes.put(pane, context);
		return context;
	}
	
	public static void cleanupPane(OpenGLPane.OpenGLNode pane) {
		JFXGLContext context = panes.remove(pane);
		if (context != null) {
			context.cleanup();;
		}
	}
	
	public static void cleanup() {
		
		// NOTE: don't cleanup the app context
		// its window is managed by the app
		
		if (javafx != null) {
			javafx.cleanup();
			javafx = null;
		}
	
		for (JFXGLContext context : panes.values()) {
			context.cleanup();
		}
		panes.clear();
	}

	public static JFXGLContext getCurrent() {
		return current;
	}
	
	public static void makeCurrent(JFXGLContext context) {
		if (current != context) {
			current = context;
			GLFW.glfwMakeContextCurrent(current.hwnd);
		}
	}
}
