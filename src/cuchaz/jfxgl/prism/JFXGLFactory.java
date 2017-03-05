package cuchaz.jfxgl.prism;

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
		drawable = new JFXGLDrawable(JFXGLContext.get().getHwnd());
		GLFactory.platformFactory = new JFXGLFactory();
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
		return JFXGLContext.get();
	}

	@Override
	public JFXGLContext createGLContext(GLDrawable drawable, GLPixelFormat pixelFormat, GLContext shareCtx, boolean vSyncRequest) {
		return JFXGLContext.get();
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
		
		// NOTE: exceptions here get swallowed by GraphicsPipeline.createPipeline()
		// so report them here explicitly
		try {
			
			nativeCtxInfo = JFXGLContext.get().getHwnd();
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
		return JFXGLContext.get().isExtensionSupported(sglExtStr);
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
