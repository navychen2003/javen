package org.javenstudio.panda.util;

import java.io.IOException;

/**
 * Interface for a component that needs to be initialized by
 * an implementation of {@link ResourceLoader}.
 * 
 * @see ResourceLoader
 */
public interface ResourceLoaderAware {

	/**
	 * Initializes this component with the provided ResourceLoader
	 * (used for loading classes, files, etc).
	 */
	public void inform(ResourceLoader loader) throws IOException;
	
}
