package com.sun.prism.es2;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class TexturedQuad {
	
	public static class Shader {
		
		private JFXGLContext context;
		private int id = 0;
		private int vertexId = 0;
		private int fragmentId = 0;
		private int viewSizeLoc = 0;
		
		public Shader(JFXGLContext context) {
			
			this.context = context;
			
			// make the shader
			vertexId = context.compileShader(Shader.class.getResource("vertex.glsl"), true);
			fragmentId = context.compileShader(Shader.class.getResource("fragment.glsl"), false);
			id = context.createProgram(
				vertexId,
				new int[] { fragmentId },
				new String[] { "inPos", "inTexCoord" },
				new int[] { 0, 1 }
			);
			
			viewSizeLoc = context.getUniformLocation(id, "viewSize");
		}
		
		public void setViewSize(int width, int height) {
			context.uniform2f(viewSizeLoc, width, height);
		}
		
		public void bind() {
			context.setShaderProgram(id);
		}
		
		public void cleanup() {
			if (vertexId != 0) {
				context.deleteShader(vertexId);
				vertexId = 0;
			}
			if (fragmentId != 0) {
				context.deleteShader(fragmentId);
				fragmentId = 0;
			}
			if (id != 0) {
				context.deleteProgram(id);
				id = 0;
			}
		}
	}
	
	private Shader shader = null;
	private int vaoId = 0;
	private int vboId = 0;
	private int tboId = 0;
	private int iboId = 0;
	private int texId = 0;
	private ByteBuffer indexBuf = null;
	private FloatBuffer vertexBuf = null;
	private FloatBuffer texCoordBuf = null;
	
	public TexturedQuad(int x, int y, int w, int h, int texId, Shader shader) {
		
		this.shader = shader;
		this.texId = texId;
		
		// make the vertex array
		vaoId = GL30.glGenVertexArrays();
		GL30.glBindVertexArray(vaoId);
	
		// make the indices
		indexBuf = BufferUtils.createByteBuffer(6);
		indexBuf.put(new byte[] {
			0, 1, 2,
			0, 2, 3
		});
		indexBuf.flip();
		iboId = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, iboId);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, indexBuf, GL15.GL_STATIC_DRAW);
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, iboId);
		
		// make the vertices
		vertexBuf = BufferUtils.createFloatBuffer(8);
		vertexBuf.put(new float[] {
			x + 0, y + 0,
			x + w, y + 0,
			x + w, y + h,
			x + 0, y + h
		});
		vertexBuf.flip();
		vboId = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuf, GL15.GL_STATIC_DRAW);
		GL20.glEnableVertexAttribArray(0);
		GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 0, 0);
		
		// make the tex coords
		texCoordBuf = BufferUtils.createFloatBuffer(8);
		texCoordBuf.put(new float[] {
			0, 0,
			1, 0,
			1, 1,
			0, 1
		});
		texCoordBuf.flip();
		tboId = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tboId);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, texCoordBuf, GL15.GL_STATIC_DRAW);
		GL20.glEnableVertexAttribArray(1);
		GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 0, 0);
		
		// unbind things
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL30.glBindVertexArray(0);
	}
	
	public void render() {
		
		// bind stuff
		shader.bind();
		GL30.glBindVertexArray(vaoId);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
		
		// draw it!
		GL11.glDrawElements(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_BYTE, 0);
		
		// unbind things
		GL30.glBindVertexArray(0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
	}
	
	public void cleanup() {
		if (vaoId != 0) {
			GL30.glDeleteVertexArrays(vaoId);
			vaoId = 0;
		}
		if (vboId != 0) {
			GL15.glDeleteBuffers(vboId);
			vboId = 0;
		}
		if (tboId != 0) {
			GL15.glDeleteBuffers(tboId);
			tboId = 0;
		}
		if (iboId != 0) {
			GL15.glDeleteBuffers(iboId);
			iboId = 0;
		}
		// NOTE: don't cleanup the shader
	}
}
