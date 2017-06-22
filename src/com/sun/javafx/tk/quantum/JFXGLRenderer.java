/*************************************************************************
 * Copyright (C) 2017, Jeffrey W. Martin "Cuchaz"
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License Version 2 with
 * the classpath exception, as published by the Free Software Foundation.
 * 
 * See LICENSE.txt in the project root folder for the full license.
 *************************************************************************/
package com.sun.javafx.tk.quantum;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.lwjgl.opengl.GL11;

import com.sun.glass.ui.jfxgl.JFXGLMainWindow;
import com.sun.glass.ui.jfxgl.JFXGLPopupWindow;
import com.sun.javafx.tk.RenderJob;
import com.sun.prism.es2.JFXGLContexts;

public class JFXGLRenderer extends QuantumRenderer {
	
	private List<Runnable> jobQueue;
	private List<Runnable> jobs;
	private List<JFXGLPopupWindow> popups;

	public JFXGLRenderer() {
		super();
		
		jobQueue = new ArrayList<>();
		jobs = new ArrayList<>();
		popups = new ArrayList<>();
		
		// install to the QuantumRenderer singleton
		QuantumRenderer.instanceReference.set(this);
	}
	
	@Override
	public Future<?> submit(Runnable job) {
		
		// relay runnable job to main thread
		synchronized (jobQueue) {
			jobQueue.add(job);
		}
		
		// no one uses the Future instance
		return null;
	}
	
	@Override
	@SuppressWarnings("rawtypes")
	public Future submitRenderJob(RenderJob r) {
		return super.submitRenderJob(r);
	}
	
	@Override
	public void checkRendererIdle() {
		// do nothing
	}
	
	public void render() {
		
		// grab all the jobs from the queue
		jobs.clear();
		synchronized (jobQueue) {
			jobs.addAll(jobQueue);
			jobQueue.clear();
		}
		
		if (!jobs.isEmpty()) {
			
			// switch to JavaFX context for JavaFX rendering
			JFXGLContexts.javafx.makeCurrent();
		
			// run all the render jobs
			for (Runnable job : jobs) {
				job.run();
			}
			jobs.clear();
			
			// explicitly flush all rendering so we can sync between contexts
			// (OSX driver doesn't seem to be smart enough to do this automatically)
			GL11.glFinish();
			
			// switch back to app context for non-JavaFX rendering
			JFXGLContexts.app.makeCurrent();
		}
		
		// copy the javafx framebuffer to the main framebuffer
		if (JFXGLMainWindow.instance != null) {
			JFXGLMainWindow.instance.renderFramebuf();
		}
		
		// render any popup windows
		// NOTE: JFXGLPopupWindow.windows is a synchronized list, so copy it to local storage before rendering
		popups.clear();
		popups.addAll(JFXGLPopupWindow.windows);
		for (JFXGLPopupWindow popup : popups) {
			popup.renderFramebuf(JFXGLMainWindow.instance.getWidth(), JFXGLMainWindow.instance.getHeight());
		}
		popups.clear();
	}
}
