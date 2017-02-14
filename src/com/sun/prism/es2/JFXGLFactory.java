package com.sun.prism.es2;

import java.util.HashMap;

import com.sun.prism.es2.GLPixelFormat.Attributes;

import cuchaz.jfxgl.Log;

public class JFXGLFactory extends GLFactory {
	
	// everything will always use the OpenGL context on the main thread
	// so make our context/drawable instances singletons
	private static long hwnd;
	private static JFXGLDrawable drawable;
	private static JFXGLContext context;
	
	static {
		hwnd = 0;
		drawable = null;
		context = null;
	}
	
	public static void init(long hwnd) {
		
		// TEMP
		Log.log("JFXGLFactory.init() hwnd=" + hwnd);
		
		JFXGLFactory.hwnd = hwnd;
		drawable = new JFXGLDrawable(hwnd);
		context = new JFXGLContext(hwnd);
	}
	
	private static void checkInit() {
		if (hwnd <= 0) {
			throw new IllegalStateException("JFXGLFactory has not been initialized yet. Call JFXGLFactory.init(hwnd)");
		}
	}
	
	public static long getHwnd() {
		checkInit();
		return hwnd;
	}
	
	public static JFXGLContext getContext() {
		checkInit();
		return context;
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
		return context;
	}

	@Override
	public JFXGLContext createGLContext(GLDrawable drawable, GLPixelFormat pixelFormat, GLContext shareCtx, boolean vSyncRequest) {
		return context;
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
	GLPixelFormat createGLPixelFormat(long nativeScreen, Attributes attrs) {
		// don't need to wrap pixel format, only consumed by Drawables
		return null;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public boolean initialize(Class psClass, Attributes attrs) {
		
		// NOTE: exceptions here get swallowed by GraphicsPipeline.createPipeline()
		// so report them here explicitly
		try {
			
			nativeCtxInfo = hwnd;
            gl2 = true;
		
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			throw t;
		}
		
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
		return context.isExtensionSupported(sglExtStr);
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
