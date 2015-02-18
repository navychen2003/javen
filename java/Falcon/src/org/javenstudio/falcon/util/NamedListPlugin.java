package org.javenstudio.falcon.util;

import org.javenstudio.falcon.ErrorException;

/**
 * A plugin that can be initialized with a NamedList
 * 
 */
public interface NamedListPlugin {
	public void init(NamedList<?> args) throws ErrorException;
}
