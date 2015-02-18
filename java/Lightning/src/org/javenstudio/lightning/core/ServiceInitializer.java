package org.javenstudio.lightning.core;

import org.javenstudio.falcon.ErrorException;

public abstract class ServiceInitializer {

	public abstract String getServiceName();
	
	public CoreService initialize(CoreContainers containers, String name) 
			throws ErrorException { 
		return null;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "{name=" + getServiceName() + "}";
	}
	
}
