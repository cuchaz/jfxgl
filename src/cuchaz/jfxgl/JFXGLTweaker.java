/*************************************************************************
 * Copyright (C) 2017, Jeffrey W. Martin "Cuchaz"
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License Version 2 with
 * the classpath exception, as published by the Free Software Foundation.
 * 
 * See LICENSE.txt in the project root folder for the full license.
 *************************************************************************/
package cuchaz.jfxgl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import sun.misc.IOUtils;

public class JFXGLTweaker {
	
	// all user classes are loaded by the usual class loader
	// OpenJFX (and all JRE extensions) is loaded in the root classloader
	private static final ClassLoader loader;
	private static final ClassLoader jreLoader;
		
	private static Method defineClassMethod = null;
	private static Method resolveClassMethod = null;
	
	static {
		
		// get our class loaders
		loader = JFXGLTweaker.class.getClassLoader();
		ClassLoader rootLoader = loader;
		while (rootLoader.getParent() != null) {
			rootLoader = rootLoader.getParent();
		}
		jreLoader = rootLoader;
		
		try {
			
			defineClassMethod = findMethod(loader.getClass(), "defineClass", String.class, byte[].class, int.class, int.class);
			defineClassMethod.setAccessible(true);
			resolveClassMethod = findMethod(loader.getClass(), "resolveClass", Class.class);
			resolveClassMethod.setAccessible(true);
			
		} catch (Exception ex) {
			throw new Error("don't know how to tweak classes with this classloader: " + loader, ex);
		}
	}
	
	private static String classnameToPath(String classname) {
		return classname.replaceAll("\\.", "/") + ".class";
	}
	
	private static boolean hasJREClass(String name) {
		return jreLoader.getResource(classnameToPath(name)) != null;
	}
	
	private static Method findMethod(Class<?> c, String name, Class<?> ... argTypes)
	throws NoSuchMethodException {
		while (c != Object.class) {
			try {
				return c.getDeclaredMethod(name, argTypes);
			} catch (NoSuchMethodException ex) {
				// not there, keep moving up the chain
				c = c.getSuperclass();
			}
		}
		throw new NoSuchMethodException(name);
	}
	
	private static void defineClass(ClassLoader loader, String name, byte[] bytecode)
	throws Exception {
		Class<?> c = (Class<?>)defineClassMethod.invoke(loader, name, bytecode, 0, bytecode.length);
		resolveClassMethod.invoke(loader, c);
	}
	
	private static class AccessTweaker{
		
		private String classname;
		private boolean unfinal;
		private boolean protectify;
		private boolean publify;
		private Map<String,Integer> unfinalFields;
		private Map<String,Integer> unfinalMethods;
		private Map<String,Integer> protectifyFields;
		private Map<String,Integer> protectifyMethods;
		private Map<String,Integer> publifyFields;
		private Map<String,Integer> publifyMethods;
		
		public AccessTweaker(String classname) {
			this.classname = classname;
			unfinal = false;
			publify = false;
			unfinalFields = new HashMap<>();
			unfinalMethods = new HashMap<>();
			protectifyFields = new HashMap<>();
			protectifyMethods = new HashMap<>();
			publifyFields = new HashMap<>();
			publifyMethods = new HashMap<>();
		}
		
		public AccessTweaker unfinal() {
			unfinal = true;
			return this;
		}
		
		@SuppressWarnings("unused")
		public AccessTweaker protectify() {
			protectify = true;
			return this;
		}
		
		public AccessTweaker publify() {
			publify = true;
			return this;
		}
		
		public AccessTweaker unfinal(String ref) {
			if (isMethod(ref)) {
				unfinalMethods.put(stripParens(ref), 0);
			} else {
				unfinalFields.put(ref, 0);
			}
			return this;
		}
		
		public AccessTweaker protectify(String ref) {
			if (isMethod(ref)) {
				protectifyMethods.put(stripParens(ref), 0);
			} else {
				protectifyFields.put(ref, 0);
			}
			return this;
		}
		
		public AccessTweaker publify(String ref) {
			if (isMethod(ref)) {
				publifyMethods.put(stripParens(ref), 0);
			} else {
				publifyFields.put(ref, 0);
			}
			return this;
		}
		
		private boolean isMethod(String ref) {
			return ref.endsWith("()");
		}
		
		private String stripParens(String ref) {
			return ref.substring(0, ref.length() - 2);
		}
		
		public void tweak() {
			
			try (InputStream in = jreLoader.getResourceAsStream(classnameToPath(classname))) {
				
				// read the class bytecode
				byte[] bytecode = IOUtils.readFully(in, -1, true);
				
				// transform it
				ClassWriter cw = new ClassWriter(0);
				ClassReader cr = new ClassReader(bytecode);
				cr.accept(new Visitor(cw, this), 0);
				bytecode = cw.toByteArray();
				
				// since I just spent 6 goddamned hours tracking a bug down to here,
				// let's do some extra checks to make sure all the fields/methods get matched
				List<String> messages = new ArrayList<>();
				checkUnmatched(messages, unfinalFields, "unfinal field");
				checkUnmatched(messages, protectifyFields, "protectify field");
				checkUnmatched(messages, publifyFields, "publify field");
				checkUnmatched(messages, unfinalMethods, "unfinal method");
				checkUnmatched(messages, protectifyMethods, "protectify method");
				checkUnmatched(messages, publifyMethods, "publify method");
				if (!messages.isEmpty()) {
					throw new Error("Some fields/methods were not matched by Tweaker for " + classname + "."
						+ "\nNormally this would cause ridiculuously hard-to-find bugs, but thankfully someone wrote a nice checker. =)"
						+ "\n\t" + String.join("\n\t", messages));
				}
				
				// give it to the JVM
				defineClass(jreLoader, classname, bytecode);
				
			} catch (Exception ex) {
				throw new Error("can't tweak class: " + classname, ex);
			}
		}
		
		private void checkUnmatched(List<String> messages, Map<String,Integer> map, String desc) {
			for (Map.Entry<String,Integer> entry : map.entrySet()) {
				String name = entry.getKey();
				int used = entry.getValue();
				if (used <= 0) {
					messages.add(desc + " " + name);
				}
			}
		}
		
		private static class Visitor extends ClassVisitor {
			
			private AccessTweaker at;

			public Visitor(ClassVisitor out, AccessTweaker at) {
				super(Opcodes.ASM5, out);
				this.at = at;
			}
			
			@Override
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				
				if (at.unfinal) {
					access = unfinal(access);
				}
				if (at.protectify) {
					access = protectify(access);
				}
				if (at.publify) {
					access = publify(access);
				}
				
				cv.visit(version, access, name, signature, superName, interfaces);
			}
			
			@Override
			public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
				
				if (hasAndIncrement(at.unfinalFields, name)) {
					access = unfinal(access);
				}
				if (hasAndIncrement(at.protectifyFields, name)) {
					access = protectify(access);
				}
				if (hasAndIncrement(at.publifyFields, name)) {
					access = publify(access);
				}
				
				return cv.visitField(access, name, desc, signature, value);
			}
			
			@Override
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				
				// ignore signatures... who cares if we transform overloads too
				
				if (hasAndIncrement(at.unfinalMethods, name)) {
					access = unfinal(access);
				}
				if (hasAndIncrement(at.protectifyMethods, name)) {
					access = protectify(access);
				}
				if (hasAndIncrement(at.publifyMethods, name)) {
					access = publify(access);
				}
			
				return cv.visitMethod(access, name, desc, signature, exceptions);
			}
			
			private boolean hasAndIncrement(Map<String,Integer> map, String name) {
				if (map.containsKey(name)) {
					map.put(name, map.get(name) + 1);
					return true;
				}
				return false;
			}
			
			private int unfinal(int access) {
				
				access &= ~Opcodes.ACC_FINAL;
				
				return access;
			}
			
			private int protectify(int access) {
				
				access &= ~Opcodes.ACC_PRIVATE;
				access &= ~Opcodes.ACC_PUBLIC;
				access |= Opcodes.ACC_PROTECTED;
				
				return access;
			}
			
			private int publify(int access) {
				
				access &= ~Opcodes.ACC_PRIVATE;
				access &= ~Opcodes.ACC_PROTECTED;
				access |= Opcodes.ACC_PUBLIC;
				
				return access;
			}
		}
	}
	
	// for debugging a real JRE environment
	@SuppressWarnings("unused")
	private static void replaceJREClass(String classname) {
		
		File dir = new File("../openjfx/modules/graphics/bin");
		File file = new File(dir, classnameToPath(classname));
		System.out.println(file.getAbsolutePath());
		
		try (InputStream in = new FileInputStream(file)) {
			defineClass(jreLoader, classname, IOUtils.readFully(in, -1, true));
		} catch (Exception ex) {
			throw new Error("can't load class: " + classname, ex);
		}
	}
	
	public static void tweak() {
		
		// are there OpenJFX classes in the JRE loader?
		if (!hasJREClass("com.sun.prism.es2.GLFactory")) {
			
			// nope, not running OpenJFX as a JRE extension
			// assume dev environment where OpenJFX classes are in the regular classloader
			// and they've been compiled from modified source
			// which means we don't have to transform anything
			return;
		}
		
		// these are tweaks to OpenJFX classes that allow us to hack the JFXGL classes into the JRE
		// these changes need to match the patch against the HG commit so the runtime classes match the patched source
		// sadly, package access doesn't appear to work across different class loaders,
		// so we can't use the package shadowing trick to get access to JRE protected scopes
		// we actually have to change everything we need to touch from package-protected to actual protected, or public
		
		// NOTE: tweak order is important, since some classes induce loading of others
		
		new AccessTweaker("com.sun.prism.es2.GLPixelFormat")
			.publify()
			.tweak();
		
		new AccessTweaker("com.sun.prism.es2.GLPixelFormat$Attributes")
			.publify()
			.tweak();
		
		new AccessTweaker("com.sun.prism.es2.GLGPUInfo")
			.publify()
			.tweak();
		
		new AccessTweaker("com.sun.prism.es2.GLFactory")
			.publify()
			.unfinal("platformFactory")
			.publify("platformFactory")
			.protectify("nativeCtxInfo")
			.protectify("gl2")
			.protectify("<init>()")
			.protectify("getPreQualificationFilter()")
			.protectify("getBlackList()")
			.protectify("isQualified()")
			.protectify("createGLContext()")
			.protectify("createGLDrawable()")
			.protectify("createDummyGLDrawable()")
			.protectify("createGLPixelFormat()")
			.protectify("initialize()")
			.protectify("isGLExtensionSupported()")
			.protectify("isNPOTSupported()")
			.protectify("getAdapterCount()")
			.protectify("getAdapterOrdinal()")
			.protectify("updateDeviceDetails()")
			.protectify("printDriverInformation()")
			.tweak();
		
		new AccessTweaker("com.sun.prism.es2.GLDrawable")
			.publify()
			.protectify("<init>()")
			.protectify("setNativeDrawableInfo()")
			.protectify("swapBuffers()")
			.tweak();
		
		new AccessTweaker("com.sun.prism.es2.GLContext")
			.publify()
			.protectify("<init>()")
			
			// constants
			.protectify("GL_ZERO")
			.protectify("GL_ONE")
			.protectify("GL_SRC_COLOR")
			.protectify("GL_ONE_MINUS_SRC_COLOR")
			.protectify("GL_DST_COLOR")
			.protectify("GL_ONE_MINUS_DST_COLOR")
			.protectify("GL_SRC_ALPHA")
			.protectify("GL_ONE_MINUS_SRC_ALPHA")
			.protectify("GL_DST_ALPHA")
			.protectify("GL_ONE_MINUS_DST_ALPHA")
			.protectify("GL_CONSTANT_COLOR")
			.protectify("GL_ONE_MINUS_CONSTANT_COLOR")
			.protectify("GL_CONSTANT_ALPHA")
			.protectify("GL_ONE_MINUS_CONSTANT_ALPHA")
			.protectify("GL_SRC_ALPHA_SATURATE")
			.protectify("GL_FLOAT")
			.protectify("GL_UNSIGNED_BYTE")
			.protectify("GL_UNSIGNED_INT_8_8_8_8_REV")
			.protectify("GL_UNSIGNED_INT_8_8_8_8")
			.protectify("GL_UNSIGNED_SHORT_8_8_APPLE")
			.protectify("GL_RGBA")
			.protectify("GL_BGRA")
			.protectify("GL_RGB")
			.protectify("GL_LUMINANCE")
			.protectify("GL_ALPHA")
			.protectify("GL_RGBA32F")
			.protectify("GL_YCBCR_422_APPLE")
			.protectify("GL_TEXTURE_2D")
			.protectify("GL_TEXTURE_BINDING_2D")
			.protectify("GL_NEAREST")
			.protectify("GL_LINEAR")
			.protectify("GL_NEAREST_MIPMAP_NEAREST")
			.protectify("GL_LINEAR_MIPMAP_LINEAR")
			.protectify("GL_UNPACK_ALIGNMENT")
			.protectify("GL_UNPACK_ROW_LENGTH")
			.protectify("GL_UNPACK_SKIP_PIXELS")
			.protectify("GL_UNPACK_SKIP_ROWS")
			.protectify("WRAPMODE_REPEAT")
			.protectify("WRAPMODE_CLAMP_TO_EDGE")
			.protectify("WRAPMODE_CLAMP_TO_BORDER")
			.protectify("GL_BACK")
			.protectify("GL_FRONT")
			.protectify("GL_NONE")
			.protectify("GL_MAX_FRAGMENT_UNIFORM_COMPONENTS")
			.protectify("GL_MAX_FRAGMENT_UNIFORM_VECTORS")
			.protectify("GL_MAX_TEXTURE_IMAGE_UNITS")
			.protectify("GL_MAX_TEXTURE_SIZE")
			.protectify("GL_MAX_VERTEX_ATTRIBS")
			.protectify("GL_MAX_VARYING_COMPONENTS")
			.protectify("GL_MAX_VARYING_VECTORS")
			.protectify("GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS")
			.protectify("GL_MAX_VERTEX_UNIFORM_COMPONENTS")
			.protectify("GL_MAX_VERTEX_UNIFORM_VECTORS")
			.protectify("MAPTYPE_DIFFUSE")
			.protectify("MAPTYPE_SPECULAR")
			.protectify("MAPTYPE_BUMP")
			.protectify("MAPTYPE_SELFILLUM")
			.protectify("NUM_MATRIX_ELEMENTS")
			
			// methods
			.protectify("activeTexture()")
			.protectify("bindFBO()")
			.protectify("bindTexture()")
			.protectify("blendFunc()")
			.protectify("clearBuffers()")
			.protectify("compileShader()")
			.protectify("createDepthBuffer()")
			.protectify("createRenderBuffer()")
			.protectify("createFBO()")
			.protectify("createProgram()")
			.protectify("createTexture()")
			.protectify("deleteRenderBuffer()")
			.protectify("deleteFBO()")
			.protectify("deleteShader()")
			.protectify("blitFBO()")
			.protectify("deleteTexture()")
			.protectify("disposeShaders()")
			.protectify("finish()")
			.protectify("genAndBindTexture()")
			.protectify("getBoundFBO()")
			.protectify("getNativeCtxInfo()")
			.protectify("getNativeHandle()")
			.protectify("setBoundTexture()")
			.protectify("getIntParam()")
			.protectify("getMaxSampleSize()")
			.protectify("getUniformLocation()")
			.protectify("isShaderCompilerSupported()")
			.protectify("makeCurrent()")
			.protectify("pixelStorei()")
			.protectify("readPixels()")
			.protectify("scissorTest()")
			.protectify("setShaderProgram()")
			.protectify("texParamsMinMax()")
			.protectify("texImage2D()")
			.protectify("texSubImage2D()")
			.protectify("updateViewportAndDepthTest()")
			.protectify("updateMSAAState()")
			.protectify("updateFilterState()")
			.protectify("updateWrapState()")
			.protectify("uniform1f()")
			.protectify("uniform2f()")
			.protectify("uniform3f()")
			.protectify("uniform4f()")
			.protectify("uniform4fv()")
			.protectify("uniform1i()")
			.protectify("uniform2i()")
			.protectify("uniform3i()")
			.protectify("uniform4i()")
			.protectify("uniform4iv()")
			.protectify("uniformMatrix4fv()")
			.protectify("enableVertexAttributes()")
			.protectify("disableVertexAttributes()")
			.protectify("drawIndexedQuads()")
			.protectify("createIndexBuffer16()")
			.protectify("setIndexBuffer()")
			.protectify("setDeviceParametersFor2D()")
			.protectify("setDeviceParametersFor3D()")
			.protectify("createES2Mesh()")
			.protectify("releaseES2Mesh()")
			.protectify("buildNativeGeometry()")
			.protectify("createES2PhongMaterial()")
			.protectify("releaseES2PhongMaterial()")
			.protectify("setSolidColor()")
			.protectify("setMap()")
			.protectify("createES2MeshView()")
			.protectify("releaseES2MeshView()")
			.protectify("setCullingMode()")
			.protectify("setMaterial()")
			.protectify("setWireframe()")
			.protectify("setAmbientLight()")
			.protectify("setPointLight()")
			.protectify("renderMeshView()")
			
			.tweak();
		
		new AccessTweaker("com.sun.javafx.tk.Toolkit")
			.protectify("TOOLKIT")
			.protectify("fxUserThread")
			.tweak();
		
		new AccessTweaker("com.sun.javafx.tk.quantum.ViewScene")
			.publify()
			.publify("repaint()")
			.tweak();
		
		new AccessTweaker("com.sun.javafx.tk.quantum.QuantumToolkit")
			.unfinal()
			.protectify("isVsyncEnabled()")
			.protectify("postPulse()")
			.protectify("shouldWaitForRenderingToComplete()")
			.tweak();
		
		new AccessTweaker("com.sun.javafx.tk.quantum.QuantumRenderer")
			.unfinal()
			.publify()
			.protectify("instanceReference")
			.protectify("<init>()")
			.protectify("submitRenderJob()")
			.protectify("checkRendererIdle()")
			.tweak();
		
		new AccessTweaker("com.sun.javafx.tk.quantum.PaintCollector")
			.unfinal()
			.publify()
			.protectify("<init>()")
			.protectify("collector")
			.protectify("hasDirty()")
			.unfinal("hasDirty()")
			.protectify("renderAll()")
			.unfinal("renderAll()")
			.protectify("waitForRenderingToComplete()")
			.tweak();
		
		new AccessTweaker("com.sun.glass.ui.PlatformFactory")
			.publify("instance")
			.tweak();
		
		new AccessTweaker("com.sun.glass.ui.Screen")
			.publify("<init>()")
			.tweak();
	}
}
