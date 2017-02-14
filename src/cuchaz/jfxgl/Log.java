package cuchaz.jfxgl;

public class Log {

	// TODO: get a real logging system, or get rid of all the debug spam
	
	public static void log(String msg, Object ... args) {
		log(String.format(msg, args));
	}
	
	public static void log(String msg) {
		System.out.println(Thread.currentThread().getName() + ": " + msg);
	}
}
