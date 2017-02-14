package cuchaz.jfxgl.glass;

import java.util.Map;

import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.View;

import cuchaz.jfxgl.CalledByEventsThread;
import cuchaz.jfxgl.CalledByMainThread;
import cuchaz.jfxgl.Log;

public class JFXGLView extends View {

	private JFXGLWindow window;
	
	@Override
	@SuppressWarnings("rawtypes")
	protected long _create(Map capabilities) {
		
		window = null;
		
		// don't care about the screen handle
		long hscreen = 1l;
		return hscreen;
	}

	@Override
	protected void _enableInputMethodEvents(long hscreen, boolean enable) {
		// TEMP
		Log.log("JFXGLView._enableInputMethodEvents()");
	}

	@Override
	protected long _getNativeView(long hscreen) {
		return hscreen;
	}

	@Override
	protected int _getX(long hscreen) {
		return 0;
	}

	@Override
	protected int _getY(long hscreen) {
		return 0;
	}

	@Override
	@CalledByEventsThread
	protected void _setParent(long hscreen, long hwnd) {
		
		// TEMP
		Log.log("JFXGLView._setParent()   hwnd=%d", hwnd);
		
		this.window = (JFXGLWindow)super.getWindow();
	}

	@Override
	protected boolean _close(long hscreen) {
		return true;
	}

	@Override
	protected void _scheduleRepaint(long hscreen) {
		// TEMP
		Log.log("JFXGLView._scheduleRepaint()");
	}

	@Override
	@CalledByMainThread
	protected void _begin(long hscreen) {
		// forward to the window
		window.renderBegin();
	}

	@Override
	@CalledByMainThread
	protected void _end(long hscreen) {
		// forward to the window
		window.renderEnd();
	}

	@Override
	@CalledByMainThread
	protected int _getNativeFrameBuffer(long hscreen) {
		// forward to the window
		return window.getFBOId();
	}

	@Override
	protected void _uploadPixels(long hscreen, Pixels pixels) {
		// TEMP
		Log.log("JFXGLView._uploadPixels()");
	}

	@Override
	protected boolean _enterFullscreen(long hscreen, boolean animate, boolean keepRatio, boolean hideCursor) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void _exitFullscreen(long hscreen, boolean animate) {
		throw new UnsupportedOperationException();
	}
	
	// just override this method to make it public
	@Override
	public void notifyResize(int width, int height) {
		super.notifyResize(width, height);
	}
}
