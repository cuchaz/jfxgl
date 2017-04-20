package com.sun.javafx.tk.quantum;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Test;

import com.sun.javafx.tk.Toolkit;

import cuchaz.jfxgl.JFXGLLauncher;

public class TestJFXGLToolkit {

	@Test
	public void install() {
		JFXGLLauncher.launchLambda(() -> {
			JFXGLToolkit.install();
			Toolkit toolkit = Toolkit.getToolkit();
			assertThat(toolkit, instanceOf(JFXGLToolkit.class));
		});
	}
}
