package org.javenstudio.panda.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * Abstraction for loading resources (streams, files, and classes).
 */
public interface ResourceLoader {

	/**
	 * Opens a named resource
	 */
	public InputStream openResource(String resource) throws IOException;
  
	/**
	 * Creates a class of the name and expected type
	 */
	// TODO: fix exception handling
	public <T> T newInstance(String cname, Class<T> expectedType) 
			throws ClassNotFoundException;
	
}