package org.javenstudio.falcon.util;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.PluginFactory;

public abstract class PluginFactoryHolder {

	private PluginFactory mFactory = null;
	
	public final PluginFactory getFactoryOrNull() { 
		return mFactory;
	}
	
	public final PluginFactory getFactory() throws ErrorException { 
		if (mFactory == null) 
			mFactory = createFactory();
		return mFactory;
	}
	
	protected abstract PluginFactory createFactory() throws ErrorException;
	
}
