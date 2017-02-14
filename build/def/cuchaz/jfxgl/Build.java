package cuchaz.jfxgl;

import java.io.File;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkModuleDependency;
import org.jerkar.api.depmanagement.JkModuleId;
import org.jerkar.api.depmanagement.JkVersion;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.java.JkJavaCompiler;
import org.jerkar.tool.JkProject;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;
import org.jerkar.tool.builtins.javabuild.JkJavaPacker;

public class Build extends JkJavaBuild {
	
	@JkProject("../openjfx/modules/graphics")
	JkJavaBuild graphicsProject;
	
	@JkProject("../openjfx/modules/fxml")
	JkJavaBuild fxmlProject;
	
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
	public JkDependencies dependencies() {
		
		final String LWJGLVersion = "3.1.1";
		
		// NOTE: don't forget to change the Eclipse JRE to the -noFX version!
		// TODO: find out how to make jerkar do that
		
		return JkDependencies.builder()
				
			// OpenJFX projects
			.on(graphicsProject.asJavaDependency())
			.on(fxmlProject.asJavaDependency())
			
			// 3rd-party libs
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
	protected JkJavaPacker createPacker() {
		return JkJavaPacker.builder(this)
			.includeVersion(true)
			.doJar(true)
			.doSources(false)
			.extraFilesInJar(JkFileTreeSet.of(baseDir().include("LICENSE.txt")))
			.build();
	}
}
