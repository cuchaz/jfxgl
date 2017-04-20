package com.sun.glass.ui.jfxgl;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Test;

import com.sun.glass.ui.PlatformFactory;

import cuchaz.jfxgl.JFXGLLauncher;

public class TestJFXGLPlatformFactory {

	@Test
	public void install() {
		JFXGLLauncher.launchLambda(() -> {
			JFXGLPlatformFactory.install();
			assertThat(PlatformFactory.getPlatformFactory(), instanceOf(JFXGLPlatformFactory.class));
		});
	}
}
