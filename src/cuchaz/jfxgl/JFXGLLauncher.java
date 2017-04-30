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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;

import sun.reflect.ConstantPool;

public class JFXGLLauncher {
	
	protected static class Loader extends URLClassLoader {
		
		private static ClassLoader getParentLoader() {
			return JFXGLLauncher.class.getClassLoader();
		}
		
		private static ClassLoader getRootLoader() {
			return getParentLoader().getParent();
		}
		
		public Loader() {
			super(((URLClassLoader)getParentLoader()).getURLs());
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
	
	public static void launchMain(Class<?> type, String[] args) {
		try (Loader loader = new Loader()) {
			
			// take over the thread loader, so spawned threads will inherit it
			Thread.currentThread().setContextClassLoader(loader);
			
			// call jfxglmain()
			Class<?> loadedType = loader.loadClass(type.getName());
			assert (loadedType.getClassLoader() == loader);
			loadedType.getMethod("jfxglmain", new Class<?>[] { String[].class })
				.invoke(null, new Object[] { args });
			
		} catch (InvocationTargetException ex) {
			
			// application error, show the message as simply as possible
			ex.getCause().printStackTrace();
			
		} catch (Exception ex) {
			
			// launch error, show all the debug info
			throw new RuntimeException("Can't launch class: " + type.getName(), ex);
		}
	}
	
	public static interface Launchable {
		void launch();
	}
	
	public static void launchLambda(Launchable launchable) {
		
		// parse the lambda info
		String lambdaName = launchable.getClass().getName();
		String outerClassName = lambdaName.replaceFirst("\\$\\$Lambda.*", "");
		
		// lambda instances are dynamically-generated classes at runtime
		// to get the static method it calls, we have to read the class data directly
		String methodName = null;
		try {
			Method getConstantPool = Class.class.getDeclaredMethod("getConstantPool");
			getConstantPool.setAccessible(true);
			ConstantPool constantPool = (ConstantPool) getConstantPool.invoke(launchable.getClass());
			for (int i=0; i<constantPool.getSize(); i++) {
				try {
					String[] methodRefInfo = constantPool.getMemberRefInfoAt(i);
					if (methodRefInfo[0].equals(outerClassName.replaceAll("\\.", "/")) && methodRefInfo[1].startsWith("lambda$")) {
						methodName = methodRefInfo[1];
						break;
					}
				} catch (IllegalArgumentException ex) {
					// not a method ref
				}
			}
		} catch (Exception ex) {
			throw new Error(ex);
		}
		
		if (methodName == null) {
			throw new Error("can't launch lambda, can't find method to call");
		}
		
		try (Loader loader = new Loader()) {
			Class<?> loadedType = loader.loadClass(outerClassName);
			assert (loadedType.getClassLoader() == loader);
			Object loadedInstance = loadedType.newInstance();
			Method method = loadedType.getDeclaredMethod(methodName, new Class<?>[] {});
			method.setAccessible(true);
			method.invoke(loadedInstance, new Object[] {});
		} catch (Exception ex) {
			throw new RuntimeException("Can't launch lambda: " + launchable.getClass().getName(), ex);
		}		
	}
}
