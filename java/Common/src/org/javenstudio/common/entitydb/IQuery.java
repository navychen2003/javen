package org.javenstudio.common.entitydb;

import java.util.Comparator;

public interface IQuery<K extends IIdentity, T extends IEntity<K>> {

	public ICursor<K,T> query(); 
	public int queryCount(); 
	public boolean match(T data); 
	
	public Comparator<T> getComparator(); 
	public IWhereClause<K,T> getWhereClause(); 
	
	public int deleteMany(); 
	public int deleteManyIndividually(); 
	
	public String toSQL(); 
	public String toWhereSQL(); 
	
}
