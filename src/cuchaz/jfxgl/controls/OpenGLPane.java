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

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.sg.prism.NGRegion;
import com.sun.javafx.tk.RenderJob;
import com.sun.javafx.tk.TKPulseListener;
import com.sun.javafx.tk.Toolkit;
import com.sun.prism.Graphics;

import cuchaz.jfxgl.CalledByEventsThread;
import cuchaz.jfxgl.CalledByMainThread;
import cuchaz.jfxgl.GLState;
import cuchaz.jfxgl.InJavaFXGLContext;
import cuchaz.jfxgl.prism.JFXGLContext;
import cuchaz.jfxgl.prism.JFXGLContexts;
import cuchaz.jfxgl.prism.OffscreenBuffer;
import javafx.animation.AnimationTimer;
import javafx.scene.layout.Background;
import javafx.scene.layout.StackPane;

public class OpenGLPane extends StackPane {
	
	public static class OpenGLNode extends NGRegion {
	
		private final OpenGLPane pane;
		
		private JFXGLContext context;
		private OffscreenBuffer buf;
		
		private GLState glstate = new GLState(
			GLState.Blend, GLState.BlendFunc, GLState.ShaderProgram,
			GLState.ActiveTexture, GLState.Texture2D[0],
			GLState.VertexArray, GLState.ArrayBuffer
		);
		
		@CalledByEventsThread
		public OpenGLNode(OpenGLPane pane) {
			
			this.pane = pane;
			this.context = null;
			
			// this node should never have any backgrounds
			updateBackground(Background.EMPTY);
		}
		
		@Override
		@CalledByEventsThread
		public void release() {
			super.release();
			
			// need to cleanup on the render thread
			Toolkit.getToolkit().addRenderJob(new RenderJob(() -> {
				
				// cleanup
				if (buf != null) {
					buf.cleanup();
					buf = null;
				}
				JFXGLContexts.cleanupPane(this);
				
			}));
		}

		@Override
		protected boolean hasVisuals() {
			return true;
		}

		@Override
		@InJavaFXGLContext
		@CalledByMainThread
		protected void renderContent(Graphics g) {
			
			// get our node bounds
			// NOTE: JavaFX apparently uses the y axis down convention
			BaseBounds bounds = getCompleteBounds(new RectBounds(), g.getTransformNoClone());
			if (g.getClipRectNoClone() != null) {
				bounds.intersectWith(g.getClipRectNoClone());
			}
			int w = (int)bounds.getWidth();
			int h = (int)bounds.getHeight();
			
			// make the context if needed
			if (context == null) {
				// NOTE: make a context that's compatible with JavaFX, which means we need to be backwards-compatible
				GLFW.glfwDefaultWindowHints();
				context = JFXGLContexts.makeNewPane(this);
			}
				
			// init the pane if needed (in its GL context)
			if (pane.initializer != null) {
				
				context.makeCurrent();
				context.updateViewportAndDepthTest(0, 0, w, h, false);
				
				pane.initializer.init(context);
				
				JFXGLContexts.javafx.makeCurrent();
				
				// don't call the initializer again
				pane.initializer = null;
			}
			
			if (pane.renderer != null) {
				
				// TODO: apparently JavaFX already allocated a framebuf for this?
				// use that instead of our own?
				
				// is there anything to draw?
				if (w > 0 && h > 0) {
					
					// JavaFX's renderer has a crap-ton of internal caching and buffering!
					// I can't quite figure out how to bypass all that from the outside,
					// but calling g.clearQuad() apparently flushes everything so we can
					// render with raw OpenGL and expect it not to be overwritten again
					// later by some other buffer flush.
					int rx = (int)bounds.getMinX();
					int ry = (int)bounds.getMinY();
					int tw = g.getRenderTarget().getContentWidth();
					int th = g.getRenderTarget().getContentHeight();
					ry = th - ry - h;
					g.clearQuad(rx, ry, rx + tw, ry + th);
					// TODO: figure out what this does and flush the things manually
					// that way we could render panes with transparency
					
					// switch to the pane GL context
					context.makeCurrent();
					
					// do we need to resize the buffer?
					boolean wasResized;
					if (buf == null) {
						buf = new OffscreenBuffer(context, w, h);
						wasResized = true;
					} else {
						wasResized = buf.resize(w, h);
					}
					if (wasResized) {
						if (pane.resizer != null) {
							pane.resizer.resize(context, w, h);
						}
					}
					
					// call the downstream renderer
					buf.bind();
					pane.renderer.render(context);
					
					JFXGLContexts.javafx.makeCurrent();
					
					glstate.backup();
					
					GL11.glDisable(GL11.GL_BLEND);
					
					// render pane fbo to javafx fbo
					boolean yflip = true; // JavaFX apparently renders upside down
					buf.render(rx, ry, tw, th, yflip);
					
					// restore original JavaFX render state
					glstate.restore();
					
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
	
	public static interface Initializer {
		void init(JFXGLContext context);
	}
	
	public static interface Renderer {
		void render(JFXGLContext context);
	}
	
	public static interface Resizer {
		void resize(JFXGLContext context, int width, int height);
	}

	private Initializer initializer = null;
	private Resizer resizer = null;
	private Renderer renderer = null;
	
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
	
	public void setInitializer(Initializer val) {
		initializer = val;
	}
	
	public void setResizer(Resizer val) {
		resizer = val;
	}

	public void setRenderer(Renderer val) {
		this.renderer = val;
	}
	
	@CalledByMainThread
	public OffscreenBuffer getBuffer() {
		return impl_getPeer().buf;
	}
}
