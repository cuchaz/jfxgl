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

import cuchaz.jfxgl.Log;

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
			
			Log.log("events thread started");
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
		throw new UnsupportedOperationException();
	}

	@Override
	protected void _leaveNestedEventLoop(Object retValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Window createWindow(Window owner, Screen screen, int styleMask) {
		return new JFXGLWindow(owner, screen, styleMask);
	}

	@Override
	public Window createWindow(long parent) {
		throw new UnsupportedOperationException();
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
		return new JFXGLCursor(x, y, pixels);
	}

	@Override
	protected void staticCursor_setVisible(boolean visible) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected Size staticCursor_getBestSize(int width, int height) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Pixels createPixels(int width, int height, ByteBuffer data) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Pixels createPixels(int width, int height, IntBuffer data) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Pixels createPixels(int width, int height, IntBuffer data, float scale) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected int staticPixels_getNativeFormat() {
		return Pixels.Format.BYTE_ARGB;
	}

	@Override
	public Robot createRobot() {
		throw new UnsupportedOperationException();
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
		// TODO: do we really need this timer?
		Log.log("\n\ncreateTimer\n\n");
		return new JFXGLTimer(r);
	}

	@Override
	protected int staticTimer_getMinPeriod() {
		return JFXGLTimer.getMinPeriod_impl();
	}

	@Override
	protected int staticTimer_getMaxPeriod() {
		return JFXGLTimer.getMaxPeriod_impl();
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

	@Override
	protected long staticView_getMultiClickTime() {
		throw new UnsupportedOperationException();
	}

	@Override
	protected int staticView_getMultiClickMaxX() {
		throw new UnsupportedOperationException();
	}

	@Override
	protected int staticView_getMultiClickMaxY() {
		throw new UnsupportedOperationException();
	}

	@Override
	protected boolean _supportsTransparentWindows() {
		return true;
	}

	@Override
	protected boolean _supportsUnifiedWindows() {
		throw new UnsupportedOperationException();
	}

	@Override
	protected int _getKeyCodeForChar(char c) {
		throw new UnsupportedOperationException();
	}
}
