/*************************************************************************
 * Copyright (C) 2017, Jeffrey W. Martin "Cuchaz"
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License Version 2 with
 * the classpath exception, as published by the Free Software Foundation.
 * 
 * See LICENSE.txt in the project root folder for the full license.
 *************************************************************************/
package cuchaz.jfxgl.prism;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public class OffscreenBuffer {

	private JFXGLContext context;
	private int width = 0;
	private int height = 0;
	private int texId = 0;
	private int fboId = 0;
	private boolean quadDirty = true;
	private TexturedQuad quad = null;
	private TexturedQuad.Shader quadShader = null;
	
	public OffscreenBuffer(JFXGLContext context, int width, int height) {
		
		if (width <= 0 || height <= 0) {
			throw new IllegalArgumentException(String.format("invalid size: %dx%d", width, height));
		}
		
		this.context = context;
		
		// lazily create the quad and shader,
		// in case we want to render this buf in a different context than the one we created it in
		// (vertex arrays aren't shared between contexts, so neither are quads)
		quad = null;
		quadShader = null;
		resize(width, height);
	}
	
	public boolean resize(int width, int height) {
		
		if (this.width == width && this.height == height) {
			return false;
		}
		
		this.width = width;
		this.height = height;
	
		// resize the texture
		if (texId != 0) {
			context.deleteTexture(texId);
		}
		texId = context.createTexture(width, height);
		
		// update the framebuf
		if (fboId == 0) {
			fboId = GL30.glGenFramebuffers();
		}
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboId);
		GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, texId, 0);
		
		// remove the old quad
		quadDirty = true;
		
		return true;
	}
	
	public int getTexId() {
		return texId;
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
		render(0, 0, width, height, false);
	}
	
	public void render(int x, int y, int w, int h, boolean yflip) {
		
		if (quadShader == null) {
			quadShader = new TexturedQuad.Shader(context);
		}
		quadShader.bind();
		quadShader.setViewPos(x, y);
		quadShader.setViewSize(w, h);
		quadShader.setYFlip(yflip);
		
		if (quadDirty) {
			quadDirty = false;
			if (quad != null) {
				quad.cleanup();
			}
			quad = new TexturedQuad(0, 0, width, height, texId, quadShader);
		}
		quad.render();
	}
	
	public void unbind(int oldFboId) {
		context.bindFBO(oldFboId);
	}
	
	public void cleanup() {
		context.deleteTexture(texId);
		if (fboId != 0) {
			GL30.glDeleteFramebuffers(fboId);
		}
		if (quad != null) {
			quad.cleanup();
		}
		if (quadShader != null) {
			quadShader.cleanup();
		}
	}
}
