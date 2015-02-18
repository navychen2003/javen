package org.javenstudio.provider.app;

public class BaseEntry {

	public static int parseInt(String str) { 
    	try { 
    		return Integer.parseInt(str);
    	} catch (Throwable e) { 
    		return 0;
    	}
    }
	
    public static long parseLong(String str) { 
    	try { 
    		return Long.parseLong(str);
    	} catch (Throwable e) { 
    		return 0;
    	}
    }
    
    public static String getString(String str) { 
    	return str != null ? str : "";
    }
	
}
