
# JXFGL

*Glue code that allows you to use [JavaFX][javafx] in your [OpenGL][opengl]/[LWJGL3][lwjgl] app.*

[javafx]:https://en.wikipedia.org/wiki/JavaFX
[opengl]:https://www.opengl.org/
[lwjgl]:https://www.lwjgl.org/

[![JFXGL Demo](https://pbs.twimg.com/ext_tw_video_thumb/837354511853309954/pu/img/36SoaDoKpHhO3CvL.jpg)](https://twitter.com/cuchaz/status/837355916789952513)

JFXGL was developed as a component for my upcoming Java-based 2D video game engine, the [Horde Engine][horde].

[horde]:https://www.cuchazinteractive.com/horde-engine

This project is essentially one giant hack. It works by modifying a small part of the [OpenJFX][openjfx]
project to allow extending the rendering and input systems to use LWJGL/GLFW instead of the cross-platform
backend it was using before. This project is most certainly not the best way to accomplish this goal,
but it's the first way I found that works, and the resulting code has good performance.

[openjfx]: http://wiki.openjdk.java.net/display/OpenJFX/Main


## Getting started

JFXGL is designed to work just like LWJGL3 and leaves you complete control over your window
and the render loop. JFXGL lets you call the JavaFX rendering system just like any other rendering
command in an OpenGL application. Once you've [downloaded JFXGL](#download), add it to your classpath.
Then the following example will help you get started.

```java
package cuchaz.jfxgl.demo;

import java.io.IOException;

import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import cuchaz.jfxgl.CalledByEventsThread;
import cuchaz.jfxgl.JFXGL;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class HelloWorld {
	
	public static void main(String[] args)
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

## OpenGLPane

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
import cuchaz.jfxgl.controls.OpenGLPane;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class HelloWorldPane {
	
	public static void main(String[] args)
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
			glpane.setRenderer(() -> render());
			glpane.getChildren().add(new Label("Hello World!"));
			stage.setScene(new Scene(glpane));
		}
		
		@CalledByMainThread
		private void render() {
			
			GL11.glClearColor(0.8f, 0.5f, 0.5f, 1f);
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
		}
	}
}

```

See the [JFXGL-demos][demos] project for more examples.

[demos]: TODO: demos URL


## Download  {#download}

The most current release is `v0.1`.

TODO: download link.

*(When JFXGL is stable enough, maybe we can think about uploading it to [Maven Central][mvn].)*

[mvn]:http://search.maven.org/


## License

JFXGL is copyright Jeff Martin ("Cuchaz") and is released under the same license as OpenJFX.

[GNU General Public License, version 2, with the Classpath Exception](LICENSE.txt)


## Caveats

JFXGL relies crucially on modifications to OpenJFX to allow extension of the rendering system. This means
the project is inexplicably tied to a particular version of the JRE. Specifically, JFXGL is developed against
[OpenJDK v8u121-b13][8u121]. In future versions of the JRE, implementation-specific details upon which JFXGL
relies may be changed, or removed entirely. There's no guarantee the techniques used by JFXGL will be compatible
at all with future versions of OpenJDK. Applications using JFXGL are best served by distributing this specific
version of the JRE along with the application. Since JFXGL was originally designed for games, I figured this
wouldn't be much of a burden. This might be a more consequential limitation for smaller applications though.

[8u121]: http://hg.openjdk.java.net/jdk8u/jdk8u/tags

Although JFXGL depends on modifications to the JavaFX JRE extension, it is also designed to work at runtime
on an unmodified version of the JRE. This works by dynamically transforming JRE classes at runtime
(ie, "modding") using the [ASM][asm] library. Runtime bytecode manipuations are fraught with peril,
and can cause some wickedly hard to debug error messages. Perhaps it would simpler to maintain a custom fork
of OpenJFX with these modifications applied. Then applications can be distributed with a JRE containing the
customized JavaFX jar instead of the one originally supplied in OpenJDK. I'm not particularly interested in
maintaining my own OpenJFX fork though, so I went with the bytecode hacking route instead.

[asm]:http://asm.ow2.org/

Since my changes to OpenJFX horribly break encapsulation and are otherwise an abomination to good programming
practice, I doubt the OpenJFX maintainers would be interested in merging it.


## Contributing

JFXGL is open-source software and contributions are welcome. Instructions for building JFXGL are provided
in the [Guide for Contributors][contributing].

[contributing]:CONTRIBUTING.md

