package cuchaz.jfxgl.glass;

import java.nio.IntBuffer;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;

import com.sun.glass.ui.Cursor;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;
import com.sun.javafx.application.PlatformImpl;
import com.sun.prism.es2.JFXGLContext;
import com.sun.prism.es2.JFXGLFactory;

import cuchaz.jfxgl.CalledByEventsThread;
import cuchaz.jfxgl.CalledByMainThread;
import cuchaz.jfxgl.Log;

public class JFXGLWindow extends Window {
	
	private static int numWindows = 0;
	
	private long hwnd;
	private JFXGLContext context;
	
	private JFXGLView view;
	
	private int width;
	private int height;
	private boolean fboDirty;
	private int texId;
	private int fboId;
	
	
	protected JFXGLWindow(Window owner, Screen screen, int styleMask) {
		super(owner, screen, styleMask);
		
		hwnd = 0;
		context = null;
		
		view = null;
		
		width = 0;
		height = 0;
		fboDirty = true;
		texId = 0;
		fboId = 0;
	}
	
	@Override
	@CalledByEventsThread
	protected long _createWindow(long ownerhwnd, long screenhwnd, int mask) {
		
		// TEMP
		Log.log("JFXGLWindow._createWindow()   ownerhwnd=%d   screenhwnd=%d", ownerhwnd, screenhwnd);
		
		// only ever create one window
		if (numWindows >= 1) {
			throw new IllegalStateException("can't create more than one window");
		}
		numWindows++;
		
		// and don't actually create it either
		// use the one that was already created by the main thread
		hwnd = JFXGLFactory.getHwnd();
		
		GLFW.glfwSetWindowSizeCallback(hwnd, (long hwndAgain, int width, int height) -> {
			
			// save our own copy of the size for framebuffer sizing
			if (width != this.width || height != this.height) {
				this.width = width;
				this.height = height;
				this.fboDirty = true;
			}
			
			// NOTE: GLFW events called on main thread, so relay to events thread
			PlatformImpl.runLater(() -> {
				if (view != null) {
					view.notifyResize(this.width, this.height);
				}
			});
		});
		
		return hwnd;
	}

	@Override
	protected long _createChildWindow(long parent) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected boolean _close(long hwnd) {
		
		// NOTE: don't call notifyClose() here
		
		// and don't actually destroy the GLFW window either
		// the main thread will take care of that
		notifyDestroy();
		
		return false; // return value apparently ignored
	}

	@Override
	@CalledByEventsThread
	protected boolean _setView(long hwnd, View view) {
		
		this.hwnd = hwnd;
		this.view = (JFXGLView)view;
		
		// TEMP
		Log.log("JFXGLWindow._setView() hwnd=" + hwnd + ", hview=" + (view == null ? null : view.getNativeView()));
		
		if (view != null) {
			
			// tell JavaFX about the current window size
			try (MemoryStack m = MemoryStack.stackPush()) {
				IntBuffer widthBuf = m.callocInt(1);
				IntBuffer heightBuf = m.callocInt(1);
				GLFW.glfwGetWindowSize(hwnd, widthBuf, heightBuf);
				width = widthBuf.get(0);
				height = heightBuf.get(0);
			}
			this.view.notifyResize(width, height);
		}
		
		return true;
	}
	
	@CalledByMainThread
	public void renderBegin() {
		
		// TEMP
		Log.log("JFXGLWindow.renderBegin()");
		
		/* TODO: get separate framebuffer working
		if (context == null) {
			context = new JFXGLContext(hwnd);
		}
		
		// do we need to make a new framebuffer?
		if (fboDirty) {
			fboDirty = false;
			
			if (texId != 0) {
				context.deleteTexture(texId);
			}
			if (fboId != 0) {
				context.deleteFBO(fboId);
			}
			
			// TEMP
			Log.log("\tcreate FBO %dx%d", width, height);
			
			texId = context.createTexture(width, height);
			fboId = context.createFBO(texId);
		}
		*/
	}
	
	@CalledByMainThread
	public void renderEnd() {
		
		// TEMP
		Log.log("JFXGLWindow.renderEnd()");
	}
	
	@CalledByMainThread
	public int getFBOId() {
		
		// NOTE: renderXXX methods called on the main thread
		
		// TEMP
		Log.log("JFXGLWindow.getFBOId()   fbo=%d", fboId);
		
		return fboId;
	}

	@Override
	protected boolean _setMenubar(long hwnd, long menubarhwnd) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected boolean _minimize(long hwnd, boolean minimize) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected boolean _maximize(long hwnd, boolean maximize, boolean wasMaximized) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected int _getEmbeddedX(long hwnd) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected int _getEmbeddedY(long hwnd) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void _setBounds(long hwnd, int x, int y, boolean xSet, boolean ySet, int w, int h, int cw, int ch, float xGravity, float yGravity) {
		// TODO: implement me
		warnIgnore("setBounds");
	}

	protected boolean _setVisible(long hwnd, boolean visible) {
		if (visible) {
			GLFW.glfwShowWindow(hwnd);
		} else {
			GLFW.glfwHideWindow(hwnd);
		}
		return true;
	}

	@Override
	protected boolean _setResizable(long hwnd, boolean resizable) {
		warnIgnore("setResizable");
		return false;
	}

	@Override
	protected boolean _requestFocus(long hwnd, int event) {
		warnIgnore("requestFocus");
		return true;
	}

	@Override
	protected void _setFocusable(long hwnd, boolean isFocusable) {
		warnIgnore("setFocusable");
	}

	@Override
	protected boolean _grabFocus(long hwnd) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void _ungrabFocus(long hwnd) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected boolean _setTitle(long hwnd, String title) {
		warnIgnore("setTitle");
		return false;
	}

	@Override
	protected void _setLevel(long hwnd, int level) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void _setAlpha(long hwnd, float alpha) {
		warnIgnore("setAlpha");
	}

	@Override
	protected boolean _setBackground(long hwnd, float r, float g, float b) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void _setEnabled(long hwnd, boolean enabled) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected boolean _setMinimumSize(long hwnd, int width, int height) {
		warnIgnore("setMinimumSize");
		return false;
	}

	@Override
	protected boolean _setMaximumSize(long hwnd, int width, int height) {
		warnIgnore("setMaximumSize");
		return false;
	}

	@Override
	protected void _setIcon(long hwnd, Pixels pixels) {
		warnIgnore("setIcon");
	}

	@Override
	protected void _setCursor(long hwnd, Cursor cursor) {
		warnIgnore("setCursor");
	}

	@Override
	protected void _toFront(long hwnd) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void _toBack(long hwnd) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void _enterModal(long hwnd) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void _enterModalWithWindow(long dialog, long window) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void _exitModal(long hwnd) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void _requestInput(long hwnd, String text, int type, double width, double height, double Mxx, double Mxy,
			double Mxz, double Mxt, double Myx, double Myy, double Myz, double Myt, double Mzx, double Mzy, double Mzz,
			double Mzt) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void _releaseInput(long hwnd) {
		throw new UnsupportedOperationException();
	}
	
	private void warnIgnore(String msg) {
		System.err.println("WARNING: ignoring request: " + msg);
	}
}
