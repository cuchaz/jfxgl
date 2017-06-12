
# Contributing to JFXGL

JFXGL is open source software and contributions are welcome!

# Workspace setup

Compiling JFXGL requires a lot of steps because it depends on a modified version of [OpenJFX][openjfx].
We'll need to download the JDK, make changes to it, clone repositories, run patches, use build tools, etc.
This whole process is another giant hack, but it mostly works.

[openjfx]: http://wiki.openjdk.java.net/display/OpenJFX/Main


## Sorry, Mario, but your princess is in another castle

Workspace setup is done by a script in a separate project, [JFXGL-env](https://bitbucket.org/cuchaz/jfxgl-env).
See the [README](https://bitbucket.org/cuchaz/jfxgl-env) there for instructions.


# Miscellaneous

## Create a new JFXGL patch from modified OpenJFX sources

If you modified OpenJFX and need to update the patch JFXGL uses, this handy command will do it.
```
$ cd jfxgl/openjfx
$ hg diff -g > ../JFXGL/openjfx.patch
```
If the patch file contains any changes from Eclipse metadata (like the `.classpath` file),
you may want to remove them from the patch file.
