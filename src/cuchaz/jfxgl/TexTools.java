package cuchaz.jfxgl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL45;

public class TexTools {
	
	private static File makeTga(String filename) {
		return new File(filename + ".tga");
	}
	
	public static int getFramebufTexId() {
		return getFramebufTexId(0);
	}
	
	public static int getFramebufTexId(int attachmentIndex) {
		
		// what's the attachment?
		int attachmentId = GL11.glGetInteger(GL20.GL_DRAW_BUFFER0 + attachmentIndex);
		if (attachmentId == GL11.GL_NONE) {
			throw new Error("framebuffer has no attachment at " + attachmentIndex);
		}
		
		// what kind of attachment is it?
		int typeId = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_FRAMEBUFFER, attachmentId, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE);
		if (typeId != GL11.GL_TEXTURE) {
			throw new Error(String.format("attachment is not a texture: 0x%x", typeId));
		}
		
		// get the texture id
		return GL30.glGetFramebufferAttachmentParameteri(GL30.GL_FRAMEBUFFER, attachmentId, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
	}
	
	public static void dumpFramebuf(String filename) {
		dumpFramebuf(makeTga(filename));
	}
	
	public static void dumpFramebuf(File file) {
		dumpTexture(getFramebufTexId(), file);
	}
	
	public static void dumpTexture(int texId, String filename) {
		dumpTexture(texId, makeTga(filename));
	}

	public static void dumpTexture(int texId, File file) {
		
		int w = GL45.glGetTextureLevelParameteri(texId, 0, GL11.GL_TEXTURE_WIDTH);
		int h = GL45.glGetTextureLevelParameteri(texId, 0, GL11.GL_TEXTURE_HEIGHT);
		int bytesPerPixel = 4;
		
		// download the texture from the gpu
		ByteBuffer imgBuf = ByteBuffer.allocateDirect(w*h*bytesPerPixel);
		GL45.glGetTextureImage(texId, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, imgBuf);
		
		// convert from RGBA to BGRA
		for (int i=0; i<imgBuf.capacity(); i += 4) {
				
			imgBuf.position(i);
			byte r = imgBuf.get();
			byte g = imgBuf.get();
			byte b = imgBuf.get();
			byte a = imgBuf.get();
			
			imgBuf.position(i);
			imgBuf.put(b);
			imgBuf.put(g);
			imgBuf.put(r);
			imgBuf.put(a);
		}
		imgBuf.flip();
		
		// write a TGA file
		try (FileOutputStream out = new FileOutputStream(file)) {
			
			// write the header for a raw pixel array
			// https://en.wikipedia.org/wiki/Truevision_TGA
			out.write(new byte[] {
				0, // arbitrary id
				0, // no color map
				2, // uncompressed image data
				0, 0, 0, 0, 0, // color map info
				0, 0, // x
				0, 0, // y
				(byte)(w % 256), (byte)(w / 256),
				(byte)(h % 256), (byte)(h / 256),
				(byte)(bytesPerPixel*8),
				0 // image descriptor
			});
			
			// write pixel data
			Channels.newChannel(out).write(imgBuf);
			
		} catch (IOException ex) {
			ex.printStackTrace(System.err);
		}
		System.out.println("Wrote texture " + texId + " to " + file.getAbsolutePath());
	}
}
