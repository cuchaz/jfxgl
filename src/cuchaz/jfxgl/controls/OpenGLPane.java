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

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.sg.prism.NGRegion;
import com.sun.javafx.tk.TKPulseListener;
import com.sun.javafx.tk.Toolkit;
import com.sun.prism.Graphics;

import cuchaz.jfxgl.CalledByEventsThread;
import cuchaz.jfxgl.CalledByMainThread;
import cuchaz.jfxgl.prism.JFXGLContext;
import cuchaz.jfxgl.prism.OffscreenBuffer;
import javafx.animation.AnimationTimer;
import javafx.scene.layout.Background;
import javafx.scene.layout.StackPane;

public class OpenGLPane extends StackPane {
	
	private static class OpenGLNode extends NGRegion {
	
		private final OpenGLPane pane;
		
		private OffscreenBuffer buf;
		
		@CalledByEventsThread
		public OpenGLNode(OpenGLPane pane) {
			this.pane = pane;
			
			// this node should never have any backgrounds
			updateBackground(Background.EMPTY);
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
		protected boolean hasVisuals() {
			return true;
		}

		@Override
		@CalledByMainThread
		protected void renderContent(Graphics g) {
			
			if (pane.renderer != null) {
				
				// TODO: apparently JavaFX already allocated a framebuf for this?
				// use that instead of our own?
				
				// get our node bounds
				// NOTE: JavaFX apparently uses the y axis down convention
				BaseBounds bounds = getCompleteBounds(new RectBounds(), g.getTransformNoClone());
				if (g.getClipRectNoClone() != null) {
					bounds.intersectWith(g.getClipRectNoClone());
				}
				int w = (int)bounds.getWidth();
				int h = (int)bounds.getHeight();
				
				// is there anything to draw?
				if (w > 0 && h > 0) {
					
					JFXGLContext context = JFXGLContext.get();
					
					// backup current OpenGL state
					final int GLBackupBits = 0
						| GL11.GL_COLOR_BUFFER_BIT
						| GL11.GL_DEPTH_BUFFER_BIT
						| GL11.GL_ENABLE_BIT
						| GL11.GL_TEXTURE_BIT
						| GL11.GL_TRANSFORM_BIT
						| GL11.GL_VIEWPORT_BIT;
					int oldShaderId = context.getShaderProgram();
					GL11.glPushAttrib(GLBackupBits);
					
					// do we need to resize the buffer?
					if (buf == null) {
						buf = new OffscreenBuffer(context, w, h);
					} else {
						buf.resize(w, h);
					}
					
					// JavaFX's renderer has a crap-ton of internal caching and buffering!
					// I can't quite figure out how to bypass all that from the outside,
					// but calling g.clearQuad() apparently flushes everything so we can
					// render with raw OpenGL and expect it not to be overwritten again
					// later by some other buffer flush.
					g.clearQuad(
						bounds.getMinX(),
						bounds.getMinY(),
						bounds.getMaxX(),
						bounds.getMaxY()
					);
					
					// go back to a semi-normal OpenGL state
					GL11.glDisable(GL11.GL_BLEND);
					GL11.glDepthMask(false);
					GL11.glDisable(GL11.GL_DEPTH_TEST);
					GL11.glDisable(GL11.GL_SCISSOR_TEST);
					
					// call the downstream renderer
					GL11.glPushAttrib(GL11.GL_VIEWPORT_BIT);
					context.updateViewportAndDepthTest(0, 0, w, h, false);
					int oldFboId = buf.bind();
					pane.renderer.run();
					buf.unbind(oldFboId);
					GL11.glPopAttrib();
					
					// render pane fbo to javafx fbo
					boolean yflip = true; // JavaFX apparently renders upside down
					int rx = (int)bounds.getMinX();
					int ry = (int)bounds.getMinY();
					int tw = g.getRenderTarget().getContentWidth();
					int th = g.getRenderTarget().getContentHeight();
					ry = th - ry - h;
					buf.render(rx, ry, tw, th, yflip);
					
					// restore original JavaFX render state
					GL11.glPopAttrib();
					context.setShaderProgram(oldShaderId);
					
					/* DEBUG
					System.out.println(String.format("render pane:    pos=%d,%d   size=%d,%d   clip=%s   bounds=%s",
						rx, ry,
						w, h,
						g.getClipRect() == null ? "null" : String.format("[%d,%d]x[%d,%d]",
							g.getClipRect().x,
							g.getClipRect().x + g.getClipRect().width,
							g.getClipRect().y,
							g.getClipRect().y + g.getClipRect().height
						),
						String.format("[%.0f,%.0f]x[%.0f,%.0f]",
							bounds.getMinX(),
							bounds.getMaxX(),
							bounds.getMinY(),
							bounds.getMaxY()
						)
					));
					*/
				}
			}
			
			// render child nodes
			super.renderContent(g);
		}
	}

	public Runnable renderer = null;
	
	// NOTE: need to keep a strong reference to this listener or it will get garbage collected
	private TKPulseListener listener = new TKPulseListener() {
		@Override
		@SuppressWarnings("deprecation")
		public void pulse() {
			
			// keep the node dirty so it always gets rendered
			impl_markDirty(DirtyBits.NODE_BOUNDS);
			impl_markDirty(DirtyBits.NODE_CONTENTS);
		}
	};
	
	private AnimationTimer animation = new AnimationTimer() {
		@Override
		public void handle(long l) {
			// nothing to do
			// we just keep this animation running so JavaFX will keep sending pulses
		}
	};

	public OpenGLPane() {
		
		Toolkit.getToolkit().addSceneTkPulseListener(listener);
		animation.start();
		
		// TODO: how to clean up animation? don't know of any dispose/cleanup/release methods for nodes
		
		// MODENA css (the default style) wants us to have a background color
		// background colors are bad. let's turn that off
		getStylesheets().add(getClass().getResource("style.css").toString());
		getStyleClass().add("openglpane");
	}
	
	@Override
	public OpenGLNode impl_createPeer() {
		return new OpenGLNode(this);
	}
	
	@Override
	@SuppressWarnings({ "unchecked", "deprecation" })
	public OpenGLNode impl_getPeer() {
		return (OpenGLNode)super.impl_getPeer();
	}

	public void setRenderer(Runnable val) {
		renderer = val;
	}
	
	@CalledByMainThread
	public OffscreenBuffer getBuffer() {
		return impl_getPeer().buf;
	}
}
