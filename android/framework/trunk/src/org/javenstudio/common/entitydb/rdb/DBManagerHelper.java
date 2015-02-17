package org.javenstudio.common.entitydb.rdb;

import org.javenstudio.common.entitydb.IDatabase;
import org.javenstudio.common.entitydb.IDatabaseManager;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.db.EntityManager;

public abstract class DBManagerHelper implements IDatabaseManager {

	protected abstract DBOpenHelper getDBOpenHelperInstance();
	
	protected Database getDatabaseInstance() { 
		return getDBOpenHelperInstance().getWritableDatabase();
	}
	
	public void lock() { getDatabaseInstance().lock(); } 
	public void unlock() { getDatabaseInstance().unlock(); } 
	
	public void beginTransaction() { getDatabaseInstance().beginTransaction(); } 
	public void endTransaction() { getDatabaseInstance().endTransaction(); } 
	public void setTransactionSuccessful() { getDatabaseInstance().setTransactionSuccessful(); } 
	public boolean inTransaction() { return getDatabaseInstance().inTransaction(); } 
	
	public void close() { getDatabaseInstance().close(); } 
	
	public boolean isReadOnly() { return getDatabaseInstance().isReadOnly(); } 
	public boolean isOpen() { return getDatabaseInstance().isOpen(); } 
	
	public int getVersion() { return getDatabaseInstance().getVersion(); } 
	public void setVersion(int version) { getDatabaseInstance().setVersion(version); } 
	
	@Override
	public <K extends IIdentity, T extends IEntity<K>> 
			EntityManager<K,T> createEntityManager(IDatabase db, String tableName, boolean cacheEnabled) { 
		return new EntityManager<K,T>(db, getDBOpenHelperInstance(), tableName, cacheEnabled);
	}
	
}
