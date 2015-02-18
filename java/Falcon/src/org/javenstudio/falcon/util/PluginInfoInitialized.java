package org.javenstudio.falcon.util;

import org.javenstudio.falcon.ErrorException;

/**
 * A plugin that can be initialized with a PluginInfo
 *
 */
public interface PluginInfoInitialized {

	public void init(PluginInfo info) throws ErrorException;

}
