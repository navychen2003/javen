package org.javenstudio.common.entitydb;

public interface IIdentity extends Comparable<Object> {

	public IIdentity clone(); 
	public String toSQL(); 
	
}
