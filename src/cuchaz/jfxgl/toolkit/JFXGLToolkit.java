/*************************************************************************
 * Copyright (C) 2017, Jeffrey W. Martin "Cuchaz"
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License Version 2 with
 * the classpath exception, as published by the Free Software Foundation.
 * 
 * See LICENSE.txt in the project root folder for the full license.
 *************************************************************************/
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
import com.sun.javafx.tk.Toolkit;
import com.sun.javafx.tk.quantum.QuantumToolkit;
import com.sun.javafx.tk.quantum.ViewScene;
import com.sun.prism.GraphicsPipeline;
import com.sun.prism.es2.ES2Pipeline;
import com.sun.prism.impl.PrismSettings;
import com.sun.scenario.DelayedRunnable;

import cuchaz.jfxgl.InJavaFXGLContext;
import javafx.scene.Scene;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public class JFXGLToolkit extends QuantumToolkit {
	
	public static void install() {
		JFXGLToolkit tk = new JFXGLToolkit();
		tk.init();
		Toolkit.TOOLKIT = tk;
	}
	
	private ES2Pipeline pipeline;
	private JFXGLRenderer renderer;
	private JFXGLPaintCollector paintCollector;
	private AtomicBoolean pulseRequested;
	private AtomicBoolean animationRunning;
	private DelayedRunnable animationRunnable;

	@Override
	@InJavaFXGLContext
	public boolean init() {
		
		// create the opengl pipeline
		GraphicsPipeline maybeAnyPipeline = GraphicsPipeline.createPipeline();
		if (maybeAnyPipeline == null) {
			throw new RuntimeException("JavaFX render init failed to create a graphics pipeline");
		} else if (!(maybeAnyPipeline instanceof ES2Pipeline)) {
			throw new RuntimeException("JavaFX render init failed to create the OpenGL graphics pipeline");
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
	@InJavaFXGLContext
	public void startup(Runnable r) {
		
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
					throw new UnsupportedOperationException("implement me!");
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
		
		// init JavaFX OpenGL rendering
		ES2Pipeline.getDefaultResourceFactory();
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

	public void addRepaintSceneRenderJob(Scene scene) {
		@SuppressWarnings("deprecation")
		ViewScene viewScene = (ViewScene)scene.impl_getPeer();
		viewScene.repaint();
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
	public boolean shouldWaitForRenderingToComplete() {
		// nope
		return false;
	}
	
	@Override
	public void exit() {
		
		// we should be on the FX thread
		checkFxUserThread();

		notifyShutdownHooks();

		// terminate the application
		Application app = Application.GetApplication();
		app.terminate();
		
		// clear the fx user thread in the toolkit
		fxUserThread = null;
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
		throw new UnsupportedOperationException("implement me!");
	}

	@Override
	public void exitNestedEventLoop(Object key, Object rval) {
		throw new UnsupportedOperationException("implement me!");
	}

	@Override
	public boolean isNestedLoopRunning() {
		throw new UnsupportedOperationException("implement me!");
	}
	
	@Override
	public TKStage createTKPopupStage(Window peerWindow, StageStyle popupStyle, TKStage owner, AccessControlContext acc) {
		throw new UnsupportedOperationException("implement me!");
	}

	@Override
	public TKStage createTKEmbeddedStage(HostInterface host, AccessControlContext acc) {
		throw new UnsupportedOperationException("implement me!");
	}

	@Override
	public AppletWindow createAppletWindow(long parent, String serverName) {
		throw new UnsupportedOperationException("implement me!");
	}

	@Override
	public void closeAppletWindow() {
		throw new UnsupportedOperationException("implement me!");
	}

	@Override
	public void waitFor(Task t) {
		throw new UnsupportedOperationException("implement me!");
	}
	
	@Override
	public boolean isVsyncEnabled() {
		// TODO: implement vsync?
		return false;
	}
}
