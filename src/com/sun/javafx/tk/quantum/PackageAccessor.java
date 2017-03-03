package com.sun.javafx.tk.quantum;

import javafx.scene.Scene;

public class PackageAccessor {

	public static void addRepaintSceneRenderJob(Scene scene) {
		@SuppressWarnings("deprecation")
		ViewScene viewScene = (ViewScene)scene.impl_getPeer();
		viewScene.repaint();
	}
}
