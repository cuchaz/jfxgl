
# JFXGL

*Glue code that allows you to use [JavaFX][javafx] in your [OpenGL][opengl]/[LWJGL3][lwjgl] app.*

[javafx]:https://en.wikipedia.org/wiki/JavaFX
[opengl]:https://www.opengl.org/
[lwjgl]:https://www.lwjgl.org/

[![JFXGL Demo](https://pbs.twimg.com/ext_tw_video_thumb/837354511853309954/pu/img/36SoaDoKpHhO3CvL.jpg)](https://twitter.com/cuchaz/status/837355916789952513)

JFXGL was developed as a component for my upcoming Java-based 2D video game engine, the [Horde Engine][horde].
The game engine is still in development, but hopefully I can finally finish JFXGL and go back to working on it.

[horde]:https://www.cuchazinteractive.com/

This project is essentially one giant hack, but less of a hack now than it was in previous versions. It works by
modifying a small part of the [OpenJFX][openjfx] project to allow extending the rendering and input systems
to use LWJGL/GLFW instead of the cross-platform backend it was using before. This project is most certainly
not the best way to accomplish this goal, but it's the first way I found that works, and the resulting code
has good performance.

[openjfx]: http://wiki.openjdk.java.net/display/OpenJFX/Main


## Getting started

### Dependencies

First, you'll need a Java 8 JRE. I know [OpenJDK][openjdk] works (the Oracle version is almost the exact same
thing), and maybe others do too.

[openjdk]: http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html

Use your favorite dependency manager (like [Gradle][gradle], [Jerkar][jerkar], or [Kobalt][kobalt])
to include the JFXGL jars onto your project's classpath. The newest version so far is ``0.3.1``.
Here are the module ids:
```
cuchaz:jfxgl:0.3.1
cuchaz:jfxgl-jfxrt:0.3
```

[gradle]: https://gradle.org
[jerkar]: http://project.jerkar.org
[kobalt]: http://beust.com/kobalt/home/index.html

These artifacts are hosted at the Cuchaz Interactive Maven repository. It's a non-standard repository,
so you'll need to add it to your dependency manager. The repo url is:
```
http://maven.cuchazinteractive.com
```

(When JFXGL is more stable, maybe we can upload the artifacts to Maven Central!)

Then you'll need to add the LWJGL dependencies too. LWJGL3 has a wonderful [download page][lwjgl-download]
you can use.

[lwjgl-download]: https://www.lwjgl.org/download

JFXGL also requires OpenGL drivers v3.0 or higher to be installed on your machine.


### Special main()

JFXGL uses some classloader voodoo to allow us to override classes from the JavaFX that's bundled with your
JRE. Most of the details are hidden behind a simple API, but there is one function you need to call to
make the magic happen. JFXGL uses a special main method to start your app and you can call it like this:
```java
import cuchaz.jfxgl.JFXGLLauncher;

public class Main {

	public static void main(String[] args) {
		// your app normally starts here
		// before you do anything else,
		// call the JFXGL launcher on this class
		JFXGLLauncher.launchMain(Main.class, args);
	}
	
	// JFXGL will then call this pseudo-main method
	// using the special classloader
	public static void jfxglmain(String[] args) {
	
		// start your app here like usual
		
	}

```

**WARNING** For some people using IntelliJ IDEA, the JRE jars get prepended to the classpath. This confuses the crap
out of JFXGL's classloader and you can see bizzare exceptions like ` java.lang.SecurityException: Prohibited package name`.
JRE jars should not be on the classpath under normal circumstances so JFXGL makes an attempt to recognize JRE jars on the
classpath and filter them out. If this happens, you'll see a warning on the console like:
```
JFXGL: JRE jar filtered from classpath: file:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/rt.jar
```
If, for some reason, you want to disable this automatic classpath filtering, you can by setting `JFXGLLauncher.filterJreJars
= false` before calling `JFXGLLauncher.launchMain()`.

If you want to keep the classpath filtering, but suppress the console warnings, you can set
`JFXGLLauncher.showFilterWarnings = false`.


### Kotlin support

JFXGL works in Kotlin too! Here's how to invoke the special main in Kotlin:
```kotlin
import cuchaz.jfxgl.JFXGLLauncher


fun main(args: Array<String>) {
	JFXGLLauncher.launchMain(JfxglMain::class.java, args)
}


class JfxglMain {
	companion object {
		@JvmStatic fun jfxglmain(args: Array<String>) {
			// start your app here like usual
		}
	}
}
```


### Ceylon support

JFXGL works in Ceylon too! (h/t to [Gavin King](https://twitter.com/1ovthafew) for the assist) Try this snippet:
```ceylon

import java.lang {
	ObjectArray,
	JString=String
}

import cuchaz.jfxgl {
	JFXGLLauncher
}


shared void run() {
	JFXGLLauncher.launchMain(`JfxglMain`, null);
}


shared class JfxglMain {
	
	shared static void jfxglmain(ObjectArray<JString> args) {
		// start your app here like usual
	}
	
	shared new () {}
}

```
For this to work, you have to run your Ceylon app in "flat classpath" mode. In Eclipse, try the "Run As .. FatJar" command.
On the command-line, you'll need this:
```
$ ceylon run --flat-classpath <your app info here>
```
Ceylon's module system is really sweet though. Maybe someday we can add custom support for Ceylon's classloaders.


### Hello World

JFXGL is designed to work just like LWJGL3 and leave you complete control over your window and the render loop.
JFXGL lets you call the JavaFX rendering system just like any other rendering command in an OpenGL application.
This example will help you get started:

```java
package cuchaz.jfxgl.demo;

import java.io.IOException;

import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import cuchaz.jfxgl.CalledByEventsThread;
import cuchaz.jfxgl.JFXGL;
import cuchaz.jfxgl.JFXGLLauncher;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class HelloWorld {
	
	public static void main(String[] args) {
		JFXGLLauncher.launchMain(HelloWorld.class, args);
	}
	
	public static void jfxglmain(String[] args)
	throws Exception {
		
		// create a window using GLFW
		GLFW.glfwInit();
		long hwnd = GLFW.glfwCreateWindow(300, 169, "JFXGL", MemoryUtil.NULL, MemoryUtil.NULL);
		
		// init OpenGL
		GLFW.glfwMakeContextCurrent(hwnd);
		GL.createCapabilities();
		
		try {
			
			// start the JavaFX app
			JFXGL.start(hwnd, args, new HelloWorldApp());
			
			// render loop
			while (!GLFW.glfwWindowShouldClose(hwnd)) {
				
				// render the JavaFX UI
				JFXGL.render();
				
				GLFW.glfwSwapBuffers(hwnd);
				GLFW.glfwPollEvents();
			}
			
		} finally {
			
			// cleanup
			JFXGL.terminate();
			Callbacks.glfwFreeCallbacks(hwnd);
			GLFW.glfwDestroyWindow(hwnd);
			GLFW.glfwTerminate();
		}
	}
	
	public static class HelloWorldApp extends Application {
		
		@Override
		@CalledByEventsThread
		public void start(Stage stage)
		throws IOException {
			
			// create the UI
			Label label = new Label("Hello World!");
			label.setAlignment(Pos.CENTER);
			stage.setScene(new Scene(label));
		}
	}
}

```
This approach allows compositing the JavaFX GUI onto of the render window in the main render loop
along with other rendering commands.

JFXGL also supports rendering from within the JavaFX scene graph using the `OpenGLPane` control.

### OpenGLPane

```java
package cuchaz.jfxgl.demo;

import java.io.IOException;

import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

import cuchaz.jfxgl.CalledByEventsThread;
import cuchaz.jfxgl.CalledByMainThread;
import cuchaz.jfxgl.JFXGL;
import cuchaz.jfxgl.JFXGLLauncher;
import cuchaz.jfxgl.controls.OpenGLPane;
import cuchaz.jfxgl.prism.JFXGLContext;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class HelloWorldPane {
	
	public static void main(String[] args) {
		JFXGLLauncher.launchMain(HelloWorldPane.class, args);
	}
	
	public static void jfxglmain(String[] args)
	throws Exception {
		
		// create a window using GLFW
		GLFW.glfwInit();
		long hwnd = GLFW.glfwCreateWindow(300, 169, "JFXGL", MemoryUtil.NULL, MemoryUtil.NULL);
		
		// init OpenGL
		GLFW.glfwMakeContextCurrent(hwnd);
		GL.createCapabilities();
		
		try {
			
			// start the JavaFX app
			JFXGL.start(hwnd, args, new HelloWorldPaneApp());
			
			// render loop
			while (!GLFW.glfwWindowShouldClose(hwnd)) {
				
				// render the JavaFX UI
				JFXGL.render();
				
				GLFW.glfwSwapBuffers(hwnd);
				GLFW.glfwPollEvents();
			}
			
		} finally {
			
			// cleanup
			JFXGL.terminate();
			Callbacks.glfwFreeCallbacks(hwnd);
			GLFW.glfwDestroyWindow(hwnd);
			GLFW.glfwTerminate();
		}
	}
	
	public static class HelloWorldPaneApp extends Application {
		
		private OpenGLPane glpane;
		
		@Override
		@CalledByEventsThread
		public void start(Stage stage)
		throws IOException {
	
			// create the UI
			glpane = new OpenGLPane();
			glpane.setRenderer((context) -> render(context));
			glpane.getChildren().add(new Label("Hello World!"));
			stage.setScene(new Scene(glpane));
		}
		
		@CalledByMainThread
		private void render(JFXGLContext context) {
			
			GL11.glClearColor(0.8f, 0.5f, 0.5f, 1f);
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
		}
	}
}

```

See the [JFXGL-demos][demos] project for more examples.

[demos]: https://bitbucket.org/cuchaz/jfxgl-demos


## License

JFXGL is copyright Jeff Martin ("Cuchaz") and is released under the same license as OpenJFX.

[GNU General Public License, version 2, with the Classpath Exception](LICENSE.txt)


## Caveats

JFXGL relies crucially on modifications to OpenJFX to allow extension of the rendering system. This means
the project is bound to a particular implementation of JavaFX. Thankfully, this implementation of JavaFX
is distributed with JFXGL, so JFXGL should still work on a wide range of JREs.

JFXGL uses classloader hacks to allow the bundled JavaFX implementation to override the one provided by the
JRE. If your application also uses classloader hacks, it may be incompatible with JFXGL.

In future versions of JavaFX, implementation-specific details upon which JFXGL relies may be changed,
or removed entirely. There's no guarantee the techniques used by JFXGL will be compatible with future
versions of JavaFX. It may be difficult to port JFXGL to future versions of JavaFX, or it may not.
It's hard to say for certain.


## Why not just send a PR to OpenJFX?

Since my changes to OpenJFX horribly break encapsulation and are otherwise an abomination to good programming
practice, I doubt the OpenJFX maintainers would be interested in merging it. If it turns out the maintainers
actually feel otherwise, feel free to reach out. I'm happy to share.


## Contributing

JFXGL is open-source software and contributions are welcome. Instructions for building JFXGL are provided
in the [Guide for Contributors][contributing].

[contributing]:CONTRIBUTING.md

