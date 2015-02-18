package org.javenstudio.common.entitydb;

import org.javenstudio.common.entitydb.db.EntityManager;

public interface IDatabaseManager {

	public <K extends IIdentity, T extends IEntity<K>> 
		EntityManager<K,T> createEntityManager(IDatabase db, String tableName, boolean cacheEnabled); 
	
	public void lock(); 
	public void unlock(); 
	
	public void beginTransaction(); 
	public void endTransaction(); 
	public void setTransactionSuccessful(); 
	public boolean inTransaction(); 
	
	public void close(); 
	
	public boolean isReadOnly(); 
	public boolean isOpen(); 
	
	public int getVersion(); 
	public void setVersion(int version); 
	
}
