package org.javenstudio.common.entitydb;

public interface IEntityManager<K extends IIdentity, T extends IEntity<K>> {

	public boolean isTableCached(); 
	
	public String getStreamPath(String fieldName, K id); 
	public int scanStreamFields(T data); 
	
	public void clear(); 
	public int count(); 
	public boolean contains(K id); 
	
	public T query(K id); 
	public T[] query(IQuery<K,T> query); 
	public T[] queryAll(); 
	public int queryCount(IQuery<K,T> query); 
	
	public K insert(T data); 
	public boolean update(K id, T data); 
	public boolean remove(K id); 
	public int deleteMany(IQuery<K,T> query); 
	
}
