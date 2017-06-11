/*************************************************************************
 * Copyright (C) 2017, Jeffrey W. Martin "Cuchaz"
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License Version 2 with
 * the classpath exception, as published by the Free Software Foundation.
 * 
 * See LICENSE.txt in the project root folder for the full license.
 *************************************************************************/
package com.sun.glass.ui;

import java.nio.IntBuffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;

import cuchaz.jfxgl.CalledByMainThread;
import cuchaz.jfxgl.JFXGL;

public class JFXGLScreen {
	
	private static Screen[] screens = null;
	
	public static Screen[] getScreens() {
		
		if (screens != null) {
			return screens;
		}
		
		PointerBuffer monitorHandles = GLFW.glfwGetMonitors();
		screens = new Screen[monitorHandles.limit()];
		for (int i=0; i<monitorHandles.limit(); i++) {
			screens[i] = JFXGLScreen.make(monitorHandles.get(i));
		}
		return screens;
	}

	public static Screen make(long handle) {
		
		int x = 0;
		int y = 0;
		try (MemoryStack stack = MemoryStack.stackPush()) {
			IntBuffer xbuf = stack.callocInt(1);
			IntBuffer ybuf = stack.callocInt(1);
			GLFW.glfwGetMonitorPos(handle, xbuf, ybuf);
			x = xbuf.get(0);
			y = ybuf.get(0);
		}
		
		GLFWVidMode mode = GLFW.glfwGetVideoMode(handle);
		int width = mode.width();
		int height = mode.height();
		int depth = mode.redBits() + mode.greenBits() + mode.blueBits();
		
		// set the "visible" region to the whole screen for now
		int visibleX = x;
		int visibleY = y;
		int visibleWidth = width;
		int visibleHeight = height;
		
		int resolutionX = 90; // arbitrary?
		int resolutionY = resolutionX;
		float uiScale = 1f;
		float renderScale = 1f;
		
		return new Screen(
			handle,
			depth,
			x, y, width, height,
			visibleX, visibleY, visibleWidth, visibleHeight,
			resolutionX, resolutionY,
			uiScale, renderScale
		);
	}
	
	public static Screen copySetVisible(Screen screen, int x, int y, int w, int h) {
		return new Screen(
			screen.getNativeScreen(),
			screen.getDepth(),
			screen.getX(), screen.getY(), screen.getWidth(), screen.getHeight(),
			x, y, w, h,
			screen.getResolutionX(), screen.getResolutionY(),
			screen.getUIScale(), screen.getRenderScale()
		);
	}
	
	@CalledByMainThread
	public static void update(int wx, int wy, int ww, int wh) {
		
		// sadly, a consequence of compositing popup windows into the main framebuffer
		// is we can't draw outside of the main window
		// JavaFX's popup positioning logic assumes we can, so we have to tell it we can't
		// by artifically restricting the "visible" bounds of the screen containing the main window
		
		// NOTE: for intervals, upper bounds are exclusive
		
		int wl = wx;
		int wr = wx + ww;
		int wb = wy;
		int wt = wy + wh;
		
		// intersect the screen with the window
		Screen[] screens = getScreens();
		for (int i=0; i<screens.length; i++) {
			
			Screen screen = screens[i];
			int sx = screen.getX();
			int sy = screen.getY();
			int sw = screen.getWidth();
			int sh = screen.getHeight();
			
			int sl = sx;
			int sr = sx + sw;
			int sb = sy;
			int st = sy + sh;
			
			// get the AABB intersection
			if (sl > wr || sr < wl || sb > wt || st < wb) {
				continue;
			}
			int il = Math.max(wl, sl);
			int ir = Math.min(wr, sr);
			int ib = Math.max(wb, sb);
			int it = Math.min(wt, st);
			
			int iw = ir - il;
			int ih = it - ib;
			screens[i] = JFXGLScreen.copySetVisible(screen, il, ib, iw, ih);
		}
		
		// tell the rest of JavaFX to read the new screen settings
		JFXGL.runOnEventsThread(() -> {
			Screen.notifySettingsChanged();
		});
	}
}
