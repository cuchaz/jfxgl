
# Contributing to JFXGL

JFXGL is open source software and contributions are welcome!

# Workspace setup

Compiling JFXGL requires a lot of steps because it depends on a modified version of [OpenJFX][openjfx].
We'll need to download the JDK, make changes to it, clone repositories, run patches, use build tools, etc.
This whole process is another giant hack, but it mostly works.

[openjfx]: http://wiki.openjdk.java.net/display/OpenJFX/Main

Currently there are two ways to do this:

## 1. The happy path

[JFXGL-env](https://bitbucket.org/cuchaz/jfxgl-env) is a separate project that has build scripts to setup
the development environment for JFXGL.
See the [README](https://bitbucket.org/cuchaz/jfxgl-env) there for instructions.


## 2. The hard way

If for some reason the `JFXGL-env` build scripts don't work for you, and you have superhuman patience, you
can [set up the dev environment manually](CONTRIBUTING-hard.md).


# Miscellaneous

## Create a new JFXGL patch from modified OpenJFX sources

If you modified OpenJFX and need to update the patch JFXGL uses, this handy command will do it.
```
$ cd jfxgl/openjfx
$ hg diff -g > ../jfxgl/openjfx.8u121.patch
```
Don't forget to update the `cuchaz.jfxgl.JFXGLTweaker` class with your changes too, so they get applied at runtime.