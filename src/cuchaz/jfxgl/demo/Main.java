package cuchaz.jfxgl.demo;

import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.Callback;
import org.lwjgl.system.MemoryUtil;

import cuchaz.jfxgl.JFXGL;
import cuchaz.jfxgl.LWJGLDebug;

public class Main {
	
	public static void main(String[] args)
	throws Exception {
		
		// init GLFW
		GLFWErrorCallback.createPrint(System.err).set();
		if (!GLFW.glfwInit()) {
			throw new Error("Can't initialize GLFW");
		}
		
		// create the window
		long hwnd = GLFW.glfwCreateWindow(600, 338, "JFXGL Demo", MemoryUtil.NULL, MemoryUtil.NULL);
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
			DemoApp app = new DemoApp();
			jfxgl.start(hwnd, args, app);
			
			// init triangle rendering
			TriangleRenderer triangle = new TriangleRenderer(jfxgl.getContext());
			
			// render loop
			while (!GLFW.glfwWindowShouldClose(hwnd)) {
				
				// clear the framebuf
				GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
				
				// update the triangle
				app.controller.update(timer.fps);
				triangle.render(app.controller.rotationRadians);

				// do JavaFX stuff
				jfxgl.render();
				
				GLFW.glfwSwapBuffers(hwnd);
				GLFW.glfwPollEvents();
				
				timer.update();
			}
			
		} finally {
			jfxgl.terminate();
		}

		// cleanup
		debugProc.free();
		Callbacks.glfwFreeCallbacks(hwnd);
		GLFW.glfwDestroyWindow(hwnd);
		GLFW.glfwTerminate();
		GLFW.glfwSetErrorCallback(null).free();
	}
}