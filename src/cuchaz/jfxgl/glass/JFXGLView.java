package cuchaz.jfxgl.glass;

import java.util.HashMap;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

import com.sun.glass.events.KeyEvent;
import com.sun.glass.events.MouseEvent;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.View;

import cuchaz.jfxgl.CalledByEventsThread;
import cuchaz.jfxgl.CalledByMainThread;
import cuchaz.jfxgl.Log;
import javafx.scene.input.KeyCode;

public class JFXGLView extends View {

	private JFXGLWindow window;
	
	@Override
	@SuppressWarnings("rawtypes")
	protected long _create(Map capabilities) {
		
		window = null;
		
		// don't care about the screen handle
		long hscreen = 1l;
		return hscreen;
	}

	@Override
	protected void _enableInputMethodEvents(long hscreen, boolean enable) {
		// TODO: implement IME?
		// it doesn't look like GLFW can handle IME yet
		// see: https://github.com/glfw/glfw/issues/41
		// but we might be able to wrap JavaFX's cross-platform IME libraries
	}

	@Override
	protected long _getNativeView(long hscreen) {
		return hscreen;
	}

	@Override
	protected int _getX(long hscreen) {
		return 0;
	}

	@Override
	protected int _getY(long hscreen) {
		return 0;
	}

	@Override
	@CalledByEventsThread
	protected void _setParent(long hscreen, long hwnd) {
		
		// TEMP
		Log.log("JFXGLView._setParent()   hwnd=%d", hwnd);
		
		this.window = (JFXGLWindow)super.getWindow();
	}

	@Override
	protected boolean _close(long hscreen) {
		return true;
	}

	@Override
	protected void _scheduleRepaint(long hscreen) {
		// TEMP
		Log.log("JFXGLView._scheduleRepaint()");
	}

	@Override
	@CalledByMainThread
	protected void _begin(long hscreen) {
		// forward to the window
		window.renderBegin();
	}

	@Override
	@CalledByMainThread
	protected void _end(long hscreen) {
		// forward to the window
		window.renderEnd();
	}

	@Override
	@CalledByMainThread
	protected int _getNativeFrameBuffer(long hscreen) {
		// forward to the window
		return window.getFBOId();
	}

	@Override
	protected void _uploadPixels(long hscreen, Pixels pixels) {
		// TEMP
		Log.log("JFXGLView._uploadPixels()");
	}

	@Override
	protected boolean _enterFullscreen(long hscreen, boolean animate, boolean keepRatio, boolean hideCursor) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void _exitFullscreen(long hscreen, boolean animate) {
		throw new UnsupportedOperationException();
	}
	
	// just override this method just to make it public
	@Override
	public void notifyResize(int width, int height) {
		super.notifyResize(width, height);
	}
	
	public void handleGLFWKey(int key, int scanCode, int action, int mods) {
		
		// translate from GLFW to JavaFX
		int type = translateKeyAction(action);
		@SuppressWarnings("deprecation")
		int keyCode = translateKey(key).impl_getCode();
		char[] keyChars = {};
		int modifiers = translateMods(mods);
		
		notifyKey(type, keyCode, keyChars, modifiers);
	}
	
	public void handleGLFWKeyChar(int codepoint, int mods) {
		
		// translate from GLFW to JavaFX
		int type = KeyEvent.TYPED;
		int keyCode = 0;
		String str = new String(new int[] { codepoint }, 0, 1);
		char[] keyChars = str.toCharArray();
		int modifiers = translateMods(mods);
		
		notifyKey(type, keyCode, keyChars, modifiers);
	}
	
	private Map<Integer,Boolean> mouseButtonIsDown = new HashMap<>();
	private int mouseX = 0;
	private int mouseY = 0;

	public void handleGLFWCursorPos(double x, double y) {
		
		// save the latest mouse pos
		mouseX = (int)x;
		mouseY = (int)y;
		
		// is a button down?
		boolean isDown = false;
		for (Boolean val : mouseButtonIsDown.values()) {
			isDown = isDown || val;
		}
		
		// translate from GLFW to JavaFX
		int type = isDown ? MouseEvent.DRAG : MouseEvent.MOVE;
		int button = MouseEvent.BUTTON_NONE;
		int modifiers = 0;
		
		notifyMouse(type, button, modifiers);
	}

	public void handleGLFWMouseButton(int button, int action, int mods) {
		
		// translate from GLFW to JavaFX
		int type = translateMouseAction(action);
		button = translateMouseButton(button);
		int modifiers = translateMods(mods);
		
		notifyMouse(type, button, modifiers);
	
		// update down state
		switch (action) {
			case GLFW.GLFW_PRESS:
				mouseButtonIsDown.put(button, true);
			break;
			case GLFW.GLFW_RELEASE:
				mouseButtonIsDown.put(button, false);
			break;
		}
	}
	
	private void notifyMouse(int type, int button, int modifiers) {
		
		int xAbs = mouseX;
		int yAbs = mouseY;
		boolean isPopupTrigger = false; // do we need to implement this?
		boolean isSynthesized = false; // do we need to implement this? has to do with touch stuff I think
		
		notifyMouse(type, button, mouseX, mouseY, xAbs, yAbs, modifiers, isPopupTrigger, isSynthesized);
	}

	public void handleGLFWScroll(double dx, double dy) {
		
		// translate from GLFW to JavaFX
		int xAbs = mouseX;
		int yAbs = mouseY;
		int modifiers = 0; // GLFW doesn't give us modifiers for mouse scroll =(
		int lines = 1;
		int chars = 0;
		int defaultLines = 0;
		int defaultChars = 0;
		double xMultiplier = 1.0;
		double yMultiplier = 1.0;
		
		notifyScroll(mouseX, mouseY, xAbs, yAbs, dx, dy, modifiers, lines, chars, defaultLines, defaultChars, xMultiplier, yMultiplier);
	}
	
	private int translateKeyAction(int action) {
		switch (action) {
			case GLFW.GLFW_PRESS: return KeyEvent.PRESS;
			case GLFW.GLFW_RELEASE: return KeyEvent.RELEASE;
			default: throw new RuntimeException("unknown key action: " + action);
		}
	}
	
	private KeyCode translateKey(int scanCode) {
		switch (scanCode) {
			case GLFW.GLFW_KEY_SPACE: return KeyCode.SPACE;
			case GLFW.GLFW_KEY_APOSTROPHE: return KeyCode.QUOTE;
			case GLFW.GLFW_KEY_COMMA: return KeyCode.COMMA;
			case GLFW.GLFW_KEY_MINUS: return KeyCode.MINUS;
			case GLFW.GLFW_KEY_PERIOD: return KeyCode.PERIOD;
			case GLFW.GLFW_KEY_SLASH: return KeyCode.SLASH;
			case GLFW.GLFW_KEY_0: return KeyCode.DIGIT0;
			case GLFW.GLFW_KEY_1: return KeyCode.DIGIT1;
			case GLFW.GLFW_KEY_2: return KeyCode.DIGIT2;
			case GLFW.GLFW_KEY_3: return KeyCode.DIGIT3;
			case GLFW.GLFW_KEY_4: return KeyCode.DIGIT4;
			case GLFW.GLFW_KEY_5: return KeyCode.DIGIT5;
			case GLFW.GLFW_KEY_6: return KeyCode.DIGIT6;
			case GLFW.GLFW_KEY_7: return KeyCode.DIGIT7;
			case GLFW.GLFW_KEY_8: return KeyCode.DIGIT8;
			case GLFW.GLFW_KEY_9: return KeyCode.DIGIT9;
			case GLFW.GLFW_KEY_SEMICOLON: return KeyCode.SEMICOLON;
			case GLFW.GLFW_KEY_EQUAL: return KeyCode.EQUALS;
			case GLFW.GLFW_KEY_A: return KeyCode.A;
			case GLFW.GLFW_KEY_B: return KeyCode.B;
			case GLFW.GLFW_KEY_C: return KeyCode.C;
			case GLFW.GLFW_KEY_D: return KeyCode.D;
			case GLFW.GLFW_KEY_E: return KeyCode.E;
			case GLFW.GLFW_KEY_F: return KeyCode.F;
			case GLFW.GLFW_KEY_G: return KeyCode.G;
			case GLFW.GLFW_KEY_H: return KeyCode.H;
			case GLFW.GLFW_KEY_I: return KeyCode.I;
			case GLFW.GLFW_KEY_J: return KeyCode.J;
			case GLFW.GLFW_KEY_K: return KeyCode.K;
			case GLFW.GLFW_KEY_L: return KeyCode.L;
			case GLFW.GLFW_KEY_M: return KeyCode.M;
			case GLFW.GLFW_KEY_N: return KeyCode.N;
			case GLFW.GLFW_KEY_O: return KeyCode.O;
			case GLFW.GLFW_KEY_P: return KeyCode.P;
			case GLFW.GLFW_KEY_Q: return KeyCode.Q;
			case GLFW.GLFW_KEY_R: return KeyCode.R;
			case GLFW.GLFW_KEY_S: return KeyCode.S;
			case GLFW.GLFW_KEY_T: return KeyCode.T;
			case GLFW.GLFW_KEY_U: return KeyCode.U;
			case GLFW.GLFW_KEY_V: return KeyCode.V;
			case GLFW.GLFW_KEY_W: return KeyCode.W;
			case GLFW.GLFW_KEY_X: return KeyCode.X;
			case GLFW.GLFW_KEY_Y: return KeyCode.Y;
			case GLFW.GLFW_KEY_Z: return KeyCode.Z;
			case GLFW.GLFW_KEY_LEFT_BRACKET: return KeyCode.OPEN_BRACKET;
			case GLFW.GLFW_KEY_BACKSLASH: return KeyCode.BACK_SLASH;
			case GLFW.GLFW_KEY_RIGHT_BRACKET: return KeyCode.CLOSE_BRACKET;
			case GLFW.GLFW_KEY_GRAVE_ACCENT: return KeyCode.BACK_QUOTE;
			case GLFW.GLFW_KEY_WORLD_1: return KeyCode.UNDEFINED;
			case GLFW.GLFW_KEY_WORLD_2: return KeyCode.UNDEFINED;
			case GLFW.GLFW_KEY_ESCAPE: return KeyCode.ESCAPE;
			case GLFW.GLFW_KEY_ENTER: return KeyCode.ENTER;
			case GLFW.GLFW_KEY_TAB: return KeyCode.TAB;
			case GLFW.GLFW_KEY_BACKSPACE: return KeyCode.BACK_SPACE;
			case GLFW.GLFW_KEY_INSERT: return KeyCode.INSERT;
			case GLFW.GLFW_KEY_DELETE: return KeyCode.DELETE;
			case GLFW.GLFW_KEY_RIGHT: return KeyCode.RIGHT;
			case GLFW.GLFW_KEY_LEFT: return KeyCode.LEFT;
			case GLFW.GLFW_KEY_DOWN: return KeyCode.DOWN;
			case GLFW.GLFW_KEY_UP: return KeyCode.UP;
			case GLFW.GLFW_KEY_PAGE_UP: return KeyCode.PAGE_UP;
			case GLFW.GLFW_KEY_PAGE_DOWN: return KeyCode.PAGE_DOWN;
			case GLFW.GLFW_KEY_HOME: return KeyCode.HOME;
			case GLFW.GLFW_KEY_END: return KeyCode.END;
			case GLFW.GLFW_KEY_CAPS_LOCK: return KeyCode.CAPS;
			case GLFW.GLFW_KEY_SCROLL_LOCK: return KeyCode.SCROLL_LOCK;
			case GLFW.GLFW_KEY_NUM_LOCK: return KeyCode.NUM_LOCK;
			case GLFW.GLFW_KEY_PRINT_SCREEN: return KeyCode.PRINTSCREEN;
			case GLFW.GLFW_KEY_PAUSE: return KeyCode.PAUSE;
			case GLFW.GLFW_KEY_F1: return KeyCode.F1;
			case GLFW.GLFW_KEY_F2: return KeyCode.F2;
			case GLFW.GLFW_KEY_F3: return KeyCode.F3;
			case GLFW.GLFW_KEY_F4: return KeyCode.F4;
			case GLFW.GLFW_KEY_F5: return KeyCode.F5;
			case GLFW.GLFW_KEY_F6: return KeyCode.F6;
			case GLFW.GLFW_KEY_F7: return KeyCode.F7;
			case GLFW.GLFW_KEY_F8: return KeyCode.F8;
			case GLFW.GLFW_KEY_F9: return KeyCode.F9;
			case GLFW.GLFW_KEY_F10: return KeyCode.F10;
			case GLFW.GLFW_KEY_F11: return KeyCode.F11;
			case GLFW.GLFW_KEY_F12: return KeyCode.F12;
			case GLFW.GLFW_KEY_F13: return KeyCode.F13;
			case GLFW.GLFW_KEY_F14: return KeyCode.F14;
			case GLFW.GLFW_KEY_F15: return KeyCode.F15;
			case GLFW.GLFW_KEY_F16: return KeyCode.F16;
			case GLFW.GLFW_KEY_F17: return KeyCode.F17;
			case GLFW.GLFW_KEY_F18: return KeyCode.F18;
			case GLFW.GLFW_KEY_F19: return KeyCode.F19;
			case GLFW.GLFW_KEY_F20: return KeyCode.F20;
			case GLFW.GLFW_KEY_F21: return KeyCode.F21;
			case GLFW.GLFW_KEY_F22: return KeyCode.F22;
			case GLFW.GLFW_KEY_F23: return KeyCode.F23;
			case GLFW.GLFW_KEY_F24: return KeyCode.F24;
			case GLFW.GLFW_KEY_F25: return KeyCode.UNDEFINED;
			case GLFW.GLFW_KEY_KP_0: return KeyCode.NUMPAD0;
			case GLFW.GLFW_KEY_KP_1: return KeyCode.NUMPAD1;
			case GLFW.GLFW_KEY_KP_2: return KeyCode.NUMPAD2;
			case GLFW.GLFW_KEY_KP_3: return KeyCode.NUMPAD3;
			case GLFW.GLFW_KEY_KP_4: return KeyCode.NUMPAD4;
			case GLFW.GLFW_KEY_KP_5: return KeyCode.NUMPAD5;
			case GLFW.GLFW_KEY_KP_6: return KeyCode.NUMPAD6;
			case GLFW.GLFW_KEY_KP_7: return KeyCode.NUMPAD7;
			case GLFW.GLFW_KEY_KP_8: return KeyCode.NUMPAD8;
			case GLFW.GLFW_KEY_KP_9: return KeyCode.NUMPAD9;
			case GLFW.GLFW_KEY_KP_DECIMAL: return KeyCode.DECIMAL;
			case GLFW.GLFW_KEY_KP_DIVIDE: return KeyCode.DIVIDE;
			case GLFW.GLFW_KEY_KP_MULTIPLY: return KeyCode.MULTIPLY;
			case GLFW.GLFW_KEY_KP_SUBTRACT: return KeyCode.SUBTRACT;
			case GLFW.GLFW_KEY_KP_ADD: return KeyCode.ADD;
			case GLFW.GLFW_KEY_KP_ENTER: return KeyCode.ENTER;
			case GLFW.GLFW_KEY_KP_EQUAL: return KeyCode.EQUALS;
			case GLFW.GLFW_KEY_LEFT_SHIFT: return KeyCode.SHIFT;
			case GLFW.GLFW_KEY_LEFT_CONTROL: return KeyCode.CONTROL;
			case GLFW.GLFW_KEY_LEFT_ALT: return KeyCode.ALT;
			case GLFW.GLFW_KEY_LEFT_SUPER: return KeyCode.WINDOWS;
			case GLFW.GLFW_KEY_RIGHT_SHIFT: return KeyCode.SHIFT;
			case GLFW.GLFW_KEY_RIGHT_CONTROL: return KeyCode.CONTROL;
			case GLFW.GLFW_KEY_RIGHT_ALT: return KeyCode.ALT;
			case GLFW.GLFW_KEY_RIGHT_SUPER: return KeyCode.WINDOWS;
			case GLFW.GLFW_KEY_MENU: return KeyCode.CONTEXT_MENU;
			default: return KeyCode.UNDEFINED;
		}
	}
	
	private int translateMouseAction(int action) {
		switch (action) {
			case GLFW.GLFW_PRESS: return MouseEvent.DOWN;
			case GLFW.GLFW_RELEASE: return MouseEvent.UP;
			default: throw new RuntimeException("unknown mouse action: " + action);
		}
	}
	
	private int translateMods(int mods) {
		
		int modifiers = 0;
		
		if ((mods & GLFW.GLFW_MOD_SHIFT) != 0) {
			modifiers |= KeyEvent.MODIFIER_SHIFT;
		}
		
		if ((mods & GLFW.GLFW_MOD_CONTROL) != 0) {
			modifiers |= KeyEvent.MODIFIER_CONTROL;
		}
		
		if ((mods & GLFW.GLFW_MOD_ALT) != 0) {
			modifiers |= KeyEvent.MODIFIER_ALT;
		}
		
		if ((mods & GLFW.GLFW_MOD_SUPER) != 0) {
			modifiers |= KeyEvent.MODIFIER_WINDOWS;
		}
		
		return modifiers;
	}
		
	private int translateMouseButton(int button) {
		switch (button) {
			case GLFW.GLFW_MOUSE_BUTTON_LEFT: return MouseEvent.BUTTON_LEFT;
			case GLFW.GLFW_MOUSE_BUTTON_RIGHT: return MouseEvent.BUTTON_RIGHT;
			default: return MouseEvent.BUTTON_OTHER;
		}
	}
}
