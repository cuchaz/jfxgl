/*************************************************************************
 * Copyright (C) 2017, Jeffrey W. Martin "Cuchaz"
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License Version 2 with
 * the classpath exception, as published by the Free Software Foundation.
 * 
 * See LICENSE.txt in the project root folder for the full license.
 *************************************************************************/
package cuchaz.jfxgl;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharModsCallbackI;
import org.lwjgl.glfw.GLFWCursorPosCallbackI;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import org.lwjgl.glfw.GLFWWindowFocusCallbackI;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.Callback;

import com.sun.glass.ui.jfxgl.JFXGLMainWindow;
import com.sun.glass.ui.jfxgl.JFXGLPlatformFactory;
import com.sun.glass.ui.jfxgl.JFXGLView;
import com.sun.javafx.application.ParametersImpl;
import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.tk.Toolkit;
import com.sun.javafx.tk.quantum.JFXGLToolkit;
import com.sun.prism.es2.JFXGLContext;
import com.sun.prism.es2.JFXGLContexts;
import com.sun.prism.es2.JFXGLFactory;

import javafx.application.Application;
import javafx.stage.Stage;

public class JFXGL {
	
	public static interface CheckedRunnable {
		void run() throws Exception;
	}
	
	private static class GLFWCallbacks {
		public GLFWKeyCallbackI key = null;
		public GLFWCharModsCallbackI keyChar = null;
		public GLFWCursorPosCallbackI cursorPos = null;
		public GLFWMouseButtonCallbackI mouseButton = null;
		public GLFWScrollCallbackI scroll = null;
		public GLFWWindowFocusCallbackI windowFocus = null;
	}
	
	private static JFXGLToolkit toolkit;
	private static Application app;
	private static PlatformImpl.FinishListener finishListener;
	private static GLFWCallbacks ourCallbacks;
	private static GLFWCallbacks existingCallbacks;
	private static Callback debugCallback;
	
	private JFXGL() {
		// static only class, don't instantiate
	}
	
	@SuppressWarnings("deprecation")
	public static JFXGLContext start(long hwnd, String[] args, Application app) {
		
		// init the app OpenGL contexts
		JFXGLContexts.app = JFXGLContext.wrapExisting(hwnd);
		
		// init the JavaFX OpenGL context
		JFXGLContexts.javafx = JFXGLContext.makeNewSharedWith(JFXGLContexts.app.hwnd);
		JFXGLContexts.javafx.makeCurrent();
		debugCallback = LWJGLDebug.enableDebugging();
		
		// init OpenGL state expected by JavaFX rendering
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
		
		// make sure JavaFX is using the OpenGL prism backend
		System.setProperty("prism.order", "es2");
		
		// DEBUG: turn on prism logging so we can see pipeline create/init errors
		//System.setProperty("prism.verbose", "true");
		
		// install our various pieces into JavaFX
		JFXGLPlatformFactory.install();
		JFXGLFactory.install();
		JFXGLToolkit.install();
		
		try {
			
			// start the platform
			CountDownLatch startupLatch = new CountDownLatch(1);
			PlatformImpl.startup(() -> {
				startupLatch.countDown();
			});
			startupLatch.await();
			
			toolkit = (JFXGLToolkit)Toolkit.getToolkit();
			
		} catch (InterruptedException ex) {
			throw new Error(ex);
		}
		
		// go back to main context
		JFXGLContexts.app.makeCurrent();
		
		// translate the String[] args into JavaFX Parameters
		ParametersImpl.registerParameters(app, new ParametersImpl(args));
		PlatformImpl.setApplicationName(app.getClass());
		
		// call app init (on this thread)
		// it's basically a second constructor
		try {
			app.init();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		
		// call app start (on the FX thread)
		runOnEventsThreadAndWait(() -> {

			// make the sage
			Stage primaryStage = new Stage();
			primaryStage.impl_setPrimary(true);
			
			// start() the app
			app.start(primaryStage);
			
			// the window is actually already showing, but JavaFX doesn't know that yet
			// so make JavaFX catch up by "showing" the window
			primaryStage.show();
		});
		
		// the app started. track it so we can stop it later
		JFXGL.app = app;
		
		// listen for input events from GLFW
		// NOTE: always keep a strong reference to GLFW callbacks, or they get garbage collected
		ourCallbacks = new GLFWCallbacks();
		existingCallbacks = new GLFWCallbacks();
		
		// TODO: allow main app to disable input forwarding
		// NOTE: callbacks are called on the main thread, so forward to events thread when necessary
		ourCallbacks.key = (long hwndAgain, int key, int scanCode, int action, int mods) -> {
			if (existingCallbacks.key != null) {
				existingCallbacks.key.invoke(hwnd, key, scanCode, action, mods);
			}
			if (JFXGLMainWindow.instance != null) {
				runOnEventsThread(() -> {
					JFXGLView view = (JFXGLView)JFXGLMainWindow.instance.getView();
					if (view != null) {
						view.handleGLFWKey(key, scanCode, action, mods);
					}
				});
			}
		};
		existingCallbacks.key = GLFW.glfwSetKeyCallback(hwnd, ourCallbacks.key);
		
		ourCallbacks.keyChar = (long hwndAgain, int codepoint, int mods) -> {
			if (existingCallbacks.keyChar != null) {
				existingCallbacks.keyChar.invoke(hwnd, codepoint, mods);
			}
			if (JFXGLMainWindow.instance != null) {
				runOnEventsThread(() -> {
					JFXGLView view = (JFXGLView)JFXGLMainWindow.instance.getView();
					if (view != null) {
						view.handleGLFWKeyChar(codepoint, mods);
					}
				});
			}
		};
		existingCallbacks.keyChar = GLFW.glfwSetCharModsCallback(hwnd, ourCallbacks.keyChar);
		
		ourCallbacks.cursorPos = (long hwndAgain, double xpos, double ypos) -> {
			if (existingCallbacks.cursorPos != null) {
				existingCallbacks.cursorPos.invoke(hwnd, xpos, ypos);
			}
			if (JFXGLMainWindow.instance != null) {
				runOnEventsThread(() -> {
					JFXGLView view = (JFXGLView)JFXGLMainWindow.instance.getView();
					if (view != null) {
						view.handleGLFWCursorPos(xpos, ypos);
					}
				});
			}
		};
		existingCallbacks.cursorPos = GLFW.glfwSetCursorPosCallback(hwnd, ourCallbacks.cursorPos);
		
		ourCallbacks.mouseButton = (long hwndAgain, int button, int action, int mods) -> {
			if (existingCallbacks.mouseButton != null) {
				existingCallbacks.mouseButton.invoke(hwnd, button, action, mods);
			}
			if (JFXGLMainWindow.instance != null) {
				runOnEventsThread(() -> {
					JFXGLView view = (JFXGLView)JFXGLMainWindow.instance.getView();
					if (view != null) {
						view.handleGLFWMouseButton(button, action, mods);
					}
				});
			}
		};
		existingCallbacks.mouseButton = GLFW.glfwSetMouseButtonCallback(hwnd, ourCallbacks.mouseButton);
		
		ourCallbacks.scroll = (long hwndAgain, double dx, double dy) -> {
			if (existingCallbacks.scroll != null) {
				existingCallbacks.scroll.invoke(hwnd, dx, dy);
			}
			if (JFXGLMainWindow.instance != null) {
				runOnEventsThread(() -> {
					JFXGLView view = (JFXGLView)JFXGLMainWindow.instance.getView();
					if (view != null) {
						view.handleGLFWScroll(dx, dy);
					}
				});
			}
		};
		existingCallbacks.scroll = GLFW.glfwSetScrollCallback(hwnd, ourCallbacks.scroll);
		
		ourCallbacks.windowFocus = (long hwndAgain, boolean isFocused) -> {
			if (existingCallbacks.windowFocus != null) {
				existingCallbacks.windowFocus.invoke(hwnd, isFocused);
			}
			if (JFXGLMainWindow.instance != null) {
				runOnEventsThread(() -> {
					JFXGLMainWindow.instance.handleGLFWFocus(isFocused);
				});
			}
		};
		existingCallbacks.windowFocus = GLFW.glfwSetWindowFocusCallback(hwnd, ourCallbacks.windowFocus);
		
		return JFXGLContexts.app;
	}
	
	public static void runOnEventsThread(CheckedRunnable runnable) {
		JFXGLToolkit.runLater(() -> {
			try {
				runnable.run();
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		});
	}
	
	public static void runOnEventsThreadAndWait(CheckedRunnable runnable) {
		
		AtomicReference<Throwable> eventsException = new AtomicReference<>(null);
		
		JFXGLToolkit.runAndWait(() -> {
			try {
				runnable.run();
			} catch (Throwable t) {
				eventsException.set(t);
			}
		});
		
		Throwable t = eventsException.get();
		if (t != null) {
			throw new RuntimeException(t);
		}
	}
	
	public static boolean runOnEventsThreadAndWait(CheckedRunnable runnable, int timeout, TimeUnit timeUnit) {
		
		AtomicReference<Throwable> eventsException = new AtomicReference<>(null);
		CountDownLatch countdown = new CountDownLatch(1);

		JFXGLToolkit.runLater(() -> {
			try {
				runnable.run();
				countdown.countDown();
			} catch (Throwable t) {
				eventsException.set(t);
			}
		});
		
		boolean isFinished;
		try {
			isFinished = countdown.await(3, TimeUnit.SECONDS);
		} catch (InterruptedException ex) {
			throw new RuntimeException(ex);
		}
		
		Throwable t = eventsException.get();
		if (t != null) {
			throw new RuntimeException(t);
		}
		
		return isFinished;
	}
	
	/**
	 * Renders the JavaFX UI into the current framebuffer.
	 * <p>
	 * After rendering is complete, the OpenGL state is restored to what it was before calling render(). 
	 */
	public static void render() {
		
		// tell JavaFX stages and scenes to update and send render jobs (on the FX thread)
		toolkit.postPulse();
		
		// process the render jobs from JavaFX
		toolkit.render();
	}

	public static void terminate() {
		
		// is the events thread still working?
		boolean canRunEvents;
		try {
			canRunEvents = runOnEventsThreadAndWait(() -> {}, 5, TimeUnit.SECONDS);
		} catch (EventsThreadNotRunningException ex) {
			canRunEvents = false;
		}
		
		if (!canRunEvents) {
			System.out.println("WARNING: JavaFX events thread not responding in a timely manner. Some event must have jammed it.");
		}
		
		try {
			
			if (app != null && canRunEvents) {
				
				// call app stop (on the FX thread)
				runOnEventsThreadAndWait(() -> {
					app.stop();
				});
			}
		
		} finally {
			
			// platform cleanup
			PlatformImpl.removeListener(finishListener);
			if (canRunEvents) {
				PlatformImpl.tkExit();
			}
			if (toolkit != null) {
				toolkit.disposePipeline();
			}
			if (debugCallback != null) {
				debugCallback.free();
				debugCallback = null;
			}
			JFXGLContexts.cleanup();
		}
	}

	public static void renderLoop() {
		
		long appHwnd = JFXGLContexts.app.hwnd;
		
		GL11.glClearColor(0f, 0f, 0f, 1f);
		
		while (!GLFW.glfwWindowShouldClose(appHwnd)) {
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
			render();
			GLFW.glfwSwapBuffers(appHwnd);
			GLFW.glfwPollEvents();
		}
	}
}
