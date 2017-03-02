package cuchaz.jfxgl.toolkit;

import java.security.AccessControlContext;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.Screen;
import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.embed.HostInterface;
import com.sun.javafx.tk.AppletWindow;
import com.sun.javafx.tk.RenderJob;
import com.sun.javafx.tk.TKStage;
import com.sun.javafx.tk.quantum.QuantumToolkit;
import com.sun.prism.GraphicsPipeline;
import com.sun.prism.es2.ES2Pipeline;
import com.sun.prism.impl.PrismSettings;
import com.sun.scenario.DelayedRunnable;

import cuchaz.jfxgl.Log;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public class JFXGLToolkit extends QuantumToolkit {
	
	private ES2Pipeline pipeline;
	private JFXGLRenderer renderer;
	private JFXGLPaintCollector paintCollector;
	private AtomicBoolean pulseRequested;
	private AtomicBoolean animationRunning;
	private DelayedRunnable animationRunnable;

	@Override
	public boolean init() {
		
		// TEMP
		Log.log("LWJGLToolkit.init()");

		// create the opengl pipeline
		GraphicsPipeline maybeAnyPipeline = GraphicsPipeline.createPipeline();
		if (maybeAnyPipeline == null || !(maybeAnyPipeline instanceof ES2Pipeline)) {
			throw new RuntimeException("JavaFX OpenGL initialization failed");
		}
		pipeline = (ES2Pipeline)maybeAnyPipeline;

		// copy device details to application
		@SuppressWarnings("unchecked")
		Map<Object, Object> deviceDetails = pipeline.getDeviceDetails();
		deviceDetails.put(com.sun.glass.ui.View.Capability.kHiDPIAwareKey, PrismSettings.allowHiDPIScaling);
		@SuppressWarnings("unchecked")
		Map<Object, Object> appsDeviceDetails = com.sun.glass.ui.Application.getDeviceDetails();
		if (appsDeviceDetails != null) {
			deviceDetails.putAll(appsDeviceDetails);
		}
		com.sun.glass.ui.Application.setDeviceDetails(deviceDetails);
		
		renderer = new JFXGLRenderer();
		paintCollector = new JFXGLPaintCollector(this);
		pulseRequested = new AtomicBoolean(false);
		animationRunning = new AtomicBoolean(false);
		animationRunnable = null;

		return true;
	}
	
	@Override
	public void startup(Runnable r) {
		
		// TEMP
		Log.log("LWJGLToolkit.startup()");
		
		CountDownLatch startupLatch = new CountDownLatch(1);
		Application.run(() -> {
			
			// set the thread used by checkFxUserThread()
			setFxUserThread(Thread.currentThread());

			// tie screens to the pipeline
			for (Screen screen : Screen.getScreens()) {
				screen.setAdapterOrdinal(pipeline.getAdapterOrdinal(screen));
			}
			
			Application.GetApplication().setEventHandler(new Application.EventHandler() {
				@Override
				public void handleQuitAction(Application app, long time) {
					// TODO: handle this?
					Log.log("WARNING: ignoring LWJGLToolkit.startup().handleQuitAction()");
					//com.sun.javafx.tk.quantum.GlassStage.requestClosingAllWindows();
				}

				@Override
				public boolean handleThemeChanged(String themeName) {
					return PlatformImpl.setAccessibilityTheme(themeName);
				}
			});
			
			r.run();
			
			javafx.stage.Screen.getPrimary();
			
			startupLatch.countDown();
		});
		
		// wait for Application.run() to finish
		try {
			startupLatch.await();
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}
		
		// create OpenGL context for the main thread
		ES2Pipeline.getDefaultResourceFactory();
		// TEMP
		Log.log("created OpenGL context");
	}
	
	@Override
	public void postPulse() {
		
		boolean shouldPulse = paintCollector.hasDirty()
			|| pulseRequested.get()
			|| animationRunning.get();
			
		if (shouldPulse) {
			pulseRequested.set(false);
			
			if (animationRunnable != null) {
				animationRunning.set(true);
				Application.invokeLater(animationRunnable);
			} else {
				animationRunning.set(false);
			}
			
			Application.invokeLater(() -> {
				firePulse();
				paintCollector.renderAll();
			});
		}
	}

	@Override
	public void requestNextPulse() {
		pulseRequested.set(true);
	}
	
	@Override
	public void setAnimationRunnable(DelayedRunnable val) {
		if (val != null) {
			animationRunning.set(true);
		}
		animationRunnable = val;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Future addRenderJob(RenderJob r) {
		return renderer.submitRenderJob(r);
	}
	
	public void render() {
		renderer.render();
	}
	
	@Override
	public void exit() {
		
		// we should be on the FX thread
		checkFxUserThread();

		notifyShutdownHooks();

		// terminate the application
		Application app = Application.GetApplication();
		app.terminate();
		
		clearFxUserThread();
	}

	public void disposePipeline() {
		
		// NOTE: must be called on same thread as init (eg, main thread)
		pipeline.dispose();
	}
	
	@Override
	public void defer(Runnable r) {
		Application.invokeLater(r);
	}
	
	@Override
	public void checkFxUserThread() {
		if (!isFxUserThread()) {
			throw new IllegalStateException("Not on FX application thread; currentThread = " + Thread.currentThread().getName());
		}
	}
	
	@Override
	public boolean canStartNestedEventLoop() {
		return false;
	}

	@Override
	public Object enterNestedEventLoop(Object key) {
		throw new UnsupportedOperationException("nested event loops not implemented");
	}

	@Override
	public void exitNestedEventLoop(Object key, Object rval) {
		throw new UnsupportedOperationException("nested event loops not implemented");
	}

	@Override
	public boolean isNestedLoopRunning() {
		return false;
	}
	
	@Override
	public TKStage createTKPopupStage(Window peerWindow, StageStyle popupStyle, TKStage owner, AccessControlContext acc) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TKStage createTKEmbeddedStage(HostInterface host, AccessControlContext acc) {
		throw new UnsupportedOperationException();
	}

	@Override
	public AppletWindow createAppletWindow(long parent, String serverName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void closeAppletWindow() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void waitFor(Task t) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean isVsyncEnabled() {
		return false;
	}
}
