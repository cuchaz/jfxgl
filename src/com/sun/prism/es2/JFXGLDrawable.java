package com.sun.prism.es2;

public class JFXGLDrawable extends GLDrawable {
	
	public static GLPixelFormat getGLFWPixelFormat() {
		// TODO: do we actually need this?
		return null;
	}

	JFXGLDrawable(long hwnd) {
		super(hwnd, getGLFWPixelFormat());
		setNativeDrawableInfo(hwnd);
	}

	@Override
	boolean swapBuffers(GLContext context) {
		// never actually swap buffers here
		// we'll do that in the renderer
		return true;
	}
}
