package cuchaz.jfxgl;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharModsCallbackI;
import org.lwjgl.glfw.GLFWCursorPosCallbackI;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import org.lwjgl.glfw.GLFWWindowFocusCallbackI;

import com.sun.javafx.application.ParametersImpl;
import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.tk.Toolkit;
import com.sun.prism.es2.JFXGLContext;
import com.sun.prism.es2.JFXGLFactory;

import cuchaz.jfxgl.glass.JFXGLView;
import cuchaz.jfxgl.glass.JFXGLWindow;
import cuchaz.jfxgl.toolkit.JFXGLToolkit;
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
	
	private JFXGLToolkit toolkit;
	private Application app;
	private PlatformImpl.FinishListener finishListener;
	private JFXGLContext context;
	private GLFWCallbacks ourCallbacks;
	private GLFWCallbacks existingCallbacks;
	
	@SuppressWarnings("deprecation")
	public void start(long hwnd, String[] args, Application app) {
		
		// install our glass,toolkit,prism implementations to JavaFX
		System.setProperty("glass.platform", "JFXGL");
		System.setProperty("javafx.toolkit", JFXGLToolkit.class.getName());
		System.setProperty("prism.es2.GLFactory", JFXGLFactory.class.getName());
		System.setProperty("prism.order", "es2");
		
		// TEMP: turn on prism logging so we can see pipeline create/init errors
		//System.setProperty("prism.verbose", "true");
		
		// there's ever only going to be one window, so save the handle globally
		JFXGLFactory.init(hwnd);
		
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
		});
		
		// the app started. track it so we can stop it later
		this.app = app;
		
		// make a context for the calling thread
		// so it doesn't get confused about how to manage context lifecycles
		context = new JFXGLContext(hwnd);
		
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
			if (JFXGLWindow.mainWindow != null) {
				runOnEventsThread(() -> {
					JFXGLView view = (JFXGLView)JFXGLWindow.mainWindow.getView();
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
			if (JFXGLWindow.mainWindow != null) {
				runOnEventsThread(() -> {
					JFXGLView view = (JFXGLView)JFXGLWindow.mainWindow.getView();
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
			if (JFXGLWindow.mainWindow != null) {
				runOnEventsThread(() -> {
					JFXGLView view = (JFXGLView)JFXGLWindow.mainWindow.getView();
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
			if (JFXGLWindow.mainWindow != null) {
				runOnEventsThread(() -> {
					JFXGLView view = (JFXGLView)JFXGLWindow.mainWindow.getView();
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
			if (JFXGLWindow.mainWindow != null) {
				runOnEventsThread(() -> {
					JFXGLView view = (JFXGLView)JFXGLWindow.mainWindow.getView();
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
			if (JFXGLWindow.mainWindow != null) {
				runOnEventsThread(() -> {
					JFXGLWindow.mainWindow.handleGLFWFocus(isFocused);
				});
			}
		};
		existingCallbacks.windowFocus = GLFW.glfwSetWindowFocusCallback(hwnd, ourCallbacks.windowFocus);
	}
	
	public void runOnEventsThread(Runnable runnable) {
		PlatformImpl.runLater(runnable);
	}
	
	public void runOnEventsThreadAndWait(CheckedRunnable runnable) {
		
		AtomicReference<Throwable> eventsException = new AtomicReference<>(null);
		
		PlatformImpl.runAndWait(() -> {
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
	
	/**
	 * This context is used internally by JavaFX. You probably
	 * don't want to use it for your application rendering unless
	 * you don't have anything better already.
	 */
	public JFXGLContext getContext() {
		return context;
	}

	public void render() {
		
		// tell JavaFX stages and scenes to update and send render jobs (on the FX thread)
		toolkit.postPulse();
		
		// process the render jobs from JavaFX
		toolkit.render();
	}
	
	public void terminate() {
		
		try {
			
			if (app != null) {
				
				// call app stop (on the FX thread)
				runOnEventsThreadAndWait(() -> {
					app.stop();
				});
			}
		
		} finally {
			
			// platform cleanup
			PlatformImpl.removeListener(finishListener);
			PlatformImpl.tkExit();
			if (toolkit != null) {
				toolkit.disposePipeline();
			}
		}
	}
}
