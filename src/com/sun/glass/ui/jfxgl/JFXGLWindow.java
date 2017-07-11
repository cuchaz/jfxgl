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
import com.sun.glass.ui.Cursor;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;
import com.sun.prism.es2.OffscreenBuffer;

import cuchaz.jfxgl.CalledByEventsThread;
import cuchaz.jfxgl.CalledByMainThread;
import cuchaz.jfxgl.InJavaFXGLContext;

public abstract class JFXGLWindow extends Window {
	
	protected static enum Implementation {
		Ignore,
		Throw;
	}
	
	public final JFXGLWindow owner;
	private final Implementation impl;
	private JFXGLView view;

	protected JFXGLWindow(Window owner, Screen screen, int styleMask, Implementation impl) {
		super(owner, screen, styleMask);
		this.owner = (JFXGLWindow)owner;
		this.impl = impl;
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
	
	public int getNumOwners() {
		int count = 0;
		JFXGLWindow owner = this.owner;
		while (owner != null) {
			count++;
			owner = owner.owner;
		}
		return count;
	}
	
	@CalledByMainThread
	@InJavaFXGLContext
	public abstract void renderBegin();
	
	@CalledByMainThread
	@InJavaFXGLContext
	public abstract void renderEnd();
	
	@CalledByMainThread
	public abstract int getFBOId();
	
	public abstract OffscreenBuffer getBuffer();

	@Override
    public void notifyFocus(int event) {
		super.notifyFocus(event);
	}
	
	@Override
	protected boolean _requestFocus(long hwnd, int event) {
		if (event == WindowEvent.FOCUS_GAINED) {
			JFXGLMainWindow.instance.focus.setFocusedWindow(this);
		}
		return true;
	}
	
	@Override
	protected long _createChildWindow(long parent) {
		if (impl == Implementation.Throw) throw new UnsupportedOperationException();
		return 0;
	}
	
	@Override
	protected boolean _setMenubar(long hwnd, long menubarhwnd) {
		if (impl == Implementation.Throw) throw new UnsupportedOperationException();
		return true;
	}
	
	@Override
	protected int _getEmbeddedX(long hwnd) {
		if (impl == Implementation.Throw) throw new UnsupportedOperationException();
		return 0;
	}

	@Override
	protected int _getEmbeddedY(long hwnd) {
		if (impl == Implementation.Throw) throw new UnsupportedOperationException();
		return 0;
	}
	
	@Override
	protected void _requestInput(long hwnd, String text, int type, double width, double height, double Mxx, double Mxy,
			double Mxz, double Mxt, double Myx, double Myy, double Myz, double Myt, double Mzx, double Mzy, double Mzz,
			double Mzt) {
		if (impl == Implementation.Throw) throw new UnsupportedOperationException();
	}

	@Override
	protected void _releaseInput(long hwnd) {
		if (impl == Implementation.Throw) throw new UnsupportedOperationException();
	}
	
	@Override
	protected boolean _minimize(long hwnd, boolean minimize) {
		if (impl == Implementation.Throw) throw new UnsupportedOperationException();
		return false;
	}

	@Override
	protected boolean _maximize(long hwnd, boolean maximize, boolean wasMaximized) {
		if (impl == Implementation.Throw) throw new UnsupportedOperationException();
		return false;
	}

	@Override
	protected void _setBounds(long hwnd, int x, int y, boolean xSet, boolean ySet, int w, int h, int cw, int ch, float xGravity, float yGravity) {
		if (impl == Implementation.Throw) throw new UnsupportedOperationException();
	}

	protected boolean _setVisible(long hwnd, boolean visible) {
		if (impl == Implementation.Throw) throw new UnsupportedOperationException();
		return visible;
	}

	@Override
	protected boolean _setResizable(long hwnd, boolean resizable) {
		if (impl == Implementation.Throw) throw new UnsupportedOperationException();
		return false;
	}
	
	@Override
	protected void _setFocusable(long hwnd, boolean isFocusable) {
		if (impl == Implementation.Throw) throw new UnsupportedOperationException();
	}

	@Override
	protected boolean _grabFocus(long hwnd) {
		if (impl == Implementation.Throw) throw new UnsupportedOperationException();
		return true;
	}

	@Override
	protected void _ungrabFocus(long hwnd) {
		if (impl == Implementation.Throw) throw new UnsupportedOperationException();
	}

	@Override
	protected boolean _setTitle(long hwnd, String title) {
		if (impl == Implementation.Throw) throw new UnsupportedOperationException();
		return false;
	}

	@Override
	protected void _setLevel(long hwnd, int level) {
		if (impl == Implementation.Throw) throw new UnsupportedOperationException();
	}

	@Override
	protected void _setAlpha(long hwnd, float alpha) {
		if (impl == Implementation.Throw) throw new UnsupportedOperationException();
	}

	@Override
	protected boolean _setBackground(long hwnd, float r, float g, float b) {
		if (impl == Implementation.Throw) throw new UnsupportedOperationException();
		return false;
	}

	@Override
	protected void _setEnabled(long hwnd, boolean enabled) {
		if (impl == Implementation.Throw) throw new UnsupportedOperationException();
	}

	@Override
	protected boolean _setMinimumSize(long hwnd, int width, int height) {
		if (impl == Implementation.Throw) throw new UnsupportedOperationException();
		return false;
	}

	@Override
	protected boolean _setMaximumSize(long hwnd, int width, int height) {
		if (impl == Implementation.Throw) throw new UnsupportedOperationException();
		return false;
	}

	@Override
	protected void _setIcon(long hwnd, Pixels pixels) {
		if (impl == Implementation.Throw) throw new UnsupportedOperationException();
	}

	@Override
	protected void _setCursor(long hwnd, Cursor cursor) {
		if (impl == Implementation.Throw) throw new UnsupportedOperationException();
	}

	@Override
	protected void _toFront(long hwnd) {
		if (impl == Implementation.Throw) throw new UnsupportedOperationException();
	}

	@Override
	protected void _toBack(long hwnd) {
		if (impl == Implementation.Throw) throw new UnsupportedOperationException();
	}

	@Override
	protected void _enterModal(long hwnd) {
		if (impl == Implementation.Throw) throw new UnsupportedOperationException();
	}

	@Override
	protected void _enterModalWithWindow(long dialog, long window) {
		if (impl == Implementation.Throw) throw new UnsupportedOperationException();
	}

	@Override
	protected void _exitModal(long hwnd) {
		if (impl == Implementation.Throw) throw new UnsupportedOperationException();
	}
}
