/*************************************************************************
 * Copyright (C) 2017, Jeffrey W. Martin "Cuchaz"
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License Version 2 with
 * the classpath exception, as published by the Free Software Foundation.
 * 
 * See LICENSE.txt in the project root folder for the full license.
 *************************************************************************/
package cuchaz.jfxgl.glass;

import java.nio.IntBuffer;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;

import com.sun.glass.ui.Screen;

public class JFXGLScreen {

	public static Screen make(long handle) {
		
		int x = 0;
		int y = 0;
		try (MemoryStack stack = MemoryStack.stackPush()) {
			IntBuffer xbuf = stack.callocInt(1);
			IntBuffer ybuf = stack.callocInt(1);
			GLFW.glfwGetMonitorPos(handle, xbuf, ybuf);
			x = xbuf.get(0);
			y = xbuf.get(0);
		}
		
		GLFWVidMode mode = GLFW.glfwGetVideoMode(handle);
		int width = mode.width();
		int height = mode.height();
		int depth = mode.redBits() + mode.greenBits() + mode.blueBits();
		int resolutionX = 90; // arbitrary?
		int resolutionY = resolutionX;
		
		int visibleX = x;
		int visibleY = x;
		int visibleWidth = width;
		int visibleHeight = height;
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
}
