package cuchaz.jfxgl;

import java.io.File;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkModuleDependency;
import org.jerkar.api.depmanagement.JkModuleId;
import org.jerkar.api.depmanagement.JkVersion;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.file.JkPathFilter;
import org.jerkar.api.java.JkJavaCompiler;
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
		
		// tell the eclipse plugin to use the workspace default JDK
		JkBuildPluginEclipse eclipsePlugin = pluginOf(JkBuildPluginEclipse.class);
		if (eclipsePlugin != null) {
			eclipsePlugin.jreContainer = "org.eclipse.jdt.launching.JRE_CONTAINER";
		}
		
		final String LWJGLVersion = "3.1.1";
		
		return JkDependencies.builder()
			
			// OpenJFX modules (already compiled)
			.on(new File("../openjfx/modules/controls/bin"))
			.on(new File("../openjfx/modules/fxml/bin"))
			.on(new File("../openjfx/modules/graphics/bin"))
			.on(new File("../openjfx/modules/base/bin"))
			
			// 3rd-party libs
			.on("ar.com.hjg:pngj:2.1.0")
			.on("org.joml:joml:1.9.2")
			.on("org.ow2.asm:asm:5.2")
			
			// LWJGL
			.on(         "org.lwjgl:lwjgl:" + LWJGLVersion)
			.on(uglyHack("org.lwjgl:lwjgl:" + LWJGLVersion + ":natives-linux"))
			.on(         "org.lwjgl:lwjgl-glfw:" + LWJGLVersion)
			.on(uglyHack("org.lwjgl:lwjgl-glfw:" + LWJGLVersion + ":natives-linux"))
			.on(         "org.lwjgl:lwjgl-jemalloc:" + LWJGLVersion)
			.on(uglyHack("org.lwjgl:lwjgl-jemalloc:" + LWJGLVersion + ":natives-linux"))
			.on(         "org.lwjgl:lwjgl-opengl:" + LWJGLVersion)
			.on(uglyHack("org.lwjgl:lwjgl-opengl:" + LWJGLVersion + ":natives-linux"))
			.on(         "org.lwjgl:lwjgl-openal:" + LWJGLVersion)
			.on(uglyHack("org.lwjgl:lwjgl-openal:" + LWJGLVersion + ":natives-linux"))
			.on(         "org.lwjgl:lwjgl-stb:" + LWJGLVersion)
			.on(uglyHack("org.lwjgl:lwjgl-stb:" + LWJGLVersion + ":natives-linux"))
			
			.build();
	}
	
	private File uglyHack(String desc) {
		JkModuleDependency modDep = JkModuleDependency.of(desc);
		if (modDep.classifier() == null) {
			throw new IllegalArgumentException("dependency must have classifier to resolve artifact directly");
		}
		return buildRepos().get(modDep);
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
