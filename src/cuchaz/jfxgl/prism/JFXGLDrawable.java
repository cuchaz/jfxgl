package cuchaz.jfxgl.prism;

import com.sun.prism.es2.GLContext;
import com.sun.prism.es2.GLDrawable;
import com.sun.prism.es2.GLPixelFormat;

public class JFXGLDrawable extends GLDrawable {
	
	public static GLPixelFormat getGLFWPixelFormat() {
		// TODO: do we actually need this?
		return null;
	}

	public JFXGLDrawable(long hwnd) {
		super(hwnd, getGLFWPixelFormat());
		setNativeDrawableInfo(hwnd);
	}

	@Override
	public boolean swapBuffers(GLContext context) {
		// never actually swap buffers here
		// we'll do that in the renderer
		return true;
	}
}
