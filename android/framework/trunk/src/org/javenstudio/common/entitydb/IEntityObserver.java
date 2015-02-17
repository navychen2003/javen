package org.javenstudio.common.entitydb;

public interface IEntityObserver {

	public void onChange(IEntity<? extends IIdentity> data, int change); 
	
}
