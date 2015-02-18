package org.javenstudio.common.entitydb;

public interface IWhereClause<K extends IIdentity, T extends IEntity<K>> {

	public void bindField(ITable<K,T> table); 
	public String toSQL(); 
	public boolean match(T data); 
	
}
