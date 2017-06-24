package com.sun.glass.ui;

import java.util.Locale;

public class PlatformTools {

	@SuppressWarnings("unchecked")
	public static PlatformFactory makePlatformFactory() {
		try {
			
			String platform = getPlatform();
			String classname = "com.sun.glass.ui." + platform.toLowerCase(Locale.ROOT) + "." + platform + "PlatformFactory";
			return (PlatformFactory)((Class<PlatformFactory>)Class.forName(classname)).newInstance();
			
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public static String getPlatform() {
		
        String osName = System.getProperty("os.name");
        String osNameLowerCase = osName.toLowerCase(Locale.ROOT);
        if (osNameLowerCase.startsWith("mac") || osNameLowerCase.startsWith("darwin")) {
            return Platform.MAC;
        } else if (osNameLowerCase.startsWith("wind")) {
            return Platform.WINDOWS;
        } else if (osNameLowerCase.startsWith("linux")) {
            return Platform.GTK;
        }
        
        throw new Error("Unsupported OS: " + osName);
	}
}
