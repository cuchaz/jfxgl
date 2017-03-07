/*************************************************************************
 * Copyright (C) 2017, Jeffrey W. Martin "Cuchaz"
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License Version 2 with
 * the classpath exception, as published by the Free Software Foundation.
 * 
 * See LICENSE.txt in the project root folder for the full license.
 *************************************************************************/
package cuchaz.jfxgl.controls;

import org.lwjgl.opengl.GL11;

import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.sg.prism.NGRegion;
import com.sun.prism.Graphics;

import cuchaz.jfxgl.CalledByEventsThread;
import cuchaz.jfxgl.CalledByMainThread;
import cuchaz.jfxgl.prism.JFXGLContext;
import cuchaz.jfxgl.prism.OffscreenBuffer;
import javafx.scene.layout.StackPane;

public class OpenGLPane extends StackPane {
	
	private static class OpenGLNode extends NGRegion {
	
		private static int GLBackupBits = GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_ENABLE_BIT
			| GL11.GL_TEXTURE_BIT | GL11.GL_TRANSFORM_BIT | GL11.GL_VIEWPORT_BIT;
			
		private final OpenGLPane pane;
		
		private OffscreenBuffer buf;
		
		@CalledByEventsThread
		public OpenGLNode(OpenGLPane pane) {
			this.pane = pane;
		}
		
		@Override
		@CalledByEventsThread
		public void release() {
			super.release();
			
			// cleanup
			if (buf != null) {
				buf.cleanup();
				buf = null;
			}
		}

		@Override
		@CalledByMainThread
		protected void renderContent(Graphics g) {
			
			if (pane.renderer != null) {
				
				// TODO: apparently JavaFX already allocated a framebuf for this?
				// use that instead of our own?
				
				GL11.glPushAttrib(GLBackupBits);
				
				// go back to a semi-normal OpenGL state
				GL11.glEnable(GL11.GL_BLEND);
				GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
				GL11.glDepthMask(false);
				GL11.glDisable(GL11.GL_DEPTH_TEST);
				GL11.glDisable(GL11.GL_SCISSOR_TEST);
				
				// get our node rect
				// NOTE: JavaFX uses top-down y axis convention
				int x = g.getClipRect().x;
				int y = g.getClipRect().y;
				int w = g.getClipRect().width;
				int h = g.getClipRect().height;
				
				JFXGLContext context = JFXGLContext.get();
				int oldFboId = context.getBoundFBO();
				
				// do we need to resize the buffer?
				if (buf == null) {
					buf = new OffscreenBuffer(context, w, h);
				} else {
					buf.resize(w, h);
				}
				
				// call the downstream renderer
				buf.bind();
				context.updateViewportAndDepthTest(0, 0, w, h, false);
				pane.renderer.run();
				buf.unbind(oldFboId);
				
				// render pane fbo to javafx fbo
				context.updateViewportAndDepthTest(x, y, w, h, false);
				boolean yflip = true; // JavaFX apparently renders upside down
				buf.render(x, y, yflip);
				
				GL11.glPopAttrib();
			}
			
			// render child nodes
			super.renderContent(g);
			
			// then set the node dirty so it gets rendered again next time
			markDirty();
		}
	}

	public Runnable renderer = null;
	
	@Override
	public NGNode impl_createPeer() {
		return new OpenGLNode(this);
	}

	public void setRenderer(Runnable val) {
		renderer = val;
	}
}
