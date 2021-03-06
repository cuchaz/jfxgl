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

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.nfd.NFDPathSet;
import org.lwjgl.util.nfd.NativeFileDialog;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.CommonDialogs.ExtensionFilter;
import com.sun.glass.ui.CommonDialogs.FileChooserResult;
import com.sun.glass.ui.CommonDialogs.Type;
import com.sun.glass.ui.Cursor;
import com.sun.glass.ui.JFXGLScreen;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.Robot;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.Size;
import com.sun.glass.ui.Timer;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;

import cuchaz.jfxgl.EventsThreadNotRunningException;

public class JFXGLApplication extends Application {

	private EventQueue events;
	
	private volatile boolean isRunning;
	
	public JFXGLApplication() {
		events = new EventQueue();
		isRunning = false;
	}
	
	@Override
    public void terminate() {
		super.terminate();
		
		// stop the event thread
		events.shutdown();
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
			try {
				events.getNextEvent().run();
			} catch (Throwable t) {
				reportException(t);
			}
		}
	}
	
	@Override
	protected void _invokeAndWait(Runnable runnable) {
		checkEventThreadRunning();
		final CountDownLatch latch = new CountDownLatch(1);
        events.postEvent(() -> {
            try {
                runnable.run();
            } finally {
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException ex) {
        	// don't care
        }
	}

	@Override
	protected void _invokeLater(Runnable runnable) {
		checkEventThreadRunning();
		events.postEvent(runnable);
	}
	
	private void checkEventThreadRunning() {
		if (!isRunning) {
			throw new EventsThreadNotRunningException();
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
		if (JFXGLMainWindow.instance == null) {
			return new JFXGLMainWindow(owner, screen, styleMask);
		} else {
			return new JFXGLPopupWindow(owner, screen, styleMask);
		}
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
		return JFXGLScreen.getScreens();
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
	public FileChooserResult staticCommonDialogs_showFileChooser(Window owner, String folder, String filename,
			String title, int type, boolean multipleMode, ExtensionFilter[] extensionFilters, int defaultFilterIndex) {
		
		// sadly, JavaFX's file dialog implementation doesn't seem to work here =(
		// at least in GTK, the dialogs don't clean up after we're done using them
		// so try NFD instead
		
		// implement extension filters, convert to e.g., "png,jpg;pdf"
		StringBuilder filterBuf = new StringBuilder();
		for (ExtensionFilter ext : extensionFilters) {
			if (filterBuf.length() > 0) {
				filterBuf.append(';');
			}
			for (int i=0; i<ext.getExtensions().size(); i++) {
				if (i > 0) {
					filterBuf.append(',');
				}
				filterBuf.append(ext.getExtensions().get(i));
			}
		}
		String filter = filterBuf.toString();
		
		if (multipleMode) {
			return openFileDialogMultiple(filter, folder);
		} else {
			if (type == Type.OPEN) {
				return openFileDialog(filter, folder);
			} else if (type == Type.SAVE) {
				return openSaveDialog(filter, folder);
			} else {
				throw new IllegalArgumentException("unknown file dialog type: " + type);
			}
		}
	}
	
	private FileChooserResult openFileDialog(String filter, String folder) {
		PointerBuffer pathBuf = MemoryUtil.memAllocPointer(1);
		try {
			int result = NativeFileDialog.NFD_OpenDialog(filter, folder, pathBuf);
			switch (result) {
				case NativeFileDialog.NFD_OKAY:
					String path = pathBuf.getStringUTF8(0);
					NativeFileDialog.nNFDi_Free(pathBuf.get(0));
					return new FileChooserResult(Arrays.asList(new File(path)), null);
				case NativeFileDialog.NFD_CANCEL:
					return new FileChooserResult();
				default:
					throw new RuntimeException("NFD error: " + NativeFileDialog.NFD_GetError());
			}
		} finally {
			MemoryUtil.memFree(pathBuf);
		}
	}
	
	private FileChooserResult openSaveDialog(String filter, String folder) {
		PointerBuffer pathBuf = MemoryUtil.memAllocPointer(1);
		try {
			int result = NativeFileDialog.NFD_SaveDialog(filter, folder, pathBuf);
			switch (result) {
				case NativeFileDialog.NFD_OKAY:
					String path = pathBuf.getStringUTF8(0);
					NativeFileDialog.nNFDi_Free(pathBuf.get(0));
					return new FileChooserResult(Arrays.asList(new File(path)), null);
				case NativeFileDialog.NFD_CANCEL:
					return new FileChooserResult();
				default:
					throw new RuntimeException("NFD error: " + NativeFileDialog.NFD_GetError());
			}
		} finally {
			MemoryUtil.memFree(pathBuf);
		}
	}

	private FileChooserResult openFileDialogMultiple(String filter, String folder) {
		try (NFDPathSet pathSet = NFDPathSet.calloc()) {
			int result = NativeFileDialog.NFD_OpenDialogMultiple(filter, folder, pathSet);
			switch (result) {
				case NativeFileDialog.NFD_OKAY:
					List<File> files = new ArrayList<>();
					long count = NativeFileDialog.NFD_PathSet_GetCount(pathSet);
					for (long i=0; i<count; i++) {
						files.add(new File(NativeFileDialog.NFD_PathSet_GetPath(pathSet, i)));
					}
					NativeFileDialog.NFD_PathSet_Free(pathSet);
					return new FileChooserResult(files, null);
				case NativeFileDialog.NFD_CANCEL:
					return new FileChooserResult();
				default:
					throw new RuntimeException("NFD error: " + NativeFileDialog.NFD_GetError());
			}
		}
	}

	@Override
	public File staticCommonDialogs_showFolderChooser(Window owner, String folder, String title) {
		PointerBuffer pathBuf = MemoryUtil.memAllocPointer(1);
		try {
			
			int result = NativeFileDialog.NFD_PickFolder(folder, pathBuf);
			switch (result) {
				case NativeFileDialog.NFD_OKAY:
					String path = pathBuf.getStringUTF8(0);
					NativeFileDialog.nNFDi_Free(pathBuf.get(0));
					return new File(path);
				case NativeFileDialog.NFD_CANCEL:
					return null;
				default:
					throw new RuntimeException("NFD error: " + NativeFileDialog.NFD_GetError());
			}
		} finally {
			MemoryUtil.memFree(pathBuf);
		}
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
