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
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.ZipOutputStream;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkMavenPublication;
import org.jerkar.api.depmanagement.JkMavenPublicationInfo;
import org.jerkar.api.depmanagement.JkModuleDependency;
import org.jerkar.api.depmanagement.JkModuleId;
import org.jerkar.api.depmanagement.JkPublishRepo;
import org.jerkar.api.depmanagement.JkPublisher;
import org.jerkar.api.depmanagement.JkRepo;
import org.jerkar.api.depmanagement.JkScopedDependency;
import org.jerkar.api.depmanagement.JkVersion;
import org.jerkar.api.depmanagement.JkVersionedModule;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.file.JkPathFilter;
import org.jerkar.api.java.JkJavaCompiler;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsZip;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.builtins.eclipse.JkBuildPluginEclipse;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;
import org.jerkar.tool.builtins.javabuild.JkJavaPacker;

public class Build extends JkJavaBuild {
	
	@JkDoc("path to local maven repo for publishing")
	private File pathMavenRepo = new File("maven");
	
	public Build() {
		// tell the eclipse plugin to use the special JDK without JavaFX
		// NOTE: you should create a JRE in the  eclipse workspace needs to have a JRE with this name!
		JkBuildPluginEclipse eclipse = new JkBuildPluginEclipse();
		eclipse.setStandardJREContainer("openjdk-noFX");
		plugins.configure(eclipse);
		
		// don't run the unit tests just to build
		tests.skip = true;
	}
	
	@Override
	public JkModuleId moduleId() {
		return JkModuleId.of("cuchaz", "jfxgl");
	}
	
	@Override
	public JkVersion version() {
		return JkVersion.name("0.5-SNAPSHOT");
	}

	@Override
	public String javaSourceVersion() {
		return JkJavaCompiler.V8;
	}
	
	@Override
	public JkJavaCompiler productionCompiler() {
		
		// make sure we're using the special JDK without JavaFX in it
		return super.productionCompiler().forkOnCompiler("../openjdk-noFX/bin/javac");
	}
	
	@Override
	public JkDependencies dependencies() {
		return JkDependencies.builder()
			
			// test libs
			// NOTE: these need to go first, so we override Junit libs in the OpenJFX modules
			.on("junit:junit:4.12").scope(TEST)
			.on("org.hamcrest:hamcrest-all:1.3").scope(TEST)
			
			// OpenJFX modules (already compiled)
			.on(new File("../openjfx/modules/controls/bin")).scope(PROVIDED)
			.on(new File("../openjfx/modules/fxml/bin")).scope(PROVIDED)
			.on(new File("../openjfx/modules/graphics/bin")).scope(PROVIDED)
			.on(new File("../openjfx/modules/base/bin")).scope(PROVIDED)
			
			// 3rd-party libs
			.on(lwjgl("3.1.2", "glfw", "jemalloc", "opengl", "nfd"))
			
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
	public JkFileTreeSet unitTestEditedSources() {
		return JkFileTreeSet.of(file("test"));
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
	
	/**
	 * Unless you're me, you probably don't want to run this =P
	 * 
	 * Also, don't forget to compile your jfxrt.jar first
	 */
	public void doMaven() {
		
		doPack();
		
		// publish to the local maven repo
		JkPublishRepo repo = JkRepo.maven(pathMavenRepo).asPublishRepo();
		
		// make a cross-platform jfxrt.jar
		File crossPlatformJar = ouputDir(moduleId().group() + "." + moduleId().name() + "-jfxrt-" + version() + ".jar");
		crossPlatformJar.delete();
		ZipOutputStream zout = JkUtilsZip.createZipOutputStream(crossPlatformJar, Deflater.DEFAULT_COMPRESSION);
		JkUtilsZip.mergeZip(zout, JkUtilsZip.zipFile(file("../openjfx/build/sdk/rt/lib/ext/jfxrt.jar")));
		File classesDir = file("../openjfx/modules/graphics/build/classes/main");
		File es2Dir = new File(classesDir, "com/sun/prism/es2");
		for (String os : Arrays.asList("EGLFB", "EGLX11", "IOS", "Mac", "Monocle", "Win", "X11")) {
			for (String filename : Arrays.asList("GLContext", "GLDrawable", "GLFactory", "GLPixelFormat")) {
				File file = new File(es2Dir, os + filename + ".class");
				if (!file.exists()) {
					throw new Error("Missing cross-platform ES2 class file: " + file);
				}
				JkUtilsZip.addZipEntry(zout, file, classesDir);
			}
		}
		JkUtilsIO.closeQuietly(zout);
		
		// publish jfxrt.jar
		JkMavenPublicationInfo jfxrtInfo = ownInfo(JkMavenPublicationInfo.of(
			"JavaFX for JFXGL",
			"Cross-platform version of OpenJFX that works with JFXGL",
			"https://bitbucket.org/cuchaz/jfxgl"
		));
		JkVersionedModule jfxrtMod = JkVersionedModule.of(
			JkModuleId.of(moduleId().group(), moduleId().name() + "-jfxrt"),
			version()
		); 
		JkPublisher.of(repo).publishMaven(
			jfxrtMod,
			JkMavenPublication.of(crossPlatformJar)
				.with(jfxrtInfo),
			JkDependencies.of()
		);
		
		// publish the JFXGL jar
		JkMavenPublicationInfo jfxglInfo = ownInfo(JkMavenPublicationInfo.of(
			"JFXGL",
			"Glue code that allows you to use JavaFX in your OpenGL/LWJGL3 app.",
			"https://bitbucket.org/cuchaz/jfxgl"
		));
		JkJavaPacker packer = packer();
		JkPublisher.of(repo).publishMaven(
			versionedModule(),
			JkMavenPublication.of(packer.jarFile())
				.with(jfxglInfo)
				.and(packer.jarSourceFile(), "sources"),
			JkDependencies.of()
		);
	}
	
	public JkMavenPublicationInfo ownInfo(JkMavenPublicationInfo info) {
		return info
			.andLicense(
				"GPL v2 with classpath exception",
				"https://bitbucket.org/cuchaz/jfxgl/src/default/LICENSE.txt"
			)
			.andDeveloper(
				"Jeff Martin (Cuchaz)",
				"jeff@cuchazinteractive.com",
				"Cuchaz Interactive",
				"https://www.cuchazinteractive.com/"
			);
	}
}
