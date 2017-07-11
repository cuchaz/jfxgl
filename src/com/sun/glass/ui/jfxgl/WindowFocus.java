/*************************************************************************
 * Copyright (C) 2017, Jeffrey W. Martin "Cuchaz"
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License Version 2 with
 * the classpath exception, as published by the Free Software Foundation.
 * 
 * See LICENSE.txt in the project root folder for the full license.
 *************************************************************************/
package com.sun.glass.ui.jfxgl;

import com.sun.glass.events.WindowEvent;

public class WindowFocus {

	private JFXGLWindow mainWindow;
	private JFXGLWindow focusedWindow;
	
	public WindowFocus(JFXGLMainWindow mainWindow) {
		this.mainWindow = mainWindow;
		focusedWindow = mainWindow;
	}
	
	public JFXGLWindow getFocusedWindow() {
		return focusedWindow;
	}
	
	public void setFocusedWindow(JFXGLWindow val) {
		if (val == null) {
			val = mainWindow;
		}
		if (focusedWindow == val) {
			return;
		}
		focusedWindow.notifyFocus(WindowEvent.FOCUS_LOST);
		focusedWindow = val;
		focusedWindow.notifyFocus(WindowEvent.FOCUS_GAINED);
	}
}
