package org.javenstudio.common.entitydb;

public interface IIdentityGenerator<K extends IIdentity> {

	public K newIdentity(Object value);
	
}
