/*************************************************************************
 * Copyright (C) 2017, Jeffrey W. Martin "Cuchaz"
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License Version 2 with
 * the classpath exception, as published by the Free Software Foundation.
 * 
 * See LICENSE.txt in the project root folder for the full license.
 *************************************************************************/
package com.sun.prism.es2;

import java.util.HashMap;

import com.sun.prism.es2.GLContext;
import com.sun.prism.es2.GLDrawable;
import com.sun.prism.es2.GLFactory;
import com.sun.prism.es2.GLGPUInfo;
import com.sun.prism.es2.GLPixelFormat;

public class JFXGLFactory extends GLFactory {
	
	private static JFXGLDrawable drawable;
	
	static {
		drawable = null;
	}
	
	public static void install() {
		
		drawable = new JFXGLDrawable(JFXGLContexts.app.hwnd);
		GLFactory.platformFactory = new JFXGLFactory();
		
		if (ES2Pipeline.glFactory == null) {
			throw new Error(JFXGLFactory.class.getSimpleName() + " not created");
		} else if (ES2Pipeline.glFactory.getClass() != JFXGLFactory.class) {
			throw new Error(JFXGLFactory.class.getSimpleName() + " not created, got a " + ES2Pipeline.glFactory.getClass().getName() + " instead.");
		}
	}
	
	@Override
	public GLGPUInfo[] getPreQualificationFilter() {
		return null;
	}

	@Override
	public GLGPUInfo[] getBlackList() {
		return null;
	}
	
	// NOTE: prism makes a lot of dummy contexts with bogus handles,
	// so we can't trust any of them to be useful until someone calls makeCurrent() with a valid hwnd

	@Override
	public JFXGLContext createGLContext(long hwnd) {
		return JFXGLContexts.javafx;
	}

	@Override
	public JFXGLContext createGLContext(GLDrawable drawable, GLPixelFormat pixelFormat, GLContext shareCtx, boolean vSyncRequest) {
		return JFXGLContexts.javafx;
	}

	@Override
	public JFXGLDrawable createGLDrawable(long hwnd, GLPixelFormat pixelFormat) {
		return drawable;
	}

	@Override
	public JFXGLDrawable createDummyGLDrawable(GLPixelFormat pixelFormat) {
		return drawable;
	}

	@Override
	public GLPixelFormat createGLPixelFormat(long nativeScreen, GLPixelFormat.Attributes attrs) {
		// don't need to wrap pixel format, only consumed by Drawables
		return null;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public boolean initialize(Class psClass, GLPixelFormat.Attributes attrs) {
		
		nativeCtxInfo = JFXGLContexts.javafx.hwnd;
		
		// NOTE: everyone just always sets this to true
		gl2 = true;
		
		// this mode is always supported, since we're basically ignoring the
		// mode specified by attrs and using whatever GLFW did already
		return true;
	}

	@Override
	public int getAdapterCount() {
		// TODO: get this from GLFW?
		return 1;
	}

	@Override
	public int getAdapterOrdinal(long nativeScreen) {
		// TODO: get this from GLFW?
		return 0;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public void updateDeviceDetails(HashMap deviceDetails) {
		// don't need to do anything here for now
		// unless we find a reason we need to
	}
	
	@Override
	public boolean isGLExtensionSupported(String sglExtStr) {
		return JFXGLContexts.javafx.isExtensionSupported(sglExtStr);
	}
	
	@Override
	public boolean isQualified(long nativeCtxInfo) {
		// sure, why not
		return true;
	}
	
	@Override
	public void printDriverInformation(int adapter) {
		// or not, who cares...
    }
}
