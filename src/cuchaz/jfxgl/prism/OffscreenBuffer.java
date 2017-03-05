package cuchaz.jfxgl.prism;

import java.io.File;

import cuchaz.jfxgl.TexTools;

public class OffscreenBuffer {

	private JFXGLContext context;
	private int width = 0;
	private int height = 0;
	private int texId = 0;
	private int fboId = 0;
	private TexturedQuad quad = null;
	private TexturedQuad.Shader quadShader = null;
	
	public OffscreenBuffer(JFXGLContext context, int width, int height) {
		
		this.context = context;
		
		quadShader = new TexturedQuad.Shader(context);
		resize(width, height);
	}
	
	public void resize(int width, int height) {
		
		// TODO: can definitely optimize this (ie, re-use FBOs after texture changes)
		
		if (this.width == width && this.height == height) {
			return;
		}
		
		this.width = width;
		this.height = height;
		
		if (texId != 0) {
			context.deleteTexture(texId);
		}
		if (fboId != 0) {
			context.deleteFBO(fboId);
		}
		if (quad != null) {
			quad.cleanup();
		}
		
		texId = context.createTexture(width, height);
		fboId = context.createFBO(texId);
		quad = new TexturedQuad(0, 0, width, height, texId, quadShader);
	}
	
	public int getFboId() {
		return fboId;
	}
	
	public int bind() {
		int oldFboId = context.getBoundFBO();
		context.bindFBO(fboId);
		return oldFboId;
	}
	
	public void render() {
		render(0, 0, false);
	}
	
	public void render(int x, int y, boolean yflip) {
		quadShader.bind();
		quadShader.setViewPos(x, y);
		quadShader.setViewSize(width, height);
		quadShader.setYFlip(yflip);
		quad.render();
	}
	
	public void unbind(int oldFboId) {
		context.bindFBO(oldFboId);
	}
	
	public void cleanup() {
		context.deleteTexture(texId);
		context.deleteFBO(fboId);
		quad.cleanup();
		quadShader.cleanup();
	}
	
	public void dump(File file) {
		TexTools.dumpTexture(texId, file);
	}
}
