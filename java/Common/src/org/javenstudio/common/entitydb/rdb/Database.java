package org.javenstudio.common.entitydb.rdb;

import java.util.Locale;
import java.util.Map;

import org.javenstudio.common.entitydb.DBException;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;

public interface Database {

	public void setLockingEnabled(boolean lockingEnabled); 
	public boolean isDbLockedByCurrentThread(); 
	public boolean isDbLockedByOtherThreads(); 
	
	public void beginTransaction(); 
	public void endTransaction(); 
	public void setTransactionSuccessful(); 
	public boolean inTransaction(); 

	public boolean yieldIfContendedSafely(); 
	public boolean yieldIfContendedSafely(long sleepAfterYieldDelay); 
	
	public void lock(); 
	public void unlock(); 
	public void close(); 
	
	public int getVersion(); 
	public void setVersion(int version); 
	
	public long getMaximumSize(); 
	public long setMaximumSize(long numBytes); 
	public long getPageSize(); 
	public void setPageSize(long numBytes); 
	
	public Map<String, String> getSyncedTables(); 
	public void markTableSyncable(String table, String deletedTable); 
	public void markTableSyncable(String table, String foreignKey, String updateTable); 
	
	public DBCursor query(boolean distinct, String table, String[] columns,
            String selection, String[] selectionArgs, String groupBy,
            String having, String orderBy, String limit); 
	
	public DBCursor rawQuery(String sql, String[] selectionArgs); 
	
	public int queryCount(String tables, String selection); 
	
	public IIdentity insert(String table, String nullColumnHack, DBValues values); 
	public IIdentity replace(String table, String nullColumnHack, DBValues initialValues); 
	
	public int delete(String table, String whereClause, String[] whereArgs); 
	public int delete(String table, IIdentity id); 
	
	public int update(String table, DBValues values, String whereClause, String[] whereArgs); 
	public int update(String table, DBValues values, IIdentity id); 
	
	public void execSQL(String sql) throws DBException; 
	public void execSQL(String sql, Object[] bindArgs) throws DBException; 
	
	public void createTable(DBTable<? extends IIdentity, ? extends IEntity<?>> table) throws DBException; 
	
	public boolean isReadOnly(); 
	public boolean isOpen(); 
	public boolean needUpgrade(int newVersion); 
	public String getPath(); 
	
	public void setLocale(Locale locale); 

}
