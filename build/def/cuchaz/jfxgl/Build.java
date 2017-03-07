package cuchaz.jfxgl;

import java.io.File;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkFileSystemDependency;
import org.jerkar.api.depmanagement.JkModuleDependency;
import org.jerkar.api.depmanagement.JkModuleId;
import org.jerkar.api.depmanagement.JkRepos;
import org.jerkar.api.depmanagement.JkScopedDependency;
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
		return JkDependencies.of(JkScopedDependency.of(JkModuleDependency.of(desc)))
			.and(allNatives(desc));
	}
	
	public static JkDependencies allNatives(String desc) {
		return JkDependencies.of(RUNTIME,
			natives(desc, "linux"),
			natives(desc, "windows"),
			natives(desc, "macos")
		);
	}
	
	public static JkFileSystemDependency natives(String desc, String os) {
		// NOTE: better support for natives in Jerkar is in-progress
		// this is method a hacky workaround
		// see: https://github.com/jerkar/jerkar/issues/60
		JkModuleDependency modDep = JkModuleDependency.of(desc + ":natives-" + os);
		return JkFileSystemDependency.of(JkRepos.mavenCentral().get(modDep));
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
