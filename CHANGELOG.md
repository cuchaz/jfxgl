
# JFXGL Changelog

## 2017-05-06 - v0.3.1
 * Improved support for Intellij IDEA by filtering JRE jars from launch classpath
 * Add Kotlin example to README


## 2017-04-30 - v0.3
 * Distribute customized JavaFX implementation with JFXGL, which adds support for Windows platforms.
 * Use classloader hacks to override JavaFX classes, instead of bytecode hacks.
   (allows us to bundle a full implementation of jfxrt.jar and not depend on JRE's version at all)
 * Host JFXGL artifacts on a Maven repo, to make getting started super duper easy-er.
 * Update setup scripts to improve (but not yet actually provide) support for contributing on Windows platforms.
 * Update docs


## 2017-03-22 - v0.2
 * Switch to separate (but shared) contexts for OpenGL rendering between the host app and JavaFX.
   (This should fix all the issues with global state fighting, and has only a small performance cost)
 * `OpenGLPane` instances also render in an isolated OpenGL context to prevent global state fighting.
 * Increase OpenGL compatibility of JFXGL shaders.
 * Create [JFXGL-env][jfxgl-env] project to automate lengthy dev environment setup.

[jfxgl-env]: https://bitbucket.org/cuchaz/jfxgl-env


## 2017-03-10 - v0.1
 * Initial release
 * Hooray! =D
