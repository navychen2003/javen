package org.javenstudio.common.entitydb;

import org.javenstudio.common.entitydb.db.EntityObserver;
import org.javenstudio.common.entitydb.db.EntitySetObserver;
import org.javenstudio.common.util.EntityCursor;

public interface ICursor<K extends IIdentity, T extends IEntity<K>> extends EntityCursor<T> {

	public ITable<K,T> getTable(); 
	public IQuery<K,T> getQuery(); 
	
	public void registerEntitySetObserver(EntitySetObserver observer); 
	public void unregisterEntitySetObserver(EntitySetObserver observer); 
	
	public void registerEntityObserver(EntityObserver observer); 
	public void unregisterEntityObserver(EntityObserver observer); 
	
	public void notifyEntitySetChange(); 
	public void notifyEntityChange(T data, int change); 
	public void notifyEntitiesChange(int count, int change); 
	public void setNotifyEntityMatcher(IEntityMatcher<K,T> matcher); 
	
	public boolean requery(); 
	public boolean requery(boolean notify); 
	public boolean isClosed(); 
	public void close(); 
	
	public int getCount(); 
	public int getPosition(); 
	public T entityAt(int position); 
	public T get(); 
	public T next(); 
	
	public boolean move(int offset);
	public boolean moveToPosition(int position);
	public boolean moveToFirst();
	public boolean moveToLast();
	public boolean moveToNext();
	public boolean moveToPrevious();
	
	public boolean isFirst(); 
	public boolean isLast(); 
	public boolean hasNext(); 
	
}
