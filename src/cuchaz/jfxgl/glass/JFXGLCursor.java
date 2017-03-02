package cuchaz.jfxgl.glass;

import com.sun.glass.ui.Cursor;
import com.sun.glass.ui.Pixels;

public class JFXGLCursor extends Cursor {
	
	protected JFXGLCursor(int type) {
		super(type);
	}
	
	@Override
	protected long _createCursor(int x, int y, Pixels pixels) {
		throw new UnsupportedOperationException("implement me!");
	}
}
