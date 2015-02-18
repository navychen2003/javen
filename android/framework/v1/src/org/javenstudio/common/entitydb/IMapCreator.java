package org.javenstudio.common.entitydb;

public interface IMapCreator<K extends IIdentity, T extends IEntity<K>> {

	public IEntityMap<K,T> createEntityMap(ITable<K,T> table);
	
}
