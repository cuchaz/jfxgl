package cuchaz.jfxgl.toolkit;

import com.sun.javafx.tk.quantum.PaintCollector;

public class JFXGLPaintCollector extends PaintCollector {

	protected JFXGLPaintCollector(JFXGLToolkit tk) {
		super(tk);
		collector = this;
	}
	
	@Override
	protected void waitForRenderingToComplete() {
		// nope
	}
	
	// override these for public access
	
	@Override
	public boolean hasDirty() {
		return super.hasDirty();
	}
	
	@Override
	public void renderAll() {
		super.renderAll();
	}
}
