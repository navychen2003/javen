package org.javenstudio.cocoka.storage.fs;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException; 
import java.io.IOException; 
import java.util.Map; 
import java.util.HashMap; 
import java.net.URI; 

import org.javenstudio.common.util.Logger;

public class FileSystems {
	private static Logger LOG = Logger.getLogger(FileSystems.class);
	
	private static Map<String, Class<?>> sFileSystems = new HashMap<String, Class<?>>(); 
	
	static {
		register(LocalFileSystem.LOCAL_SCHEME, LocalFileSystem.class.getName()); 
	}

	public static void register(String scheme, String className) {
		if (scheme == null || scheme.length() == 0 || 
			className == null || className.length() == 0) 
			return; 
		
		scheme = scheme.toLowerCase(); 
		
		try {
            Class<?> clazz = Class.forName(className);
            
            if (!IFileSystem.class.isAssignableFrom(clazz)) 
            	throw new ClassNotFoundException(className + " is not a IFileSystem class"); 
            
            synchronized (sFileSystems) {
            	sFileSystems.put(scheme, clazz); 
            }
            
        } catch (ClassNotFoundException ex) {
        	LOG.warn(className + " could not be loaded", ex); 
        }
	}
	
	public static IFileSystem get(String scheme, URI uri) throws IOException {
		if (scheme == null || scheme.length() == 0) 
			scheme = "file"; 
		
		scheme = scheme.toLowerCase(); 
		
		Class<?> clazz = null; 
		
		synchronized (sFileSystems) {
			clazz = sFileSystems.get(scheme); 
		}
		
		if (clazz == null)
			throw new IOException("filesystem for scheme: "+scheme+" not registered"); 
		
		try {
			Method method = clazz.getDeclaredMethod("get", new Class[]{URI.class});
			IFileSystem fs = (IFileSystem)method.invoke(clazz, uri);
			
			if (fs == null) 
				throw new IOException("could not get filesystem for "+uri); 
			
			return fs; 
			
		} catch (NoSuchMethodException e) {
			throw new IOException(
            		clazz + " could not be instantiated" + e);
        } catch (InvocationTargetException ex) {
            throw new IOException(
            		clazz + " could not be instantiated" + ex);
        } catch (IllegalAccessException ex) {
            throw new IOException(
            		clazz + " could not be instantiated" + ex);
		}
	}
	
}
