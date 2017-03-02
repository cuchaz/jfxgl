package cuchaz.jfxgl.demo;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import com.sun.prism.es2.JFXGLContext;

public class TriangleRenderer {
	
	public class Shader {
		
		private final int id;
		private final int vertexId;
		private final int fragmentId;
		
		public final Matrix4f model;
		public final Matrix4f view;
		public final Matrix4f projection;
		
		public final int posLoc;
		public final int colorLoc;
		private final int modelLoc;
		private final int viewLoc;
		private final int projectionLoc;
		
		public Shader() {
			
			// make the shader
			vertexId = context.compileShader(Shader.class.getResource("triangle.vertex.glsl"), true);
			fragmentId = context.compileShader(Shader.class.getResource("triangle.fragment.glsl"), false);
			posLoc = 0;
			colorLoc = 1;
			id = context.createProgram(
				vertexId,
				new int[] { fragmentId },
				new String[] { "inPos", "inColor" },
				new int[] { posLoc, colorLoc }
			);
			
			model = new Matrix4f();
			model.identity();
			view = new Matrix4f();
			view.identity();
			projection = new Matrix4f();
			float extra = 0.1f;
			projection.setOrtho(
				-0.5f - extra, 0.5f + extra,
				0f - extra, 1f + extra,
				-1f, 1f
			);
			
			modelLoc = context.getUniformLocation(id, "model");
			viewLoc = context.getUniformLocation(id, "view");
			projectionLoc = context.getUniformLocation(id, "projection");
			
			// upload uniforms
			try (MemoryStack m = MemoryStack.stackPush()) {
				bind();
				FloatBuffer buf = m.mallocFloat(16);
				model.get(buf);
				GL20.glUniformMatrix4fv(modelLoc, false, buf);
				view.get(buf);
				GL20.glUniformMatrix4fv(viewLoc, false, buf);
				projection.get(buf);
				GL20.glUniformMatrix4fv(projectionLoc, false, buf);
			}
		}
		
		public void bind() {
			context.setShaderProgram(id);
		}
		
		public void setRotation(float radians) {
			
			model.setRotationXYZ(0, radians, 0);
	
			// upload
			try (MemoryStack m = MemoryStack.stackPush()) {
				FloatBuffer buf = m.mallocFloat(16);
				model.get(buf);
				GL20.glUniformMatrix4fv(modelLoc, false, buf);
			}
		}
		
		public void cleanup() {
			context.deleteShader(vertexId);
			context.deleteShader(fragmentId);
			context.deleteProgram(id);
		}
	}
	
	private final JFXGLContext context;
	private final Shader shader;
	private final int vaoId;
	private final int vboId;
	private final int iboId;
	
	public TriangleRenderer(JFXGLContext context) {
		
		this.context = context;
		
		// init the shader
		shader = new Shader();
		
		// make the vertex array
		vaoId = GL30.glGenVertexArrays();
		GL30.glBindVertexArray(vaoId);
		
		try (MemoryStack m = MemoryStack.stackPush()) {
			
			// make the indices
			ByteBuffer indexBuf = m.bytes(new byte[] {
				0, 1, 2,
				0, 2, 3
			});
			iboId = GL15.glGenBuffers();
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, iboId);
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, indexBuf, GL15.GL_STATIC_DRAW);
			GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, iboId);
			
			// make the vertices
			FloatBuffer vertexBuf = m.floats(new float[] {
				-0.5f, 0f, 0f,  1f, 0f, 0f,
				 0.5f, 0f, 0f,  0f, 1f, 0f,
				   0f, 1f, 0f,  0f, 0f, 1f
			});
			vboId = GL15.glGenBuffers();
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuf, GL15.GL_STATIC_DRAW);
			GL20.glEnableVertexAttribArray(0);
			GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, Float.BYTES*(3+3), 0);
			GL20.glEnableVertexAttribArray(1);
			GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, Float.BYTES*(3+3), Float.BYTES*3);
		}
	
		// unbind things
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL30.glBindVertexArray(0);
	}

	public void render(float rotationRadians) {
		
		// update the shader
		shader.bind();
		shader.setRotation(rotationRadians);
		
		// bind stuff
		GL30.glBindVertexArray(vaoId);
		
		// draw it!
		GL11.glDrawElements(GL11.GL_TRIANGLES, 3, GL11.GL_UNSIGNED_BYTE, 0);
		
		// unbind things
		GL30.glBindVertexArray(0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
	}
	
	public void cleanup() {
		GL30.glDeleteVertexArrays(vaoId);
		GL15.glDeleteBuffers(vboId);
		GL15.glDeleteBuffers(iboId);
	}
}
