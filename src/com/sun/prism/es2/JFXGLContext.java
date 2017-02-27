package com.sun.prism.es2;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL41;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;

import com.sun.prism.Texture.WrapMode;
import com.sun.prism.paint.Color;

public class JFXGLContext extends GLContext {
	
	private long hwnd;
	private GLCapabilities caps;
	
	public JFXGLContext(long hwnd) {
		
		if (hwnd <= 0) {
			throw new IllegalArgumentException("hwnd is invalid");
		}
		
		this.hwnd = hwnd;
		this.caps = GL.createCapabilities();
	}

	@Override
	public long getNativeHandle() {
		return hwnd;
	}
	
	@Override
	public long getNativeCtxInfo() {
		return hwnd;
	}

	@Override
	public void makeCurrent(GLDrawable drawable) {
		// this thread/window should already be current, so there's nothing left to do
	}
	
	public boolean isExtensionSupported(String sglExtStr) {
		try {
			
			// sadly, LWJGL has no string-based lookup, so use reflection
			return caps.getClass().getDeclaredField(sglExtStr).getBoolean(caps);
			
		} catch (NoSuchFieldException ex) {
			return false;
		} catch (IllegalAccessException ex) {
			throw new RuntimeException("can't check extension", ex);
		}
	}
	
	@Override
	public void activeTexture(int texUnit) {
		GL13.glActiveTexture(GL13.GL_TEXTURE0 + texUnit);
	}

	@Override
	public void bindFBO(int nativeFBOID) {
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, nativeFBOID);
	}

	@Override
	public void bindTexture(int texID) {
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texID);
	}

	@Override
	public void blendFunc(int sFactor, int dFactor) {
		GL11.glBlendFunc(translateScaleFactor(sFactor), translateScaleFactor(dFactor));
	}
	
	private Color clearColor = null;

	@Override
	public void clearBuffers(Color color, boolean clearColor, boolean clearDepth, boolean ignoreScissor) {
		
		// temporarily disable the scissor test if needed
		if (ignoreScissor && this.scissorEnabled) {
			GL11.glDisable(GL11.GL_SCISSOR_TEST);
		}

		int clearFlags = 0;

		if (clearColor) {
			clearFlags |= GL11.GL_COLOR_BUFFER_BIT;
			if (this.clearColor == null || !this.clearColor.equals(color)) {
				this.clearColor = color;
				GL11.glClearColor(this.clearColor.getRedPremult(), this.clearColor.getGreenPremult(),
						this.clearColor.getBluePremult(), this.clearColor.getAlpha());
			}
		}

		if (clearDepth) {
			clearFlags |= GL11.GL_DEPTH_BUFFER_BIT;
			
			// temporarily enable the depth dest if needed
			if (!this.depthTestEnabled) {
				GL11.glDepthMask(true);
			}
			
			GL11.glClear(clearFlags);
			
			// restore depth test if neeed
			if (!this.depthTestEnabled) {
				GL11.glDepthMask(false);
			}
			
		} else {
			GL11.glClear(clearFlags);
		}

		// restore the scissor test if needed
		if (ignoreScissor && this.scissorEnabled) {
			GL11.glEnable(GL11.GL_SCISSOR_TEST);
		}
	}

	@Override
	public int compileShader(String source, boolean isVertex) {
		
		int type;
		if (isVertex) {
			type = GL20.GL_VERTEX_SHADER;
		} else {
			type = GL20.GL_FRAGMENT_SHADER;
		}
		
		int id = GL20.glCreateShader(type);
		GL20.glShaderSource(id, source);
		GL20.glCompileShader(id);
		
		boolean isSuccess = GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) != GL11.GL_FALSE;
		if (!isSuccess) {
			
			// get debug info
			StringBuilder buf = new StringBuilder();
			buf.append("Shader did not compile\n");
			
			// show the compiler log
			buf.append("\nCOMPILER LOG:\n");
			buf.append(GL20.glGetShaderInfoLog(id, 4096));
			
			// show the source with correct line numbering
			buf.append("\nSOURCE:\n");
			String[] lines = source.split("\\n");
			for (int i=0; i<lines.length; i++) {
				buf.append(String.format("%4d: ", i + 1));
				buf.append(lines[i]);
				buf.append("\n");
			}
			
			throw new RuntimeException(buf.toString());
		}
		
		return id;
	}

	@Override
	public int createDepthBuffer(int width, int height, int msaaSamples) {
		throw new UnsupportedOperationException("IMPLEMENT ME!");
	}

	@Override
	public int createRenderBuffer(int width, int height, int msaaSamples) {
		throw new UnsupportedOperationException("IMPLEMENT ME!");
	}

	@Override
	public int createFBO(int texId) {
		
		int id = GL30.glGenFramebuffers();
		bindFBO(id);

		if (texId != 0) {

			GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, texId, 0);

			// check for FBO errors
			String err = getFramebufferError();
			if (err != null) {
				System.err.println("Error creating framebuffer object with texture " + texId + ": " + err);
				return 0;
			}

			// remove garbage from newly-allocated buffers
			clearBuffers(Color.BLACK, true, false, true);
		}

		return id;
	}

	@Override
	public int createProgram(int vertexShaderId, int[] fragmentShaderIds, String[] attrs, int[] indices) {
		
		// build the shader program
		int id = GL20.glCreateProgram();
		GL20.glAttachShader(id, vertexShaderId);
		for (int fragmentShaderId : fragmentShaderIds) {
			GL20.glAttachShader(id, fragmentShaderId);
		}
		
		assert (attrs.length == indices.length);
		for (int i=0; i<attrs.length; i++) {
			GL20.glBindAttribLocation(id, indices[i], attrs[i]);
		}
		
		GL20.glLinkProgram(id);
		boolean isSuccess = GL20.glGetProgrami(id, GL20.GL_LINK_STATUS) == GL11.GL_TRUE;
		if (!isSuccess) {
			throw new RuntimeException("Shader program did not link:\n" + GL20.glGetProgramInfoLog(id, 4096));
		}
		GL20.glValidateProgram(id);
		isSuccess = GL20.glGetProgrami(id, GL20.GL_VALIDATE_STATUS) == GL11.GL_TRUE;
		if (!isSuccess) {
			throw new RuntimeException("Shader program did not validate:\n" + GL20.glGetProgramInfoLog(id, 4096));
		}
		
		return id;
	}

	@Override
	public int createTexture(int width, int height) {
		
		int texId = GL11.glGenTextures();
		if (texId == 0) {
			return 0;
		}

		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);

		// make the texture
		clearGLErrors();
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, 0);

		// if something bad happened, delete the texture
		if (hasGLError()) {
			GL11.glDeleteTextures(texId);
			return 0;
		}
		
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		
		return texId;
	}

	@Override
	public void deleteRenderBuffer(int dbID) {
		throw new UnsupportedOperationException("IMPLEMENT ME!");
	}

	@Override
	public void deleteFBO(int fboId) {
		GL30.glDeleteFramebuffers(fboId);
	}

	@Override
	public void deleteShader(int shadeID) {
		throw new UnsupportedOperationException("IMPLEMENT ME!");
	}

	@Override
	public void blitFBO(int srcFboId, int dstFboId, int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1) {
		GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, srcFboId);
		GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, dstFboId);
		GL30.glBlitFramebuffer(
			srcX0, srcY0,
			srcX1, srcY1,
			dstX0, dstY0,
			dstX1, dstY1,
			GL11.GL_COLOR_BUFFER_BIT,
			GL11.GL_NEAREST
		);
	}

	@Override
	public void deleteTexture(int texId) {
		GL11.glDeleteTextures(texId);
	}

	@Override
	public void disposeShaders(int pID, int vID, int[] fID) {
		throw new UnsupportedOperationException("IMPLEMENT ME!");
	}

	@Override
	public void finish() {
		throw new UnsupportedOperationException("IMPLEMENT ME!");
	}

	@Override
	public int genAndBindTexture() {
		int texId = GL11.glGenTextures();
		setBoundTexture(texId);
		return texId;
	}

	@Override
	public int getBoundFBO() {
		return GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
	}

	@Override
	public int getIntParam(int param) {
		return GL11.glGetInteger(translatePrismToGL(param));
	}

	@Override
	public int getMaxSampleSize() {
		throw new UnsupportedOperationException("IMPLEMENT ME!");
	}

	public int getUniformLocation(int programID, String name) {
		return GL20.glGetUniformLocation(programID, name);
	}

	@Override
	public boolean isShaderCompilerSupported() {
		return true;
	}

	@Override
	public void pixelStorei(int pname, int param) {
		GL11.glPixelStorei(translatePixelStoreName(pname), param);
	}

	@Override
	public boolean readPixels(Buffer buffer, int x, int y, int w, int h) {
		throw new UnsupportedOperationException("IMPLEMENT ME!");
	}

	private boolean scissorEnabled = false;
	
	@Override
	public void scissorTest(boolean enable, int x, int y, int w, int h) {
		if (enable) {
			if (!this.scissorEnabled) {
				GL11.glEnable(GL11.GL_SCISSOR_TEST);
				this.scissorEnabled = true;
			}
			GL11.glScissor(x, y, w, h);
		} else if (this.scissorEnabled) {
			GL11.glDisable(GL11.GL_SCISSOR_TEST);
			this.scissorEnabled = false;
		}
	}

	@Override
	public void setShaderProgram(int progid) {
		GL20.glUseProgram(progid);
	}

	@Override
	public void texParamsMinMax(int pname, boolean useMipmap) {
		
		int min = pname;
		int max = pname;

		if (useMipmap) {
			if (min == GLContext.GL_LINEAR) {
				min = GLContext.GL_LINEAR_MIPMAP_LINEAR;
			} else {
				min = GLContext.GL_NEAREST_MIPMAP_NEAREST;
			}
		}
		
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, translatePrismToGL(max));
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, translatePrismToGL(min));
	}
	
	private ByteBuffer imageBuf = null;

	@Override
	public boolean texImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, Buffer pixels, boolean useMipmap) {
		
		// NOTE: null pixels are allowed
		
		if (pixels != null && !(pixels instanceof ByteBuffer)) {
			throw new Error("buffer should be a " + ByteBuffer.class.getName() + ", not a " + pixels.getClass().getName());
		}
		ByteBuffer buf = (ByteBuffer)pixels;
		
		if (useMipmap) {
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_GENERATE_MIPMAP, GL11.GL_TRUE);
		}
		
		// if buf is not direct, copy it to a direct buffer
		if (buf != null && !buf.isDirect()) {
			imageBuf = updateBuffer(imageBuf, buf);
			buf = imageBuf;
		}
		
		clearGLErrors();
		GL11.glTexImage2D(
			translatePrismToGL(target),
			level,
			translatePrismToGL(internalFormat),
			width,
			height,
			border,
			translatePrismToGL(format),
			translatePrismToGL(type),
			buf
		);
		return !hasGLError();
	}

	@Override
	public void texSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, java.nio.Buffer pixels) {
		
		// NOTE: null pixels are allowed
		
		if (pixels != null && !(pixels instanceof ByteBuffer)) {
			throw new Error("buffer should be a " + ByteBuffer.class.getName() + ", not a " + pixels.getClass().getName());
		}
		ByteBuffer buf = (ByteBuffer)pixels;
		
		// if buf is not direct, copy it to a direct buffer
		if (buf != null && !buf.isDirect()) {
			imageBuf = updateBuffer(imageBuf, buf);
			buf = imageBuf;
		}

		GL11.glTexSubImage2D(
			translatePrismToGL(target),
			level,
			xoffset,
			yoffset,
			width,
			height,
			translatePrismToGL(format),
			translatePrismToGL(type),
			buf
		);
	}
	
	private boolean depthTestEnabled = false;

	@Override
	public void updateViewportAndDepthTest(int x, int y, int w, int h, boolean depthTest) {
		GL11.glViewport(x, y, w, h);
		
		if (this.depthTestEnabled != depthTest) {
			if (depthTest) {
				GL11.glEnable(GL11.GL_DEPTH_TEST);
				GL11.glDepthFunc(GL11.GL_LEQUAL);
				GL11.glDepthMask(true);
			} else {
				GL11.glDisable(GL11.GL_DEPTH_TEST);
				GL11.glDepthMask(false);
			}
			depthTestEnabled = depthTest;
		}
	}

	@Override
	public void updateMSAAState(boolean msaa) {
		if (msaa) {
			GL11.glEnable(GL13.GL_MULTISAMPLE);
		} else {
			GL11.glDisable(GL13.GL_MULTISAMPLE);
		}
	}

	@Override
	public void updateFilterState(int texID, boolean linearFilter) {
		if (linearFilter) {
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		} else {
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		}
	}

	@Override
	public void updateWrapState(int texID, WrapMode wrapMode) {
		int wm;
		switch (wrapMode) {
			case REPEAT_SIMULATED: // mode should not matter for this case
			case REPEAT:
				wm = WRAPMODE_REPEAT;
				break;
			case CLAMP_TO_ZERO_SIMULATED:
			case CLAMP_TO_EDGE_SIMULATED: // needed for top/left edge cases
			case CLAMP_TO_EDGE:
				wm = WRAPMODE_CLAMP_TO_EDGE;
				break;
			case CLAMP_TO_ZERO:
				wm = WRAPMODE_CLAMP_TO_BORDER;
				break;
			case CLAMP_NOT_NEEDED:
				return;
			default:
				throw new Error("Unrecognized wrap mode: " + wrapMode);
		}
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, translatePrismToGL(wm));
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, translatePrismToGL(wm));
	}

	@Override
	public void uniform1f(int location, float v0) {
		GL20.glUniform1f(location, v0);
	}

	@Override
	public void uniform2f(int location, float v0, float v1) {
		GL20.glUniform2f(location, v0, v1);
	}

	@Override
	public void uniform3f(int location, float v0, float v1, float v2) {
		GL20.glUniform3f(location, v0, v1, v2);
	}

	@Override
	public void uniform4f(int location, float v0, float v1, float v2, float v3) {
		GL20.glUniform4f(location, v0, v1, v2, v3);
	}

	@Override
	public void uniform4fv(int location, int count, FloatBuffer val) {
		GL20.glUniform4fv(location, val);
	}

	@Override
	public void uniform1i(int location, int v0) {
		GL20.glUniform1i(location, v0);
	}

	@Override
	public void uniform2i(int location, int v0, int v1) {
		GL20.glUniform2i(location, v0, v1);
	}

	@Override
	public void uniform3i(int location, int v0, int v1, int v2) {
		GL20.glUniform3i(location, v0, v1, v2);
	}

	@Override
	public void uniform4i(int location, int v0, int v1, int v2, int v3) {
		GL20.glUniform4i(location, v0, v1, v2, v3);
	}

	@Override
	public void uniform4iv(int location, int count, IntBuffer val) {
		GL20.glUniform4iv(location, val);
	}

	@Override
	public void uniformMatrix4fv(int location, boolean transpose, float values[]) {
		GL20.glUniformMatrix4fv(location, transpose, values);
	}

	@Override
	public void enableVertexAttributes() {
		// JavaFX apparently uses attributes in [0,3]
		for (int i=0; i<4; i++) {
			GL20.glEnableVertexAttribArray(i);
		}
	}

	@Override
	public void disableVertexAttributes() {
		for (int i=0; i<4; i++) {
			GL20.glDisableVertexAttribArray(i);
		}
	}

	private static final int PosCoordFloats = 3;
	private static final int TexCoordFloats = 2;
	private static final int PosTexBytes = (PosCoordFloats + TexCoordFloats*2)*Float.BYTES;
	private static final int PosOffsetBytes = 0;
	private static final int Tex0OffsetBytes = PosOffsetBytes + PosCoordFloats*Float.BYTES;
	private static final int Tex1OffsetBytes = Tex0OffsetBytes + TexCoordFloats*Float.BYTES;
	private static final int ColorBytes = 4;
	
	private ByteBuffer indexedQuadsCoordsBuf = null;
	private ByteBuffer indexedQuadsColorsBuf = null;
	
	@Override
	public void drawIndexedQuads(float coords[], byte colors[], int numVertices) {
		
		int numQuads = numVertices/4;
		int numIndices = numQuads*2*3;
		
		// for some reason (compatibility maybe?) JavaFX keeps all its data on the JVM heap (eg, in arrays)
		// sadly, LWJGL won't let use attribute offsets on heap buffers, so we need to copy into direct buffers
		// hopefully we're not drawing that many quads, and this won't be too slow
		
		// pos,tex coords
		indexedQuadsCoordsBuf = updateBuffer(indexedQuadsCoordsBuf, coords, numVertices*PosTexBytes);
		GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, PosTexBytes, MemoryUtil.memAddress(indexedQuadsCoordsBuf) +  PosOffsetBytes);
		// index 1 is color, handled below
		GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, PosTexBytes, MemoryUtil.memAddress(indexedQuadsCoordsBuf) + Tex0OffsetBytes);
		GL20.glVertexAttribPointer(3, 2, GL11.GL_FLOAT, false, PosTexBytes, MemoryUtil.memAddress(indexedQuadsCoordsBuf) + Tex1OffsetBytes);
		
		// colors
		indexedQuadsColorsBuf = updateBuffer(indexedQuadsColorsBuf, colors, numVertices*ColorBytes);
		GL20.glVertexAttribPointer(1, 4, GL11.GL_UNSIGNED_BYTE, true, ColorBytes, indexedQuadsColorsBuf);
		
		GL11.glDrawElements(GL11.GL_TRIANGLES, numIndices, GL11.GL_UNSIGNED_SHORT, 0);
	}

	@Override
	public int createIndexBuffer16(short data[]) {
		int id = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, id);
		GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, data, GL15.GL_STATIC_DRAW);
		return id;
	}

	@Override
	public void setIndexBuffer(int ib) {
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ib);
	}

	@Override
	public void setDeviceParametersFor2D() {
		throw new UnsupportedOperationException("IMPLEMENT ME!");
	}

	@Override
	public void setDeviceParametersFor3D() {
		throw new UnsupportedOperationException("IMPLEMENT ME!");
	}

	@Override
	public long createES2Mesh() {
		throw new UnsupportedOperationException("IMPLEMENT ME!");
	}

	@Override
	public void releaseES2Mesh(long nativeHandle) {
		throw new UnsupportedOperationException("IMPLEMENT ME!");
	}

	@Override
	public boolean buildNativeGeometry(long nativeHandle, float[] vertexBuffer, int vertexBufferLength, short[] indexBuffer, int indexBufferLength) {
		throw new UnsupportedOperationException("IMPLEMENT ME!");
	}

	@Override
	public boolean buildNativeGeometry(long nativeHandle, float[] vertexBuffer, int vertexBufferLength, int[] indexBuffer, int indexBufferLength) {
		throw new UnsupportedOperationException("IMPLEMENT ME!");
	}

	@Override
	public long createES2PhongMaterial() {
		throw new UnsupportedOperationException("IMPLEMENT ME!");
	}

	@Override
	public void releaseES2PhongMaterial(long nativeHandle) {
		throw new UnsupportedOperationException("IMPLEMENT ME!");
	}

	@Override
	public void setSolidColor(long nativePhongMaterial, float r, float g, float b, float a) {
		throw new UnsupportedOperationException("IMPLEMENT ME!");
	}

	@Override
	public void setMap(long nativePhongMaterial, int mapType, int texID) {
		throw new UnsupportedOperationException("IMPLEMENT ME!");
	}

	@Override
	public long createES2MeshView(long nativeMeshInfo) {
		throw new UnsupportedOperationException("IMPLEMENT ME!");
	}

	@Override
	public void releaseES2MeshView(long nativeHandle) {
		throw new UnsupportedOperationException("IMPLEMENT ME!");
	}

	@Override
	public void setCullingMode(long nativeMeshViewInfo, int cullMode) {
		throw new UnsupportedOperationException("IMPLEMENT ME!");
	}

	@Override
	public void setMaterial(long nativeMeshViewInfo, long nativePhongMaterialInfo) {
		throw new UnsupportedOperationException("IMPLEMENT ME!");
	}

	@Override
	public void setWireframe(long nativeMeshViewInfo, boolean wireframe) {
		throw new UnsupportedOperationException("IMPLEMENT ME!");
	}

	@Override
	public void setAmbientLight(long nativeMeshViewInfo, float r, float g, float b) {
		throw new UnsupportedOperationException("IMPLEMENT ME!");
	}

	@Override
	public void setPointLight(long nativeMeshViewInfo, int index, float x, float y, float z, float r, float g, float b, float w) {
		throw new UnsupportedOperationException("IMPLEMENT ME!");
	}

	@Override
	public void renderMeshView(long nativeMeshViewInfo) {
		throw new UnsupportedOperationException("IMPLEMENT ME!");
	}
	
	private static int translatePrismToGL(int value) {
		switch (value) {
			case GLContext.GL_FLOAT:
				return GL11.GL_FLOAT;
			case GLContext.GL_UNSIGNED_BYTE:
				return GL11.GL_UNSIGNED_BYTE;
			case GLContext.GL_UNSIGNED_INT_8_8_8_8_REV:
				return GL12.GL_UNSIGNED_INT_8_8_8_8_REV;
			case GLContext.GL_UNSIGNED_INT_8_8_8_8:
				return GL12.GL_UNSIGNED_INT_8_8_8_8;
			case GLContext.GL_UNSIGNED_SHORT_8_8_APPLE:
				return 0x85BA;

			case GLContext.GL_RGBA:
				return GL11.GL_RGBA;
			case GLContext.GL_BGRA:
				return GL12.GL_BGRA;
			case GLContext.GL_RGB:
				return GL11.GL_RGB;
			case GLContext.GL_LUMINANCE:
				return GL11.GL_LUMINANCE;
			case GLContext.GL_ALPHA:
				return GL11.GL_ALPHA;
			case GLContext.GL_RGBA32F:
				return GL30.GL_RGBA32F;
			case GLContext.GL_YCBCR_422_APPLE:
				return 0x85B9;

			case GLContext.GL_TEXTURE_2D:
				return GL11.GL_TEXTURE_2D;
			case GLContext.GL_TEXTURE_BINDING_2D:
				return GL11.GL_TEXTURE_BINDING_2D;
			case GLContext.GL_NEAREST:
				return GL11.GL_NEAREST;
			case GLContext.GL_LINEAR:
				return GL11.GL_LINEAR;
			case GLContext.GL_NEAREST_MIPMAP_NEAREST:
				return GL11.GL_NEAREST_MIPMAP_NEAREST;
			case GLContext.GL_LINEAR_MIPMAP_LINEAR:
				return GL11.GL_LINEAR_MIPMAP_LINEAR;

			case GLContext.WRAPMODE_REPEAT:
				return GL11.GL_REPEAT;
			case GLContext.WRAPMODE_CLAMP_TO_EDGE:
				return GL12.GL_CLAMP_TO_EDGE;
			case GLContext.WRAPMODE_CLAMP_TO_BORDER:
				return GL13.GL_CLAMP_TO_BORDER;

			case GLContext.GL_MAX_FRAGMENT_UNIFORM_COMPONENTS:
				return GL20.GL_MAX_FRAGMENT_UNIFORM_COMPONENTS;
			case GLContext.GL_MAX_FRAGMENT_UNIFORM_VECTORS:
				return GL41.GL_MAX_FRAGMENT_UNIFORM_VECTORS;
			case GLContext.GL_MAX_TEXTURE_IMAGE_UNITS:
				return GL20.GL_MAX_TEXTURE_IMAGE_UNITS;
			case GLContext.GL_MAX_TEXTURE_SIZE:
				return GL11.GL_MAX_TEXTURE_SIZE;
			case GLContext.GL_MAX_VARYING_COMPONENTS:
				return GL30.GL_MAX_VARYING_COMPONENTS;
			case GLContext.GL_MAX_VARYING_VECTORS:
				return GL41.GL_MAX_VARYING_VECTORS;
			case GLContext.GL_MAX_VERTEX_ATTRIBS:
				return GL20.GL_MAX_VERTEX_ATTRIBS;
			case GLContext.GL_MAX_VERTEX_UNIFORM_COMPONENTS:
				return GL20.GL_MAX_VERTEX_UNIFORM_COMPONENTS;
			case GLContext.GL_MAX_VERTEX_UNIFORM_VECTORS:
				return GL41.GL_MAX_VERTEX_UNIFORM_VECTORS;
			case GLContext.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS:
				return GL20.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS;

			default:
				// don't know how to translate, just hope for the best
				return value;
		}
	}
	
	private static int translateScaleFactor(int scaleFactor) {
		switch (scaleFactor) {
			case GLContext.GL_ZERO:
				return GL11.GL_ZERO;
			case GLContext.GL_ONE:
				return GL11.GL_ONE;
			case GLContext.GL_SRC_COLOR:
				return GL11.GL_SRC_COLOR;
			case GLContext.GL_ONE_MINUS_SRC_COLOR:
				return GL11.GL_ONE_MINUS_SRC_COLOR;
			case GLContext.GL_DST_COLOR:
				return GL11.GL_DST_COLOR;
			case GLContext.GL_ONE_MINUS_DST_COLOR:
				return GL11.GL_ONE_MINUS_DST_COLOR;
			case GLContext.GL_SRC_ALPHA:
				return GL11.GL_SRC_ALPHA;
			case GLContext.GL_ONE_MINUS_SRC_ALPHA:
				return GL11.GL_ONE_MINUS_SRC_ALPHA;
			case GLContext.GL_DST_ALPHA:
				return GL11.GL_DST_ALPHA;
			case GLContext.GL_ONE_MINUS_DST_ALPHA:
				return GL11.GL_ONE_MINUS_DST_ALPHA;
			case GLContext.GL_CONSTANT_COLOR:
				return GL14.GL_CONSTANT_COLOR;
			case GLContext.GL_ONE_MINUS_CONSTANT_COLOR:
				return GL14.GL_ONE_MINUS_CONSTANT_COLOR;
			case GLContext.GL_CONSTANT_ALPHA:
				return GL14.GL_CONSTANT_ALPHA;
			case GLContext.GL_ONE_MINUS_CONSTANT_ALPHA:
				return GL14.GL_ONE_MINUS_CONSTANT_ALPHA;
			case GLContext.GL_SRC_ALPHA_SATURATE:
				return GL11.GL_SRC_ALPHA_SATURATE;
			default:
				// don't know what to do, just return zero
				return GL11.GL_ZERO;
		}
	}
	
	private static String getFramebufferError() {
		int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
		switch(status) {
			case GL30.GL_FRAMEBUFFER_COMPLETE:
				return null;
			case GL30.GL_FRAMEBUFFER_UNSUPPORTED:
				return "GL_FRAMEBUFFER_UNSUPPORTED";
			case GL30.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
				return "GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT";
			case GL30.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
				return "GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT";
			/* doesn't look like we have these constants
			case GL30.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT:
				return "GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT";
			case GL30.GL_FRAMEBUFFER_INCOMPLETE_FORMATS_EXT:
				return "GL_FRAMEBUFFER_INCOMPLETE_FORMATS_EXT";
			*/
			case GL30.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
				return "GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER";
			case GL30.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
				return "GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER";
			case GL30.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE:
				return "GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE";
			default:
				return "(unknown error: " + status + ")";
		}
	}
	
	private static int translatePixelStoreName(int pname) {
		switch (pname) {
			case GLContext.GL_UNPACK_ALIGNMENT:
				return GL11.GL_UNPACK_ALIGNMENT;
			case GLContext.GL_UNPACK_ROW_LENGTH:
				return GL11.GL_UNPACK_ROW_LENGTH;
			case GLContext.GL_UNPACK_SKIP_PIXELS:
				return GL11.GL_UNPACK_SKIP_PIXELS;
			case GLContext.GL_UNPACK_SKIP_ROWS:
				return GL11.GL_UNPACK_SKIP_ROWS;
			default:
				// don't know what to do, just hope for the best
				return pname;
		}
	}
	
	private static void clearGLErrors() {
		while (hasGLError());
	}
	
	private static boolean hasGLError() {
		return getGLError() != GL11.GL_NO_ERROR;
	}
	
	private static int getGLError() {
		return GL11.glGetError();
	}
	
	private static int findPowerOf2(int a) {
		
		if (a == 0) {
			return 0;
		}
		
		int i = 1;
		for (int j=0; j<31; j++) {
			if (i >= a) {
				return i;
			}
			i <<= 1;
		}
		throw new Error("argument too big for 32 bits: " + a);
	}
	
	private static ByteBuffer manageBufferSize(ByteBuffer buf, int numBytesNeeded) {
		
		final int MinBufferBytes = 1024;
		int bufferSize = Math.max(findPowerOf2(numBytesNeeded), MinBufferBytes);
		
		if (buf == null || buf.capacity() < bufferSize) {
			buf = BufferUtils.createByteBuffer(bufferSize);
		}
		return buf;
	}
	
	private static ByteBuffer updateBuffer(ByteBuffer buf, byte[] data, int numBytesNeeded) {
		buf = manageBufferSize(buf, numBytesNeeded);
		buf.clear();
		buf.put(data, 0, numBytesNeeded);
		buf.flip();
		return buf;
	}
	
	private static ByteBuffer updateBuffer(ByteBuffer buf, float[] data, int numBytesNeeded) {
		buf = manageBufferSize(buf, numBytesNeeded);
		buf.clear();
		buf.asFloatBuffer().put(data, 0, numBytesNeeded/Float.BYTES);
		buf.flip();
		return buf;
	}
	
	private static ByteBuffer updateBuffer(ByteBuffer buf, ByteBuffer data) {
		buf = manageBufferSize(buf, data.limit());
		data.rewind();
		buf.clear();
		buf.put(data);
		buf.flip();
		return buf;
	}
}
