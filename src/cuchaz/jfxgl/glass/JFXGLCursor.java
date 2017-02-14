package cuchaz.jfxgl.glass;

import com.sun.glass.ui.Cursor;
import com.sun.glass.ui.Pixels;

public class JFXGLCursor extends Cursor {
	
	private int hotspotX;
	private int hotspotY;
	private Pixels pixels;

	protected JFXGLCursor(int type) {
		super(type);
		
		this.hotspotX = 0;
		this.hotspotY = 0;
		this.pixels = makePixels(type);
	}
	
	protected JFXGLCursor(int x, int y, Pixels pixels) {
		super(x, y, pixels);
	}

	@Override
	protected long _createCursor(int x, int y, Pixels pixels) {
		this.hotspotX = x;
		this.hotspotY = y;
		this.pixels = pixels;
		return 1l;
	}
	
	private Pixels makePixels(int type) {
		// TODO
		return null;
	}
}
