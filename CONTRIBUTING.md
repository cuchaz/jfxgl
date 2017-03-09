
# Contributing to JFXGL

JFXGL is open source software and contributions are welcome!

# Workspace setup

Compiling JFXGL requires a lot of steps because it depends on a modified version of [OpenJFX][openjfx].
We'll need to download the JDK, make changes to it, clone repositories, run patches, use build tools, etc.
This whole process is another giant hack, but it mostly works.

[openjfx]: http://wiki.openjdk.java.net/display/OpenJFX/Main


## 1. Download OpenJDK

Download [OpenJDK 8u121][8u121] for your platform.
We'll be making a few copies of OpenJDK along with downloading other projects, so it'll help to keep to everything
organized under a main project folder. Let's call this parent folder `jfxgl`. Or call it whatever you like. This
document will refer to subfolders in this folder as `jfxgl/subfolder`.

[8u121]: http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html

Extract OpenJDK 8u121 to `jfxgl/openjdk-8u121`. This will be our unmodified version of OpenJDK.


## 2. Copy OpenJDK and delete the JavaFX jar

Since we want to modify JavaFX, and JavaFX is loaded as an extension to the JRE, it's not possible to compile
replacement classes for JavaFX and have them loaded by a regular Java classloader. The classes in the JRE
will always be used instead of the classes in our project. To work around this, we need to create a copy of
the JDK that does not have JavaFX classes at all. Then we can load our compiled JavaFX classes in a regular
Java classloader.

Make a copy of `jfxgl/openjdk-8u121` into the folder `jfxgl/openjdk-8u121-noFX`. Then find the file
`jfxgl/openjdk-8u121-noFX/jre/lib/ext/jfxrt.jar` and delete it.

`jfxrt.jar` (JavaFX runtime) is the Jar file containing the compiled classes for JavaFX.


## 3. Clone OpenJFX using Mercurial

OpenJFX, like OpenJDK, is developed using the [Mercurial][hg] source control tool.
JFXGL was developed against commit `149fdbc41c8f5ab43c0414b970d9133e1f4e9cbd` of the OpenJFX project, which
was made on 2017-01-28. Since JFXGL is a relatively small patch of entirely accesss modifications to
classes/fields/methods/etc, it can probably be applied to other more recent commits as well. But since I used
this commit in particular, it's the only one I can guarantee will work.

```
$ mkdir jfxgl/openjfx
$ cd jfxgl/openjfx
$ hg clone -r 149fdbc41c8f5ab43c0414b970d9133e1f4e9cbd http://hg.openjdk.java.net/openjfx/8u-dev/rt .
```

[hg]: https://www.mercurial-scm.org/

The whole repo is about 690 MiB, so the download can take a while.


## 4. Build OpenJFX

OpenJFX uses the [Gradle][gradle] build tool, which under normal circumstances, would be a nice
easy way to [build OpenJFX][building-openjfx].

[building-openjfx]: https://wiki.openjdk.java.net/display/OpenJFX/Building+OpenJFX
[gradle]: https://gradle.org/

Instead, we're going to hack the shit out of OpenJFX and nothing will be easy anymore. We'll have
to build (or somewhat build, as you'll see) OpenJFX the hard way.

These instructions are written for the [Eclipse][eclipse] IDE and build system, but they could probably
work on any build system that has a Generously Forgiving Compiler. Sadly, Gradle is too rigid and linear
to use for this purpose.

[eclipse]: http://www.eclipse.org/

First, we actually will run the Gradle build, because it downloads some dependencies and generates
some sources we need. But first, we have to configure Gradle.
```
$ cd jfxgl/openjfx
$ cp gradle.properties.template gradle.properties
```
Find the line:
```
#JDK_HOME = /path/to/the/jdk
```
and replace it with:
```
JDK_HOME = /path/to/jfxgl/openjdk-8u121-noFX
```
Make sure `/path/to/jfxgl/openjdk-8u121-noFX` is the full absolute path to the "noFX" JDK on your system.
Then we can run the Gradle build.
```
$ cd jfxgl/openjfx
$ gradle
```
That should just work out-of-the-box. If not, check the [documentation at OpenJFX][building-openjfx] for help.

Then, make an Eclipse workspace in the `jfxgl` folder. Import all the projects in the `jfxgl/openjfx/`
folder and sub-folders.
(Right click "Package Explorer" view > "Import...", choose "General" > "Existing Projects Into Workspace",
make sure "Search for nested projects" is checked)
These projects all have build information for Eclipse already, so we don't have to do too much extra work
here thankfully. Once you've imported all the projects, Eclipse will go mad trying to build them all.
You'll end up with tens of thousands of compiler errors, but that's actually ok. Eclipse should have
successfully built all the pieces we'll actually need. All the errors are in parts we won't use.

If Eclipse can't figure out how to build OpenJFX, it might be because Eclipse got stuck on the `buildSrc`
project. If there's a big red exclamation point on the project's icon in the "Package Explorer", then
Eclipse is stuck. Sometimes, it can't find the dependency jars, even though the files really are there.
I think Eclipse is using the wrong folder to resolve relative paths, but who knows what's really going on.
If this happens, you can fix it by just removing the 5 missing jars from the project's classpath.
(Right click project > "Build Path" > "Configure Build Path...", "Libraries" tab)
Turns out we don't really need them anyway.


## 5. Clone JFXGL using Mercurial

Next, download the JFXGL project.

```
$ mkdir jfxgl/JFXGL
$ cd jfxgl/JFXGL
$ hg clone https://cuchaz@bitbucket.org/cuchaz/jfxgl .
```
We actually need to name the folder `JFXGL` (in all caps) because some case-sensitive tools we'll use
later will look for that exact string.

Add it to your Eclipse workspace using the same import project procedure we used for OpenJFX.
The project won't compile yet. Don't worry, we'll fix that in the next steps.


## 6. Setup the JFXGL project classpath

To compile JFXGL correctly, we need to tell Eclipse about our "noFX" copy of OpenJDK. In the menu:
"Window" > "Preferences", "Java" > "Installed JREs", "Add" button, "Standard VM", "Next".
Set "JRE_HOME" to `jfxgl/openjdk-8u121-noFX`, then "Finish". Make sure the JRE name is
exactly "openjdk-8u121-noFX".

JFXGL uses the [Jerkar][jerkar] build tool to handle compilation and dependency management.
[Install Jerkar][install-jerkar] if you haven't already (and make sure to setup the classpath
variables in Eclipse), then generate the classpath for JFXGL:
```
$ cd jfxgl/JFXGL
$ jerkar eclipse#generateFiles
```

[jerkar]: http://project.jerkar.org
[install-jerkar]: http://project.jerkar.org/documentation/latest/getting_started.html

At this point, Eclipse should be able to compile JFXGL.

Well, mostly.

(You may need to "Refresh" the project, so Eclipse detects the classpath changes made by Jerkar.
Also, double check you configured **Jerkar's classpath variables in Eclipse).

We should get some compiler errors for the JFXGL project. Eclipse will bury them in a sea of
errors from the other projects, but you can filter the "Problems" view by going to its menu (it looks
like a little down arrow): "Show" > "Errors/Warnings on Project".

These errors happen because JFXGL depends on a modified version of OpenJFX, but we haven't
modified OpenJFX yet.


## 7. Apply the JFXGL patch to OpenJFX

```
$ cd jfxgl/openjfx
$ hg patch --no-commit ../JFXGL/openjfx.8u121.patch
```

If that worked, then we're done with this step! Yay!

If it didn't work, and hg complained about uncommitted changes (if, say, we modifed the classpath for `buildSrc`),
we'll have to revert those changes before we can patch because Mercurial is dumb sometimes. We could `shelve`
the changes, but somehow that extension is not enabled by default. Anyway, if you need it:
```
$ cd jfxgl/openjfx
$ hg revert --all
$ hg patch --no-commit ../JFXGL/openjfx.8u121.patch
```
Then edit the `buildSrc` project again to fix the classpath.

Then recompile the `graphics` project in Eclipse. Eclipse will complain horribly and report even more
compiler errors. This is actually ok, because the JFXGL patch breaks a lot of things. Thankfully,
we don't actually care about any of the things we broke, because JFXGL replaces all of them.
Luckily for us, Eclipse will still faithfully compile all the classes that aren't broken.

To fix these compiler errors, we'd have to update all the downstream code that uses the classes changed by
the JFXGL patch. Frankly, I'm too lazy to do that, and it would make the patch much bigger anyway.
We won't actually need those classes anymore, so who cares if they no longer compile.

Now, we can recompile JFXGL and all the compiler errors should be gone.

**Congratulation! You should have a working JFXGL build environment now!**


## 8. Run the JFXGL demos

JFXGL has a demos project to show some examples of how to use JFXGL. It's also a good test to
see if everything is working correctly.
```
$ mkdir jfxgl/JFXGL-demos
$ cd jfxgl/JFXGL-demos
$ hg clone hg clone https://cuchaz@bitbucket.org/cuchaz/jfxgl-demos .
$ jerkar eclipse#generateFiles
```

Then import the demos project into your Eclipse workspace. Now that our environment is setup correctly,
this project should build in Eclipse with no trouble at all.

Then find the `HelloWorld` class and run it. (right click, "Run As" > "Java Application")

You can try the `HelloWorldPane`, `demo.overlay.Main`, and `demo.pane.Main` classes too.


# Addendum

## Create a new JFXGL patch from OpenJFX sources
```
$ cd jfxgl/openjfx
$ hg diff -g > ../jfxgl/openjfx.8u121.patch
```
