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

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkModuleDependency;
import org.jerkar.api.depmanagement.JkModuleId;
import org.jerkar.api.depmanagement.JkScopedDependency;
import org.jerkar.api.depmanagement.JkVersion;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.file.JkPathFilter;
import org.jerkar.api.java.JkJavaCompiler;
import org.jerkar.tool.builtins.eclipse.JkBuildPluginEclipse;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;
import org.jerkar.tool.builtins.javabuild.JkJavaPacker;

public class Build extends JkJavaBuild {
	
	public Build() {
		// tell the eclipse plugin to use the special JDK without JavaFX
		// NOTE: you should create a JRE in the  eclipse workspace needs to have a JRE with this name!
		JkBuildPluginEclipse eclipse = new JkBuildPluginEclipse();
		eclipse.setStandardJREContainer("openjdk-8u121-noFX");
		plugins.configure(eclipse);
	}
	
	@Override
	public JkModuleId moduleId() {
		return JkModuleId.of("cuchaz", "jfxgl");
	}
	
	@Override
	public JkVersion version() {
		return JkVersion.name("0.2");
	}

	@Override
	public String javaSourceVersion() {
		return JkJavaCompiler.V8;
	}
	
	@Override
	public JkJavaCompiler productionCompiler() {
		
		// make sure we're using the special JDK without JavaFX in it
		return super.productionCompiler().forkOnCompiler("../openjdk-8u121-noFX/bin/javac");
	}
	
	@Override
	public JkDependencies dependencies() {
		return JkDependencies.builder()
			
			// OpenJFX modules (already compiled)
			.on(new File("../openjfx/modules/controls/bin")).scope(PROVIDED)
			.on(new File("../openjfx/modules/fxml/bin")).scope(PROVIDED)
			.on(new File("../openjfx/modules/graphics/bin")).scope(PROVIDED)
			.on(new File("../openjfx/modules/base/bin")).scope(PROVIDED)
			
			// 3rd-party libs
			.on("org.ow2.asm:asm:5.2")
			.on(lwjgl("3.1.1", "glfw", "jemalloc", "opengl"))
			
			.build();
	}
	
	public static JkDependencies lwjgl(String version, String ... modules) {
		JkDependencies deps = lwjgl(version, (String)null);
		for (String module : modules) {
			deps = deps.and(lwjgl(version, module));
		}
		return deps;
	}
	
	public static JkDependencies lwjgl(String version, String module) {
		String name = "lwjgl";
		if (module != null) {
			name += "-" + module;
		}
		String desc = "org.lwjgl:" + name + ":" + version;
		JkModuleDependency dep = JkModuleDependency.of(desc);
		return JkDependencies.of(
			JkScopedDependency.of(dep, COMPILE),
			JkScopedDependency.of(dep.classifier("natives-linux"), RUNTIME),
			JkScopedDependency.of(dep.classifier("natives-windows"), RUNTIME),
			JkScopedDependency.of(dep.classifier("natives-macos"), RUNTIME)
		);
	}
	
	@Override
	public JkFileTreeSet editedSources() {
		return JkFileTreeSet.of(file("src"));
	}
	
	@Override
	public JkFileTreeSet editedResources() {
		return JkFileTreeSet.of(file("resources"));
	}
	
	@Override
	protected JkJavaPacker createPacker() {
		return JkJavaPacker.builder(this)
			.includeVersion(true)
			.doJar(true)
			.doSources(true)
			.extraFilesInJar(JkFileTreeSet.of(baseDir().include("LICENSE.txt")))
			.build();
	}
	
	public void doMakeControlsJar() {
		compile();
		JkFileTreeSet.of(classDir())
			.andFilter(JkPathFilter.include("cuchaz/jfxgl/controls/**/*.*"))
			.zip()
			.to(ouputDir().file("jfxgl-controls.jar"));
	}
}
