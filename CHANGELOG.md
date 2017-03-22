
# JFXGL Changelog


## 2016-03-22 - v0.2

 * Switch to separate (but shared) contexts for OpenGL rendering between the host app and JavaFX.
   (This should fix all the issues with global state fighting, and has only a small performance cost)
 * `OpenGLPane` instances also render in an isolated OpenGL context to prevent global state fighting.
 * Increase OpenGL compatibility of JFXGL shaders.
 * Create [JFXGL-env][jfxgl-env] project to automate lengthy dev environment setup.

[jfxgl-env]: https://bitbucket.org/cuchaz/jfxgl-env


## 2016-03-10 - v0.1

 * Initial release
 * Hooray! =D
