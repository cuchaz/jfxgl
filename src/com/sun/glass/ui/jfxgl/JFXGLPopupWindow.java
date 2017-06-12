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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lwjgl.opengl.GL11;

import com.sun.glass.events.WindowEvent;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;
import com.sun.prism.es2.JFXGLContext;
import com.sun.prism.es2.JFXGLContexts;
import com.sun.prism.es2.OffscreenBuffer;

import cuchaz.jfxgl.CalledByEventsThread;
import cuchaz.jfxgl.CalledByMainThread;
import cuchaz.jfxgl.GLState;
import cuchaz.jfxgl.InAppGLContext;
import cuchaz.jfxgl.InJavaFXGLContext;

public class JFXGLPopupWindow extends JFXGLWindow {
	
	public static List<JFXGLPopupWindow> windows = Collections.synchronizedList(new ArrayList<>());
	
	private JFXGLContext context = null;
	private JFXGLView view = null;
	
	private int renderY = 0;
	private int renderX = 0;
	private int width = 0;
	private int height = 0;
	private OffscreenBuffer buf = null;
	private GLState glstate = new GLState(
		GLState.Blend, GLState.BlendFunc, GLState.ShaderProgram,
		GLState.ActiveTexture, GLState.Texture2D[0],
		GLState.VertexArray, GLState.ArrayBuffer,
		GLState.Viewport
	);
	
	protected JFXGLPopupWindow(Window owner, Screen screen, int styleMask) {
		super(owner, screen, styleMask, Implementation.Ignore);
		
		// get our context
		context = JFXGLContexts.app;
		
		windows.add(this);
	}
	
	@Override
	@CalledByEventsThread
	protected long _createWindow(long ownerhwnd, long screenhwnd, int mask) {
		// use the main app hwnd
		return JFXGLContexts.app.hwnd;
	}
	
	@Override
	@CalledByEventsThread
	protected boolean _setView(long hwnd, View view) {
		this.view = (JFXGLView)view;
		return true;
	}
	
	@CalledByMainThread
	public JFXGLView getRenderView() {
		return view;
	}

	@Override
	@CalledByEventsThread
	protected boolean _close(long hwnd) {
		
		windows.remove(this);
		
		notifyDestroy();
		return false; // return value apparently ignored
	}

	@Override
	protected void _setBounds(long hwnd, int x, int y, boolean xSet, boolean ySet, int w, int h, int cw, int ch, float xGravity, float yGravity) {
		
		if (xSet) {
			this.renderY = x - JFXGLMainWindow.instance.getX();
		}
		if (ySet) {
			this.renderX = y - JFXGLMainWindow.instance.getY();
		}
		
		this.width = w;
		this.height = h;
		
		// tell the window and view to resize
		notifyResize(WindowEvent.RESIZE, width, height);
		if (view != null) {
			view.notifyResize(this.width, this.height);
		}
	}
	
	public int getRenderX() {
		return renderY;
	}
	
	public int getRenderY() {
		return renderX;
	}
	
	public int getRenderWidth() {
		return width;
	}
	
	public int getRenderHeight() {
		return height;
	}
	
	@Override
	@CalledByMainThread
	@InJavaFXGLContext
	public void renderBegin() {
		
		// do we even have a size?
		if (width <= 0 || height <= 0) {
			return;
		}
		
		// do we need to resize the framebuffer?
		if (buf == null) {
			buf = new OffscreenBuffer(context, width, height);
		} else {
			buf.resize(width, height);
		}
	}
	
	@Override
	@CalledByMainThread
	@InJavaFXGLContext
	public void renderEnd() {
	
		// nothing to do
	}
	
	@Override
	@CalledByMainThread
	public int getFBOId() {
		if (buf != null) {
			return buf.getFboId();
		}
		return 0;
	}
	
	@CalledByMainThread
	@InAppGLContext
	public void renderFramebuf(int windowWidth, int windowHeight) {
		if (buf != null) {
			
			glstate.backup();
			
			// compute pos
			int x = renderY;
			int y = windowHeight - height - renderX;
			
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			GL11.glViewport(x, y, width, height);
			
			// composite our framebuffer onto the main framebuffer
			buf.render(0, 0, width, height, false);
			
			glstate.restore();
		}
	}
	
	@Override
	public OffscreenBuffer getBuffer() {
		return buf;
	}
}
