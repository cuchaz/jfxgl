package cuchaz.jfxgl;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLDebugMessageCallback;
import org.lwjgl.opengl.KHRDebug;
import org.lwjgl.system.APIUtil;
import org.lwjgl.system.Callback;
import org.lwjgl.system.MemoryUtil;

public class LWJGLDebug {
	
	public static PrintStream stream = APIUtil.DEBUG_STREAM;
	public static Integer[] exceptionSeverities = { GL43.GL_DEBUG_SEVERITY_HIGH };
	public static Integer[] reportSeverities = { GL43.GL_DEBUG_SEVERITY_HIGH, GL43.GL_DEBUG_SEVERITY_MEDIUM, GL43.GL_DEBUG_SEVERITY_LOW };
	
	public static Callback enableDebugging() {
		
		// copy global state for the closure
		PrintStream stream = LWJGLDebug.stream;
		Set<Integer> exceptionSeverities = new HashSet<>(Arrays.asList(LWJGLDebug.exceptionSeverities));
		Set<Integer> reportSeverities = new HashSet<>(Arrays.asList(LWJGLDebug.reportSeverities));
		
		// define the error reporting
		GLDebugMessageCallback proc = GLDebugMessageCallback.create((source, type, id, severity, length, message, userParam) -> {
			
			// skip this message?
			if (!reportSeverities.contains(severity)) {
				return;
			}
			
			// build the message
			StringBuilder buf = new StringBuilder();
			buf.append("OpenGL Debug Message:\n");
			buf.append(String.format("\tID:        0x%X\n", id));
			buf.append(String.format("\tSource:    %s\n", getDebugSource(source)));
			buf.append(String.format("\tType:      %s\n", getDebugType(type)));
			buf.append(String.format("\tSeverity:  %s\n", getDebugSeverity(severity)));
			buf.append(String.format("\tMessage:   %s\n", GLDebugMessageCallback.getMessage(length, message)));
			
			boolean isException = exceptionSeverities.contains(severity);
			if (isException) {
				throw new RuntimeException(buf.toString());
			} else {
				stream.print(buf.toString());
			}
		});
		
		// if supported, get java stack traces with opengl errors
		GLCapabilities caps = GL.getCapabilities();
		if (caps.GL_KHR_debug) {
			GL11.glEnable(KHRDebug.GL_DEBUG_OUTPUT_SYNCHRONOUS);
		}
		
		// install the callback
		GL43.glDebugMessageCallback(proc, MemoryUtil.NULL);
		
		// turn on opengl debugging globally
		GL11.glEnable(GL43.GL_DEBUG_OUTPUT);
		
		return proc;
	}
	
	public static String getDebugSource(int source) { switch (source) {
			case GL43.GL_DEBUG_SOURCE_API:
				return "API";
			case GL43.GL_DEBUG_SOURCE_WINDOW_SYSTEM:
				return "WINDOW SYSTEM";
			case GL43.GL_DEBUG_SOURCE_SHADER_COMPILER:
				return "SHADER COMPILER";
			case GL43.GL_DEBUG_SOURCE_THIRD_PARTY:
				return "THIRD PARTY";
			case GL43.GL_DEBUG_SOURCE_APPLICATION:
				return "APPLICATION";
			case GL43.GL_DEBUG_SOURCE_OTHER:
				return "OTHER";
			default:
				return "(unknown: " + source + ")";
		}
	}

	public static String getDebugType(int type) {
		switch (type) {
			case GL43.GL_DEBUG_TYPE_ERROR:
				return "ERROR";
			case GL43.GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR:
				return "DEPRECATED BEHAVIOR";
			case GL43.GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR:
				return "UNDEFINED BEHAVIOR";
			case GL43.GL_DEBUG_TYPE_PORTABILITY:
				return "PORTABILITY";
			case GL43.GL_DEBUG_TYPE_PERFORMANCE:
				return "PERFORMANCE";
			case GL43.GL_DEBUG_TYPE_OTHER:
				return "OTHER";
			case GL43.GL_DEBUG_TYPE_MARKER:
				return "MARKER";
			default:
				return "(unknown: " + type + ")";
		}
	}

	public static String getDebugSeverity(int severity) {
		switch (severity) {
			case GL43.GL_DEBUG_SEVERITY_HIGH:
				return "HIGH";
			case GL43.GL_DEBUG_SEVERITY_MEDIUM:
				return "MEDIUM";
			case GL43.GL_DEBUG_SEVERITY_LOW:
				return "LOW";
			case GL43.GL_DEBUG_SEVERITY_NOTIFICATION:
				return "NOTIFICATION";
			default:
				return "(unknown: " + severity + ")";
		}
	}
}
