
# Contributing to JFXGL

JFXGL is open source software and contributions are welcome!

# Workspace setup

Compiling JFXGL requires a lot of steps because it depends on a modified version of [OpenJFX][openjfx].
We'll need to download the JDK, make changes to it, clone repositories, run patches, use build tools, etc.
This whole process is another giant hack, but it mostly works.a

[openjfx]: http://wiki.openjdk.java.net/display/OpenJFX/Main


## Sorry, Mario, but your princess is in another castle

Workspace setup is done by a script in a separate project, [JFXGL-env](https://github.com/cuchaz/jfxgl-env).
See the [README](https://github.com/cuchaz/jfxgl-env) there for instructions.


# Building JFXGL

JFXGL has two artifacts: The jar file for JFXGL itself, and the jar file for our customized version of OpenJFX.
JFXGL uses the [Jerkar][jerkar] build tool to automate building these artifacts. A version of Jerkar has been embedded into
the JFXGL project so you only need to invoke the provided shell scripts to use it.

[jerkar]: http://project.jerkar.org

To build both artifacts, run the command:
```
$ cd jfxgl-env/JFXGL
$ ./jerkar doMaven
```
This will create the directory `jfxgl-env/JFXGL/maven` containing a maven-style repository with the build artifacts.

The `jfxgl.jar` artifact is compiled from the sources in your `jfxgl-env/JFXGL` project. This is typical behavior for
a build tool.

Our OpenJFX artifact is a bit different though. The `jfxgl-jfxrt.jar` artifact containing our customized version of
OpenJFX is built based on the current compiled class files of your `jfxgl-env/openjfx` project. Jerkar's build script
will not perform this compilation automatically. You'll need to run OpenJFX's gradle build script to perform that compilation.


# Miscellaneous

## Create a new JFXGL patch from modified OpenJFX sources

If you modified OpenJFX and need to update the patch JFXGL uses, this handy command will do it.
```
$ cd jfxgl-env/openjfx
$ hg diff -g > ../JFXGL/openjfx.patch
```
If the patch file contains any changes from Eclipse metadata (like the `.classpath` file),
you may want to remove them from the patch file.
