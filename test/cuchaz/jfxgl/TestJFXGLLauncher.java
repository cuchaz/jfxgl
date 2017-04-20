/*************************************************************************
 * Copyright (C) 2017, Jeffrey W. Martin "Cuchaz"
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License Version 2 with
 * the classpath exception, as published by the Free Software Foundation.
 * 
 * See LICENSE.txt in the project root folder for the full license.
 *************************************************************************/
package cuchaz.jfxgl;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import org.junit.Test;


public class TestJFXGLLauncher {
	
	public static class LaunchableCheckClassloader {
		public static void jfxglmain(String[] args) {
			// NOTE: compare the class names, since the class instances will be from different classloaders
			assertThat(LaunchableCheckArgs.class.getClassLoader().getClass().getName(), is(JFXGLLauncher.Loader.class.getName()));
		}
	}
	
	@Test
	public void main() {
		JFXGLLauncher.launchMain(LaunchableCheckClassloader.class, null);
	}
	
	public static class LaunchableCheckArgs {
		public static void jfxglmain(String[] args) {
			assertThat(args, is(new String[] { "hello", "world" }));
		}
	}

	@Test
	public void mainArgs() {
		JFXGLLauncher.launchMain(LaunchableCheckArgs.class, new String[] { "hello", "world" });
	}
	
	@Test
	public void launchable() {
		JFXGLLauncher.launchLambda(() -> {
			assertThat(getClass().getClassLoader().getClass().getName(), is(JFXGLLauncher.Loader.class.getName()));
		});
	}
}
