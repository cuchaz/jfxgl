package cuchaz.jfxgl.toolkit;

import com.sun.javafx.tk.quantum.PaintCollector;
import com.sun.javafx.tk.quantum.QuantumToolkit;
import com.sun.javafx.tk.quantum.ViewScene;

public class JFXGLPaintCollector extends PaintCollector {
	
	public JFXGLPaintCollector(QuantumToolkit qt) {
		super(qt);
		
		// install to the PaintCollector singleton
		PaintCollector.install(this);
	}
	
	@Override
	public void waitForRenderingToComplete() {
		// nope
	}
	
	@Override
	public void liveRepaintRenderJob(ViewScene scene) {
		throw new UnsupportedOperationException();
	}
}
