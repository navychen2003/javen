package org.javenstudio.lightning.core;

import org.javenstudio.falcon.ErrorException;

public interface CoreAware {

	public void inform(Core core) throws ErrorException;
	
}
