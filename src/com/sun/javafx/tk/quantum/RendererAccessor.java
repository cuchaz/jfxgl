package com.sun.javafx.tk.quantum;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

public class RendererAccessor {

	private static Field instanceReferenceField;
	
	static {
		try {
			instanceReferenceField = QuantumRenderer.class.getDeclaredField("instanceReference");
			instanceReferenceField.setAccessible(true);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public static void setRendererInstance(QuantumRenderer val) {
		try {
			@SuppressWarnings("unchecked")
			AtomicReference<QuantumRenderer> instanceReference = (AtomicReference<QuantumRenderer>)instanceReferenceField.get(null);
			instanceReference.set(val);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
}
