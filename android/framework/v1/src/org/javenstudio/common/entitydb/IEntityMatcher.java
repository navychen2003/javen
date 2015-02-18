package org.javenstudio.common.entitydb;

public interface IEntityMatcher<K extends IIdentity, T extends IEntity<K>> {

	public boolean match(T data); 
	
}
