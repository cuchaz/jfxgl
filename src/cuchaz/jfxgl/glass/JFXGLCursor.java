/*************************************************************************
 * Copyright (C) 2017, Jeffrey W. Martin "Cuchaz"
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License Version 2 with
 * the classpath exception, as published by the Free Software Foundation.
 * 
 * See LICENSE.txt in the project root folder for the full license.
 *************************************************************************/
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
