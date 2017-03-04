package com.sun.javafx.tk.quantum;

import java.lang.reflect.Field;

import com.sun.javafx.tk.Toolkit;

public class ToolkitAccessor {

	private static Field fxUserThreadField;
	
	static {
		try {
			fxUserThreadField = Toolkit.class.getDeclaredField("fxUserThread");
			fxUserThreadField.setAccessible(true);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public static void setFxUserThread(Thread val) {
		try {
			fxUserThreadField.set(null, val);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
}
