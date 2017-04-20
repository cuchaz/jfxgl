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

import org.junit.Test;


public class TestJFXGLLauncher {
	
	// NOTE: don't use hamcrest inside the launched code
	// it can't deal with being loaded by two different classloaders
	// and I don't know how to stop the main classloader from loading it
	
	public static class LaunchableCheckClassloader {
		public static void launch(String[] args) {
			String exp = JFXGLLauncher.Loader.class.getName();
			String obs = LaunchableCheckArgs.class.getClassLoader().getClass().getName();
			assert (obs == exp) : String.format("%s != %s", obs, exp);
		}
	}
	
	@Test
	public void classloader() {
		JFXGLLauncher.launch(LaunchableCheckClassloader.class, null);
	}
	
	public static class LaunchableCheckArgs {
		public static void launch(String[] args) {
			assert (args.length == 2) : String.format("%d != %d", args.length, 2);
			assert (args[0].equals("hello")) : String.format("%s != %s", args[0], "hello");
			assert (args[1].equals("world")) : String.format("%s != %s", args[0], "world");
		}
	}

	@Test
	public void args() {
		JFXGLLauncher.launch(LaunchableCheckArgs.class, new String[] { "hello", "world" });
	}
}
