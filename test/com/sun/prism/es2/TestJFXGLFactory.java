package com.sun.prism.es2;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Test;

import cuchaz.jfxgl.JFXGLLauncher;

public class TestJFXGLFactory {

	@Test
	public void install() {
		JFXGLLauncher.launchLambda(() -> {
			JFXGLContexts.app = JFXGLContext.wrapExisting(5);
			JFXGLFactory.install();
			assertThat(GLFactory.getFactory(), instanceOf(JFXGLFactory.class));
		});
	}
}
