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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.List;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

public class GLState {
	
	public static final Part.Flag Blend = new Part.Flag(GL11.GL_BLEND);
	public static final Part.Flag ScissorTest = new Part.Flag(GL11.GL_SCISSOR_TEST);
	
	public static final Part BlendFunc = new Part(
		Integer.BYTES*2,
		(out) -> {
			out.writeInt(GL11.glGetInteger(GL11.GL_BLEND_SRC));
			out.writeInt(GL11.glGetInteger(GL11.GL_BLEND_DST));
		},
		(in) -> {
			GL11.glBlendFunc(in.readInt(), in.readInt());
		}
	);
	
	public static final Part.IntVal ShaderProgram = new Part.IntVal(
		() -> {
			return GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
		},
		(int val) -> {
			GL20.glUseProgram(val);
		}
	);
	
	protected static class Texture2D extends Part.IntVal {
		
		public Texture2D(int index) {
			super(
				() -> {
					GL13.glActiveTexture(GL13.GL_TEXTURE0 + index);
					return GL11.glGetInteger(GL11.GL_TEXTURE_2D);
				},
				(int val) -> {
					GL13.glActiveTexture(GL13.GL_TEXTURE0 + index);
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, val);
				}
			);
		}
	}
	
	/** This should go AFTER ActiveTexture in the GLState list, since it modifies the active texture. */
	public static final Part.IntVal[] Texture2D = {
		new Texture2D(0),
		new Texture2D(1),
		new Texture2D(2),
		new Texture2D(3),
		new Texture2D(4),
		new Texture2D(5),
		new Texture2D(6),
		new Texture2D(7),
		new Texture2D(8),
		new Texture2D(9),
		new Texture2D(10),
		new Texture2D(11),
		new Texture2D(12),
		new Texture2D(13),
		new Texture2D(14),
		new Texture2D(15),
		new Texture2D(16),
		new Texture2D(17),
		new Texture2D(18),
		new Texture2D(19),
		new Texture2D(20),
		new Texture2D(21),
		new Texture2D(22),
		new Texture2D(23),
		new Texture2D(24),
		new Texture2D(25),
		new Texture2D(26),
		new Texture2D(27),
		new Texture2D(28),
		new Texture2D(29),
		new Texture2D(30),
		new Texture2D(31)
	};
	
	public static final Part.IntVal ActiveTexture = new Part.IntVal(
		() -> {
			return GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
		},
		(int val) -> {
			GL13.glActiveTexture(val);
		}
	);
	
	public static final Part.IntVal VertexArray = new Part.IntVal(
		() -> {
			return GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
		},
		(int val) -> {
			GL30.glBindVertexArray(val);
		}
	);
	
	public static final Part.IntVal ArrayBuffer = new Part.IntVal(
		() -> {
			return GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
		},
		(int val) -> {
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, val);
		}
	);
	
	public static final Part.BoolVal DepthMask = new Part.BoolVal(
		() -> {
			return GL11.glGetInteger(GL11.GL_DEPTH_WRITEMASK) == GL11.GL_TRUE;
		},
		(boolean val) -> {
			GL11.glDepthMask(val);
		}
	);
	
	public static final Part ClearColor = new Part.Float4Val(
		(buf) -> {
			GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, buf);
		},
		(r, g, b, a) -> {
			GL11.glClearColor(r, g, b, a);
		}
	);
	
	public static final Part Viewport = new Part.Int4Val(
		(buf) -> {
			GL11.glGetIntegerv(GL11.GL_VIEWPORT, buf);
		},
		(x, y, w, h) -> {
			GL11.glViewport(x, y, w, h);
		}
	);
	

	public static class Part {
		
		public static interface Backuper {
			void backup(DataOutputStream out) throws IOException;
		}
		
		public static interface Restorer {
			void restore(DataInputStream in) throws IOException;
		}
		
		public final int size;
		public final Backuper backuper;
		public final Restorer restorer;
		
		protected Part(int size, Backuper backuper, Restorer restorer) {
			this.size = size;
			this.backuper = backuper;
			this.restorer = restorer;
		}
		
		public static class IntVal extends Part {
			
			public static interface Getter {
				int get();
			}
			
			public static interface Setter {
				void set(int val);
			}
			
			private final Getter getter;
			private final Setter setter;
			
			public IntVal(Getter getter, Setter setter) {
				super(
					Integer.BYTES,
					(out) -> {
						out.writeInt(getter.get());
					},
					(in) -> {
						setter.set(in.readInt());
					}
				);
				this.getter = getter;
				this.setter = setter;
			}
			
			public int get() {
				return getter.get();
			}
			
			public void set(int val) {
				setter.set(val);
			}
		}
		
		public static class BoolVal extends Part {
			
			public static interface Getter {
				boolean get();
			}
			
			public static interface Setter {
				void set(boolean val);
			}
			
			private final Getter getter;
			private final Setter setter;
			
			public BoolVal(Getter getter, Setter setter) {
				super(
					1,
					(out) -> {
						out.writeBoolean(getter.get());
					},
					(in) -> {
						setter.set(in.readBoolean());
					}
				);
				this.getter = getter;
				this.setter = setter;
			}
			
			public boolean get() {
				return getter.get();
			}
			
			public void setter(boolean val) {
				setter.set(val);
			}
		}
		
		public static class Flag extends BoolVal {
			
			public Flag(int flag) {
				super(
					() -> {
						return GL11.glIsEnabled(flag);
					},
					(boolean val) -> {
						if (val) {
							GL11.glEnable(flag);
						} else {
							GL11.glDisable(flag);
						}
					}
				);
			}
		}
		
		public static class Int4Val extends Part {
			
			public static interface Getter {
				void get(IntBuffer buf);
			}
			
			public static interface Setter {
				void set(int a, int b, int c, int d);
			}
			
			public Int4Val(Getter getter, Setter setter) {
				super(
					Integer.BYTES*4,
					(out) -> {
						try (MemoryStack m = MemoryStack.stackPush()) {
							IntBuffer buf = m.ints(0, 0, 0, 0);
							getter.get(buf);
							out.writeInt(buf.get(0));
							out.writeInt(buf.get(1));
							out.writeInt(buf.get(2));
							out.writeInt(buf.get(3));
						}
					},
					(in) -> {
						setter.set(in.readInt(), in.readInt(), in.readInt(), in.readInt());
					}
				);
			}
		}
		
		public static class Float4Val extends Part {
			
			public static interface Getter {
				void get(FloatBuffer buf);
			}
			
			public static interface Setter {
				void set(float a, float b, float c, float d);
			}
			
			public Float4Val(Getter getter, Setter setter) {
				super(
					Float.BYTES*4,
					(out) -> {
						try (MemoryStack m = MemoryStack.stackPush()) {
							FloatBuffer buf = m.floats(0, 0, 0, 0);
							getter.get(buf);
							out.writeFloat(buf.get(0));
							out.writeFloat(buf.get(1));
							out.writeFloat(buf.get(2));
							out.writeFloat(buf.get(3));
						}
					},
					(in) -> {
						setter.set(in.readFloat(), in.readFloat(), in.readFloat(), in.readFloat());
					}
				);
			}
		}
	}
	
	private List<Part> parts;
	private ByteBuffer buf;
	
	public GLState(Part ... parts) {
		this.parts = Arrays.asList(parts);
		
		int size = 0;
		for (Part part : this.parts) {
			size += part.size;
		}
		this.buf = ByteBuffer.allocate(size);
	}
	
	public void backup() {
		
		OutputStream out = new OutputStream() {

			@Override
			public void write(int b)
			throws IOException {
				buf.put((byte)(b & 0xff));
			}
		};
		
		try (DataOutputStream dout = new DataOutputStream(out)) {
			buf.clear();
			
			// iterate in forward order for backup
			for (int i=0; i<parts.size(); i++) {
				parts.get(i).backuper.backup(dout);
			}
			
			buf.flip();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public void restore() {
		
		InputStream in = new InputStream() {

			@Override
			public int read()
			throws IOException {
				return buf.get() & 0xff;
			}
		};
		
		try (DataInputStream din = new DataInputStream(in)) {
			
			// iterate in reverse order for restore
			int pos = buf.limit();
			for (int i=parts.size() - 1; i>=0; i--) {
				Part part = parts.get(i);
				pos -= part.size;
				buf.position(pos);
				part.restorer.restore(din);
			}
			
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
}
