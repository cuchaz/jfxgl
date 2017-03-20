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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import com.sun.javafx.tk.RenderJob;
import com.sun.javafx.tk.quantum.QuantumRenderer;

import cuchaz.jfxgl.glass.JFXGLWindow;
import cuchaz.jfxgl.prism.JFXGLContexts;

public class JFXGLRenderer extends QuantumRenderer {
	
	private List<Runnable> jobQueue;
	private List<Runnable> jobs;

	public JFXGLRenderer() {
		super();
		
		jobQueue = new ArrayList<>();
		jobs = new ArrayList<>();
		
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
			
			// switch back to app context for non-JavaFX rendering
			JFXGLContexts.app.makeCurrent();
		}
		
		// copy the javafx framebuffer to the main framebuffer
		if (JFXGLWindow.mainWindow != null) {
			JFXGLWindow.mainWindow.renderFramebuf();
		}
	}
}
