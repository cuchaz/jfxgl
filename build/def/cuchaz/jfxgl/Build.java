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

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkFileSystemDependency;
import org.jerkar.api.depmanagement.JkModuleDependency;
import org.jerkar.api.depmanagement.JkModuleId;
import org.jerkar.api.depmanagement.JkScopedDependency;
import org.jerkar.api.depmanagement.JkVersion;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.file.JkPathFilter;
import org.jerkar.api.java.JkJavaCompiler;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.tool.builtins.eclipse.JkBuildPluginEclipse;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;
import org.jerkar.tool.builtins.javabuild.JkJavaPacker;

public class Build extends JkJavaBuild {
	
	@Override
	public JkModuleId moduleId() {
		return JkModuleId.of("cuchaz", "jfxgl");
	}
	
	@Override
	public JkVersion version() {
		return JkVersion.name("0.1");
	}

	@Override
	public String javaSourceVersion() {
		return JkJavaCompiler.V8;
	}
	
	@Override
	public JkJavaCompiler productionCompiler() {
		
		// make sure we're using the special JDK without JavaFX in it
		return JkJavaCompiler.outputtingIn(classDir())
			.andSources(sources())
			.withClasspath(depsFor(COMPILE, PROVIDED))
			.withSourceVersion(javaSourceVersion())
			.withTargetVersion(javaTargetVersion())
			.forkOnCompiler("../openjdk-8u121-noFX/bin/javac");
	}

	@Override
	public JkDependencies dependencies() {
		
		// tell the eclipse plugin to use the special JDK without JavaFX
		// NOTE: you should create a JRE in the  eclipse workspace needs to have a JRE with this name!
		String jdkName = "openjdk-8u121-noFX";
		JkBuildPluginEclipse eclipsePlugin = pluginOf(JkBuildPluginEclipse.class);
		if (eclipsePlugin != null) {
			eclipsePlugin.jreContainer = "org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/" + jdkName;
		}
		
		return JkDependencies.builder()
			
			// OpenJFX modules (already compiled)
			// NOTE: ideally these would be referenced projects in Eclipse (better IDE integration that way),
			// but jerkar wants to always compile sub-projects and I don't know how to tell it no
			// fix incoming, see github issue: https://github.com/jerkar/jerkar/issues/61
			.on(new File("../openjfx/modules/controls/bin")).scope(PROVIDED)
			.on(new File("../openjfx/modules/fxml/bin")).scope(PROVIDED)
			.on(new File("../openjfx/modules/graphics/bin")).scope(PROVIDED)
			.on(new File("../openjfx/modules/base/bin")).scope(PROVIDED)
			
			// 3rd-party libs
			.on("org.ow2.asm:asm:5.2")
			.on(lwjgl(this, "3.1.1", "glfw", "jemalloc", "opengl"))
			
			.build();
	}

	/* TEMP
	@Override
	public JkDependencies dependencies() {
		return JkDependencies.builder()
			.on("org.lwjgl:lwjgl:3.1.1")
			.on("org.lwjgl:lwjgl:3.1.1:natives-linux")
			.build();
	}
	*/
	
	public static JkDependencies lwjgl(JkJavaBuild build, String version, String ... modules) {
		JkDependencies deps = lwjgl(build, version, (String)null);
		for (String module : modules) {
			deps = deps.and(lwjgl(build, version, module));
		}
		return deps;
	}
	
	public static JkDependencies lwjgl(JkJavaBuild build, String version, String module) {
		String name = "lwjgl";
		if (module != null) {
			name += "-" + module;
		}
		String desc = "org.lwjgl:" + name + ":" + version;
		JkModuleDependency dep = JkModuleDependency.of(desc);
		return JkDependencies.of(
			JkScopedDependency.of(dep, COMPILE),
			JkScopedDependency.of(classifiedJar(build, dep, "natives-linux"), RUNTIME),
			JkScopedDependency.of(classifiedJar(build, dep, "natives-windows"), RUNTIME),
			JkScopedDependency.of(classifiedJar(build, dep, "natives-macos"), RUNTIME)
		);
	}
	
	public static JkFileSystemDependency classifiedJar(JkJavaBuild build, JkModuleDependency dep, String classifier) {
		// NOTE: better support for natives in Jerkar is in-progress
		// this is method a hacky workaround
		// see: https://github.com/jerkar/jerkar/issues/60
		
		// just manually download the jar from Maven Central, eg at URL:
		// http://search.maven.org/remotecontent?filepath=org/lwjgl/lwjgl/3.1.1/lwjgl-3.1.1-natives-linux.jar
		// http://search.maven.org/remotecontent?filepath=org/lwjgl/lwjgl-opengl/3.1.1/lwjgl-opengl-3.1.1-natives-linux.jar
		
		String filename = String.format("%s-%s-%s.jar", dep.moduleId().name(), dep.versionRange(), classifier);
		File file = new File(build.file("build/libs/runtime"), filename);
		if (!file.exists()) {
			file.getParentFile().mkdirs();
			try {
				// NOTE: use "https" protocol here, not "http"
				// Maven Central will redirect http->https, but Java's URLConnection will not follow cross-protocol redirects
				URL url = new URL(String.format("https://search.maven.org/remotecontent?filepath=%s/%s/%s/%s",
					dep.moduleId().group().replaceAll("\\.", "/"),
					dep.moduleId().name(),
					dep.versionRange(),
					filename
				));
				JkUtilsIO.copyUrlToFile(url, file);
			} catch (MalformedURLException ex) {
				throw new Error(ex);
			}
		}
		return JkFileSystemDependency.of(file);
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
			.to(this.ouputDir().file("jfxgl-controls.jar"));
	}
}
