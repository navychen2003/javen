package org.javenstudio.common.entitydb;

public interface IEntityMap<K extends IIdentity, T extends IEntity<K>> {

	public void clear();
	public int size();
	
	public boolean containsKey(K key);
	public T get(K key);
	
	public T[] get(IQuery<K,T> query);
	public int count(IQuery<K,T> query);
	public int remove(IQuery<K,T> query);
	
	public T put(K key, T value);
	public T remove(K key);
	
	public T[] toArray();
	
}
