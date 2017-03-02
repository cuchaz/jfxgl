package cuchaz.jfxgl;

import java.util.concurrent.CountDownLatch;

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

	public static interface ApplicationFactory<T extends Application> {
		T makeApplication();
	}
	
	private JFXGLToolkit toolkit;
	private Application app;
	private PlatformImpl.FinishListener finishListener;
	private JFXGLContext context;
	
	public volatile boolean keepRendering;

	@SuppressWarnings("deprecation")
	public <T extends Application> T start(long hwnd, String[] args, ApplicationFactory<T> factory) {
		
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
			Log.log("starting platform...");
			CountDownLatch startupLatch = new CountDownLatch(1);
			PlatformImpl.startup(() -> {
				startupLatch.countDown();
			});
			startupLatch.await();
			Log.log("platform started");
			
			toolkit = (JFXGLToolkit)Toolkit.getToolkit();
			
		} catch (InterruptedException ex) {
			throw new Error(ex);
		}
		
		// use the keep rendering flag to track errors for now
		keepRendering = true;
		
		// call the app constructor (on the FX thread)
		Log.log("app()");
		PlatformImpl.runAndWait(() -> {
			try {
				
				app = factory.makeApplication();
				
				// translate the String[] args into JavaFX Parameters
				ParametersImpl.registerParameters(app, new ParametersImpl(args));
				PlatformImpl.setApplicationName(app.getClass());
				
			} catch (Throwable t) {
				System.err.println("Exception in Application constructor");
				t.printStackTrace(System.err);
				keepRendering = false;
			}
		});
		
		// no really, this is safe, I promise
		@SuppressWarnings("unchecked")
		T typedApp = (T)app;
		
		if (!keepRendering) {
			return typedApp;
		}
		
		// call app init (on this thread)
		Log.log("app.init()");
		try {
			app.init();
		} catch (Throwable t) {
			System.err.println("Exception in Application init method");
			t.printStackTrace(System.err);
			keepRendering = false;
		}
		
		if (!keepRendering) {
			return typedApp;
		}
		
		// call app start (on the FX thread)
		Log.log("app.start()");
		PlatformImpl.runAndWait(() -> {
			try {
				
				// make the sage
				Stage primaryStage = new Stage();
				primaryStage.impl_setPrimary(true);
				app.start(primaryStage);
				
			} catch (Throwable t) {
				System.err.println("Exception in Application start method");
				t.printStackTrace(System.err);
				keepRendering = false;
			}
		});
		
		if (!keepRendering) {
			return typedApp;
		}
		
		// setup app exit listener
		finishListener = new PlatformImpl.FinishListener() {
			
			@Override
			public void idle(boolean implicitExit) {
				if (implicitExit) {
					keepRendering = false;
					return;
				}
			}

			@Override
			public void exitCalled() {
				keepRendering = false;
			}
		};
		PlatformImpl.addListener(finishListener);
		
		// make a context for the calling thread
		// so it doesn't get confused about how to manage context lifecycles
		context = new JFXGLContext(hwnd);
		
		return typedApp;
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
		
		if (app != null) {
			
			// call app stop (on the FX thread)
			Log.log("app.stop()");
			PlatformImpl.runAndWait(() -> {
				try {
					app.stop();
				} catch (Throwable t) {
					System.err.println("Exception in Application stop method");
					t.printStackTrace(System.err);
				}
			});
		}
		
		// platform cleanup
		Log.log("platform cleanup");
		PlatformImpl.removeListener(finishListener);
		PlatformImpl.tkExit();
		if (toolkit != null) {
			toolkit.disposePipeline();
		}
		Log.log("platform finished");
	}

	public void key(int key, int scanCode, int action, int mods) {
		
		if (JFXGLWindow.mainWindow != null) {
			
			// we're on the main thread, so send to events thread
			PlatformImpl.runLater(() -> {
				JFXGLView view = (JFXGLView)JFXGLWindow.mainWindow.getView();
				if (view != null) {
					view.handleGLFWKey(key, scanCode, action, mods);
				}
			});
		}
	}
	
	public void keyChar(int codepoint, int mods) {
		if (JFXGLWindow.mainWindow != null) {
			
			// we're on the main thread, so send to events thread
			PlatformImpl.runLater(() -> {
				JFXGLView view = (JFXGLView)JFXGLWindow.mainWindow.getView();
				if (view != null) {
					view.handleGLFWKeyChar(codepoint, mods);
				}
			});
		}
	}

	public void cursorPos(double x, double y) {
		
		if (JFXGLWindow.mainWindow != null) {
			
			// we're on the main thread, so send to events thread
			PlatformImpl.runLater(() -> {
				JFXGLView view = (JFXGLView)JFXGLWindow.mainWindow.getView();
				if (view != null) {
					view.handleGLFWCursorPos(x, y);
				}
			});
		}
	}

	public void mouseButton(int button, int action, int mods) {
		
		if (JFXGLWindow.mainWindow != null) {
			
			// we're on the main thread, so send to events thread
			PlatformImpl.runLater(() -> {
				JFXGLView view = (JFXGLView)JFXGLWindow.mainWindow.getView();
				if (view != null) {
					view.handleGLFWMouseButton(button, action, mods);
				}
			});
		}
	}

	public void scroll(double dx, double dy) {
		
		if (JFXGLWindow.mainWindow != null) {
			
			// we're on the main thread, so send to events thread
			PlatformImpl.runLater(() -> {
				JFXGLView view = (JFXGLView)JFXGLWindow.mainWindow.getView();
				if (view != null) {
					view.handleGLFWScroll(dx, dy);
				}
			});
		}
	}

	public void focus(boolean isFocused) {
		
		if (JFXGLWindow.mainWindow != null) {
			
			// we're on the main thread, so send to events thread
			PlatformImpl.runLater(() -> {
				JFXGLWindow.mainWindow.handleGLFWFocus(isFocused);
			});
		}
	}
}
