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

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.CommonDialogs.ExtensionFilter;
import com.sun.glass.ui.CommonDialogs.FileChooserResult;
import com.sun.glass.ui.Cursor;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.Robot;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.Size;
import com.sun.glass.ui.Timer;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;

public class JFXGLApplication extends Application {

	private static abstract class Event {
		abstract void dispatch();
	}

	private static class RunnableEvent extends Event {
		private boolean wait;
		private Runnable runnable;

		RunnableEvent(boolean wait, Runnable runnable) {
			this.wait = wait;
			this.runnable = runnable;
		}

		@Override
		void dispatch() {
			runnable.run();
			if (wait) {
				synchronized (invokeAndWaitLock) {
					waitingFor = null;
					invokeAndWaitLock.notify();
				}
			}
		}

		@Override
		public String toString() {
			return "RunnableEvent[runnable=" + runnable + ",wait=" + wait + "]";
		}
	}
	
	private static final Object invokeAndWaitLock = new Object();
	private static Runnable waitingFor;

	private LinkedList<Event> eventList;
	private boolean isRunning;
	
	public JFXGLApplication() {
		eventList = new LinkedList<Event>();
		isRunning = false;
	}
	
	@Override
    public void terminate() {
		super.terminate();
		
		// stop the event thread
		isRunning = false;
	}
	
	@Override
	protected void runLoop(Runnable r) {
		
		// start the events thread
		Thread toolkitThread = new Thread(() -> {
			r.run();
			eventLoop();
			
		}, "FXEvents");
		setEventThread(toolkitThread);
		toolkitThread.start();
	}
	
	private void eventLoop() {
		
		isRunning = true;
		while (isRunning) {
			
			// process events
			Event event = null;
			synchronized (eventList) {
				event = eventList.poll();
			}
			if (event != null) {
				try {
					event.dispatch();
				} catch (Exception ex) {
					reportException(ex);
				}
			}
		}
	}
	
	@Override
	protected void _invokeAndWait(Runnable runnable) {
		synchronized (invokeAndWaitLock) {
			waitingFor = runnable;
		}
		synchronized (eventList) {
			eventList.addLast(new RunnableEvent(true, runnable));
			eventList.notify();
		}
		synchronized (invokeAndWaitLock) {
			while (waitingFor == runnable) {
				try {
					invokeAndWaitLock.wait();
				} catch (InterruptedException ex) {
				}
			}
		}
	}

	@Override
	protected void _invokeLater(Runnable runnable) {
		synchronized (eventList) {
			eventList.addLast(new RunnableEvent(false, runnable));
			eventList.notify();
		}
	}

	@Override
	protected Object _enterNestedEventLoop() {
		throw new UnsupportedOperationException("implement me!");
	}

	@Override
	protected void _leaveNestedEventLoop(Object retValue) {
		throw new UnsupportedOperationException("implement me!");
	}

	@Override
	public Window createWindow(Window owner, Screen screen, int styleMask) {
		return new JFXGLWindow(owner, screen, styleMask);
	}

	@Override
	public Window createWindow(long parent) {
		throw new UnsupportedOperationException("implement me!");
	}

	@Override
	public View createView() {
		return new JFXGLView();
	}

	@Override
	public Cursor createCursor(int type) {
		return new JFXGLCursor(type);
	}

	@Override
	public Cursor createCursor(int x, int y, Pixels pixels) {
		throw new UnsupportedOperationException("implement me!");
	}

	@Override
	protected void staticCursor_setVisible(boolean visible) {
		throw new UnsupportedOperationException("implement me!");
	}

	@Override
	protected Size staticCursor_getBestSize(int width, int height) {
		throw new UnsupportedOperationException("implement me!");
	}

	@Override
	public Pixels createPixels(int width, int height, ByteBuffer data) {
		throw new UnsupportedOperationException("implement me!");
	}

	@Override
	public Pixels createPixels(int width, int height, IntBuffer data) {
		throw new UnsupportedOperationException("implement me!");
	}

	@Override
	public Pixels createPixels(int width, int height, IntBuffer data, float scale) {
		throw new UnsupportedOperationException("implement me!");
	}

	@Override
	protected int staticPixels_getNativeFormat() {
		return Pixels.Format.BYTE_ARGB;
	}

	@Override
	public Robot createRobot() {
		throw new UnsupportedOperationException("implement me!");
	}

	@Override
	protected double staticScreen_getVideoRefreshPeriod() {
		
		// indicate millisecond resolution
		return 0.0;
	}

	@Override
	protected Screen[] staticScreen_getScreens() {
		PointerBuffer monitorHandles = GLFW.glfwGetMonitors();
		Screen[] screens = new Screen[monitorHandles.limit()];
		for (int i=0; i<monitorHandles.limit(); i++) {
			screens[i] = JFXGLScreen.make(monitorHandles.get(i));
		}
		return screens;
	}

	@Override
	public Timer createTimer(Runnable r) {
		throw new UnsupportedOperationException("implement me!");
	}

	@Override
	protected int staticTimer_getMinPeriod() {
		throw new UnsupportedOperationException("implement me!");
	}

	@Override
	protected int staticTimer_getMaxPeriod() {
		throw new UnsupportedOperationException("implement me!");
	}

	@Override
	protected FileChooserResult staticCommonDialogs_showFileChooser(Window owner, String folder, String filename,
			String title, int type, boolean multipleMode, ExtensionFilter[] extensionFilters, int defaultFilterIndex) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected File staticCommonDialogs_showFolderChooser(Window owner, String folder, String title) {
		throw new UnsupportedOperationException();
	}

	// these are pretty arbitrary, I guess
	private static long multiClickTime = 500;
	private static int multiClickMaxX = 20;
	private static int multiClickMaxY = 20;
	
	@Override
	protected long staticView_getMultiClickTime() {
		return multiClickTime;
	}

	@Override
	protected int staticView_getMultiClickMaxX() {
		return multiClickMaxX;
	}

	@Override
	protected int staticView_getMultiClickMaxY() {
		return multiClickMaxY;
	}

	@Override
	protected boolean _supportsTransparentWindows() {
		return true;
	}

	@Override
	protected boolean _supportsUnifiedWindows() {
		throw new UnsupportedOperationException("implement me!");
	}

	@Override
	protected int _getKeyCodeForChar(char c) {
		throw new UnsupportedOperationException("implement me!");
	}
}
