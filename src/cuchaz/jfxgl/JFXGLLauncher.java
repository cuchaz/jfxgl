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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JFXGLLauncher {
	
	private static final File OpenJFXDir = new File("../openjfx");
	private static final List<File> BinDirs = Arrays.asList(
		new File(OpenJFXDir, "modules/graphics/bin")
	);
	
	protected static class Loader extends URLClassLoader {
		
		private static ClassLoader getParentLoader() {
			return JFXGLLauncher.class.getClassLoader();
		}
		
		private static ClassLoader getRootLoader() {
			return getParentLoader().getParent();
		}
		
		public Loader() {
			super(makeURLs());
		}
		
		private static URL[] makeURLs() {
			// TODO: point to built openjfx jar?
			try {
				List<URL> urls = new ArrayList<>();
				for (File binDir : BinDirs) {
					urls.add(binDir.toURI().toURL());
				}
				urls.addAll(Arrays.asList(((URLClassLoader)getParentLoader()).getURLs()));
				URL[] array = new URL[urls.size()];
				return urls.toArray(array);
			} catch (MalformedURLException ex) {
				throw new RuntimeException(ex);
			}
		}
		
		@Override
		protected Class<?> loadClass(String name, boolean resolve)
		throws ClassNotFoundException {
			synchronized (getClassLoadingLock(name)) {
				
				Class<?> c = findLoadedClass(name);
				if (c == null) {
					
					// first, look in our URLs
					try {
						c = findClass(name);
					} catch (ClassNotFoundException e) {
						// didn't find it
					}
					
					// then, look in the root
					if (c == null) {
						c = getRootLoader().loadClass(name);
					}
				}
				
				if (resolve) {
					resolveClass(c);
				}
				
				return c;
			}
		}
	}
	
	public static void launch(Class<?> type, String[] args) {
		try (Loader loader = new Loader()) {
			Class<?> loadedType = loader.loadClass(type.getName());
			assert (loadedType.getClassLoader() == loader);
			loadedType.getMethod("launch", new Class<?>[] { String[].class })
				.invoke(null, new Object[] { args });
		} catch (Exception ex) {
			throw new RuntimeException("Can't launch class: " + type.getName(), ex);
		}
	}
}
