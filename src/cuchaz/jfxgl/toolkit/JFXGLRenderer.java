package cuchaz.jfxgl.toolkit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import com.sun.javafx.tk.quantum.QuantumRenderer;

public class JFXGLRenderer extends QuantumRenderer {
	
	private List<Runnable> jobQueue;
	private List<Runnable> jobs;

	public JFXGLRenderer() {
		super();
		
		jobQueue = new ArrayList<>();
		jobs = new ArrayList<>();
		
		// install to the QuantumRenderer singleton
		QuantumRenderer.install(this);
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
		
		// run all the render jobs
		for (Runnable job : jobs) {
			job.run();
		}
		jobs.clear();
	}
}
