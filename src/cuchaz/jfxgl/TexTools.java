package cuchaz.jfxgl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL45;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineInt;
import ar.com.hjg.pngj.PngWriter;

public class TexTools {
	
	public static void dumpTexture(int texId, File file) {
		dumpTexture(texId, file, true);
	}
	
	public static void dumpTexture(int texId, File file, boolean flipY) {
		
		int w = GL45.glGetTextureLevelParameteri(texId, 0, GL11.GL_TEXTURE_WIDTH);
		int h = GL45.glGetTextureLevelParameteri(texId, 0, GL11.GL_TEXTURE_HEIGHT);
		int numChannels = 4;
		int bitsPerPixel = 8;
		
		// download the texture from the gpu
		ByteBuffer imgBuf = ByteBuffer.allocateDirect(w*h*numChannels);
		GL45.glGetTextureImage(texId, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, imgBuf);
		
		// write a PNG
		try (FileOutputStream out = new FileOutputStream(file)) {
			ImageInfo info = new ImageInfo(w, h, bitsPerPixel, true, false, false);
			PngWriter writer = new PngWriter(out, info);
			ImageLineInt pixels = new ImageLineInt(info);
			int[] scanline = pixels.getScanline();
			int stride = w*numChannels;
			
			int ystart = 0;
			int yend = h;
			int ystep = 1;
			if (flipY) {
				ystart = h - 1;
				yend = -1;
				ystep = -1;
			}
			
			for (int y = ystart; y != yend; y += ystep) {
				imgBuf.position(y*stride);
				for (int x=0; x<w; x++) {
					for (int c=0; c<numChannels; c++) {
						scanline[x*numChannels + c] = imgBuf.get();
					}
				}
				writer.writeRow(pixels);
			}
			writer.end();
		} catch (IOException ex) {
			ex.printStackTrace(System.err);
		}
		System.out.println("Wrote texture " + texId + " to " + file.getAbsolutePath());
	}
}
