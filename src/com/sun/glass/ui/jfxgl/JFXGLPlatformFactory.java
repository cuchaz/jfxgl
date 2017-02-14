package com.sun.glass.ui.jfxgl;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.Menu;
import com.sun.glass.ui.MenuBar;
import com.sun.glass.ui.MenuItem;
import com.sun.glass.ui.PlatformFactory;
import com.sun.glass.ui.delegate.ClipboardDelegate;
import com.sun.glass.ui.delegate.MenuBarDelegate;
import com.sun.glass.ui.delegate.MenuDelegate;
import com.sun.glass.ui.delegate.MenuItemDelegate;

import cuchaz.jfxgl.glass.JFXGLApplication;

public class JFXGLPlatformFactory extends PlatformFactory {

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
		throw new UnsupportedOperationException();
	}
}
