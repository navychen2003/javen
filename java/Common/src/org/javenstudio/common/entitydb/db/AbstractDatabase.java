package org.javenstudio.common.entitydb.db;

import org.javenstudio.common.entitydb.DBException;
import org.javenstudio.common.entitydb.IDatabase;
import org.javenstudio.common.entitydb.IDatabaseManager;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.IStreamStore;
import org.javenstudio.common.entitydb.ITable;

public abstract class AbstractDatabase implements IDatabase {
	
	private final IDatabaseManager mDatabaseManager; 
	private final String mDatabaseName; 
	private IStreamStore mStore = null; 
	
	public AbstractDatabase(String dbname, IDatabaseManager manager) {
		mDatabaseManager = manager; 
		mDatabaseName = dbname; 
	}
	
	@Override
	public final String getDatabaseName() {
		return mDatabaseName; 
	}
	
	@Override
	public final IDatabaseManager getDatabaseManager() { 
		return mDatabaseManager; 
	}
	
	public final synchronized void setStreamStore(IStreamStore store) { 
		if (mStore != null) 
			throw new DBException("stream store already set");
		
		if (store == null) 
			throw new DBException("input stream store is null");
		
		if (store != mStore) 
			mStore = store;
	}
	
	@Override
	public final synchronized IStreamStore getStreamStore() { 
		return mStore; 
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <K extends IIdentity> K insert(IEntity<K> data) {
		if (data == null) return null; 
		ITable<K, ? extends IEntity<K>> table = ((ITable<K, ? extends IEntity<K>>)getTable(data.getClass())); 
		return table.insertWithCheck(data); 
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <K extends IIdentity> K update(IEntity<K> data) {
		if (data == null) return null; 
		ITable<K, ? extends IEntity<K>> table = ((ITable<K, ? extends IEntity<K>>)getTable(data.getClass())); 
		return table.updateWithCheck(data); 
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <K extends IIdentity> boolean delete(IEntity<K> data) {
		if (data == null) return false; 
		return getTable(data.getClass()).delete(data.getIdentity()); 
	}
	
	@Override
	public void beginTransaction() {
		if (mDatabaseManager != null) 
			mDatabaseManager.beginTransaction(); 
	} 
	
	@Override
	public void endTransaction() {
		if (mDatabaseManager != null) 
			mDatabaseManager.endTransaction(); 
	} 
	
	@Override
	public void setTransactionSuccessful() {
		if (mDatabaseManager != null) 
			mDatabaseManager.setTransactionSuccessful(); 
	} 
	
	@Override
	public boolean inTransaction() { 
		if (mDatabaseManager != null) 
			return mDatabaseManager.inTransaction(); 
		return false; 
	} 
	
	@Override
	public void close() { 
		if (mDatabaseManager != null) 
			mDatabaseManager.close(); 
	} 
	
	@Override
	public boolean isReadOnly() { 
		if (mDatabaseManager != null) 
			return mDatabaseManager.isReadOnly(); 
		return false; 
	} 
	
	@Override
	public boolean isOpen() { 
		if (mDatabaseManager != null) 
			return mDatabaseManager.isOpen(); 
		return true; 
	} 
	
	@Override
	public int getVersion() { 
		if (mDatabaseManager != null) 
			return mDatabaseManager.getVersion(); 
		return 0; 
	} 
	
	@Override
	public void setVersion(int version) {
		if (mDatabaseManager != null) 
			mDatabaseManager.setVersion(version); 
	} 
	
}
