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
