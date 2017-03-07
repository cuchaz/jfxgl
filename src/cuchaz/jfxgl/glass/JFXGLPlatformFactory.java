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

import com.sun.glass.ui.Application;
import com.sun.glass.ui.Clipboard;
import com.sun.glass.ui.Menu;
import com.sun.glass.ui.MenuBar;
import com.sun.glass.ui.MenuItem;
import com.sun.glass.ui.PlatformFactory;
import com.sun.glass.ui.delegate.ClipboardDelegate;
import com.sun.glass.ui.delegate.MenuBarDelegate;
import com.sun.glass.ui.delegate.MenuDelegate;
import com.sun.glass.ui.delegate.MenuItemDelegate;

public class JFXGLPlatformFactory extends PlatformFactory {
	
	public static void install() {
		PlatformFactory.instance = new JFXGLPlatformFactory();
	}

	@Override
	public Application createApplication() {
		return new JFXGLApplication();
	}

	@Override
	public MenuBarDelegate createMenuBarDelegate(MenuBar menubar) {
		throw new UnsupportedOperationException();
	}

	@Override
	public MenuDelegate createMenuDelegate(Menu menu) {
		throw new UnsupportedOperationException();
	}

	@Override
	public MenuItemDelegate createMenuItemDelegate(MenuItem menuItem) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ClipboardDelegate createClipboardDelegate() {
		return new ClipboardDelegate() {
			@Override
			public Clipboard createClipboard(String clipboardName) {
				// TODO: implement clipboards
				return null;
			}
		};
	}
}
