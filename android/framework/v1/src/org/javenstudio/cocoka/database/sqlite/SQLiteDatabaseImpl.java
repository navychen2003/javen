package org.javenstudio.cocoka.database.sqlite;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;

import org.javenstudio.common.entitydb.DBException;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.rdb.DBCursor;
import org.javenstudio.common.entitydb.rdb.DBField;
import org.javenstudio.common.entitydb.rdb.DBTable;
import org.javenstudio.common.entitydb.rdb.DBValues;
import org.javenstudio.common.entitydb.rdb.Database;
import org.javenstudio.common.entitydb.type.LongIdentity;
import org.javenstudio.common.util.Logger;

public class SQLiteDatabaseImpl implements Database {
	private static Logger LOG = Logger.getLogger(SQLiteDatabaseImpl.class);

	/** Synchronize on this when accessing the database */
    private final ReentrantLock mLock = new ReentrantLock(true);
    
    /**
     * If set then the SQLiteDatabase is made thread-safe by using locks
     * around critical sections
     */
    private boolean mLockingEnabled = true;
    
	private final SQLiteDatabase mDatabase; 
	
	public SQLiteDatabaseImpl(SQLiteDatabase db) {
		mDatabase = db; 
	}
	
	public void setLockingEnabled(boolean lockingEnabled) {
		mDatabase.setLockingEnabled(lockingEnabled); 
		mLockingEnabled = lockingEnabled;
	}
	
	public boolean isDbLockedByCurrentThread() {
		return mDatabase.isDbLockedByCurrentThread(); 
	}
	
	public boolean isDbLockedByOtherThreads() {
		return mDatabase.isDbLockedByOtherThreads(); 
	}
	
	public void beginTransaction() {
		if (LOG.isDebugEnabled()) LOG.debug("SQLiteDatabase: beginTransaction");
		mDatabase.beginTransaction(); 
	}
	
	public void endTransaction() {
		if (LOG.isDebugEnabled()) LOG.debug("SQLiteDatabase: endTransaction");
		mDatabase.endTransaction(); 
	}
	
	public void setTransactionSuccessful() {
		if (LOG.isDebugEnabled()) LOG.debug("SQLiteDatabase: setTransactionSuccessful");
		mDatabase.setTransactionSuccessful(); 
	}
	
	public boolean inTransaction() {
		return mDatabase.inTransaction(); 
	}

	public boolean yieldIfContendedSafely() {
		return mDatabase.yieldIfContendedSafely(); 
	}
	
	public boolean yieldIfContendedSafely(long sleepAfterYieldDelay) {
		return mDatabase.yieldIfContendedSafely(sleepAfterYieldDelay); 
	}
	
	public void lock() {
		if (!mLockingEnabled) return;
        mLock.lock();
	} 
	
	public void unlock() {
		if (!mLockingEnabled) return;
		mLock.unlock();
	} 
	
	public void close() {
		mDatabase.close(); 
	}
	
	public int getVersion() {
		return mDatabase.getVersion(); 
	}
	
	public void setVersion(int version) {
		if (LOG.isDebugEnabled()) LOG.debug("SQLiteDatabase: setVersion "+version);
		mDatabase.setVersion(version); 
	}
	
	public long getMaximumSize() {
		return mDatabase.getMaximumSize(); 
	}
	
	public long setMaximumSize(long numBytes) {
		if (LOG.isDebugEnabled()) LOG.debug("SQLiteDatabase: setMaximumSize "+numBytes);
		return mDatabase.setMaximumSize(numBytes); 
	}
	
	public long getPageSize() {
		return mDatabase.getPageSize(); 
	}
	
	public void setPageSize(long numBytes) {
		if (LOG.isDebugEnabled()) LOG.debug("SQLiteDatabase: setPageSize "+numBytes);
		mDatabase.setPageSize(numBytes); 
	}
	
	public Map<String, String> getSyncedTables() {
		return mDatabase.getSyncedTables(); 
	}
	
	public void markTableSyncable(String table, String deletedTable) {
		mDatabase.markTableSyncable(table, deletedTable); 
	}
	
	public void markTableSyncable(String table, String foreignKey, String updateTable) {
		mDatabase.markTableSyncable(table, foreignKey, updateTable); 
	}
	
	//public Cursor query(boolean distinct, String table, String[] columns,
    //        String selection, String[] selectionArgs, String groupBy,
    //        String having, String orderBy, String limit) {
	//	if (Constants.isDebug()) Log.d(Constants.getTag(), "SQLiteDatabase: query: "+table);
	//	return mDatabase.query(distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit); 
	//}
	
	public DBCursor query(boolean distinct, String tables, String[] columns, 
			String selection, String[] selectionArgs, String groupBy, 
			String having, String orderBy, String limit) {
		String sql = SQLiteQueryBuilder.buildQueryString(distinct, 
				tables, columns, selection, groupBy, having, orderBy, limit); 
		return rawQuery(sql, selectionArgs); 
	}
	
	public int queryCount(String tables, String selection) {
		String sql = "SELECT count(*) FROM " + tables; 
		if (selection != null && selection.length() > 0) 
			sql += " WHERE " + selection; 
		
		Cursor cursor = rawQuerySqlite(sql, null); 
		try {
			if (cursor != null) { 
				cursor.moveToFirst(); 
				return cursor.getInt(0); 
			}
		} finally {
			if (cursor != null) 
				cursor.close(); 
		}
		
		return 0; 
	}
	
	public Cursor rawQuerySqlite(String sql, String[] selectionArgs) {
		if (LOG.isDebugEnabled()) LOG.debug("SQLiteDatabase: rawQuery: "+sql);
		return mDatabase.rawQuery(sql, selectionArgs); 
	}
	
	public DBCursor rawQuery(String sql, String[] selectionArgs) {
		return new SQLiteCursor(rawQuerySqlite(sql, selectionArgs));
	}
	
	public IIdentity insert(String table, String nullColumnHack, DBValues values) {
		if (LOG.isDebugEnabled()) LOG.debug("SQLiteDatabase: insert: "+table+" values: "+values);
		return new LongIdentity( 
				mDatabase.insert(table, nullColumnHack, 
						SQLiteHelper.toContentValues(values))); 
	}
	
	public IIdentity replace(String table, String nullColumnHack, DBValues initialValues) {
		if (LOG.isDebugEnabled()) LOG.debug("SQLiteDatabase: replace: "+table+" values: "+initialValues);
		return new LongIdentity( 
				mDatabase.replace(table, nullColumnHack, 
						SQLiteHelper.toContentValues(initialValues))); 
	}
	
	public int delete(String table, String whereClause, String[] whereArgs) {
		if (LOG.isDebugEnabled()) LOG.debug("SQLiteDatabase: delete: "+table+" whereClause: "+whereClause);
		return mDatabase.delete(table, whereClause, whereArgs); 
	}
	
	public int delete(String table, IIdentity id) {
		return delete(table, BaseColumns._ID+"="+id, null); 
	}
	
	public int update(String table, DBValues values, String whereClause, String[] whereArgs) {
		if (values.size() == 0) {
			LOG.warn("SQLiteDatabase: update: "+table+" whereClause: "+whereClause+" empty values, do nothing"); 
			return 0; 
		}
		if (LOG.isDebugEnabled()) LOG.debug("SQLiteDatabase: update: "+table+" whereClause: "+whereClause+" values: "+values);
		return mDatabase.update(table, SQLiteHelper.toContentValues(values), whereClause, whereArgs); 
	}
	
	public int update(String table, DBValues values, long id) {
		return update(table, values, BaseColumns._ID+"="+id, null); 
	}
	
	public int update(String table, DBValues values, IIdentity id) {
		return update(table, values, BaseColumns._ID+"="+id, null); 
	}
	
	public void execSQL(String sql) throws SQLException {
		if (LOG.isDebugEnabled()) LOG.debug("SQLiteDatabase: execSQL: "+sql);
		mDatabase.execSQL(sql); 
	}
	
	public void execSQL(String sql, Object[] bindArgs) throws SQLException {
		if (LOG.isDebugEnabled()) LOG.debug("SQLiteDatabase: execSQL: "+sql);
		mDatabase.execSQL(sql, bindArgs); 
	}
	
	public boolean isReadOnly() {
		return mDatabase.isReadOnly(); 
	}
	
	public boolean isOpen() {
		return mDatabase.isOpen(); 
	}
	
	public boolean needUpgrade(int newVersion) {
		return mDatabase.needUpgrade(newVersion); 
	}
	
	public String getPath() {
		return mDatabase.getPath(); 
	}
	
	public void setLocale(Locale locale) {
		mDatabase.setLocale(locale); 
	}

	@SuppressWarnings("unchecked")
	public void createTable(DBTable<? extends IIdentity, ? extends IEntity<?>> entityTable) throws SQLException {
		if (!(entityTable instanceof SQLiteTable)) 
			throw new DBException("Entity DBTable isn't a SQLiteTable instance");
		
		final SQLiteTable<? extends IEntity<?>> table = (SQLiteTable<? extends IEntity<?>>)entityTable;
		
		StringBuilder sbuf = new StringBuilder(); 
		
		sbuf.append("CREATE TABLE "); 
		sbuf.append(table.getTableName()); 
		sbuf.append(" (");
		
		for (int i=0; i < table.getFieldList().size(); i++) {
			DBField field = table.getFieldList().get(i).dbField; 
			if (i > 0) sbuf.append(", "); 
			
			sbuf.append(field.getName()); 
			
			String stype = SQLiteHelper.fieldTypeToString(field.getType()); 
			if (stype != null && stype.length() > 0) {
				sbuf.append(' '); 
				sbuf.append(stype); 
			}
			
			DBField.FieldProperty[] props = field.getProperties(); 
			for (int j=0; props != null && j < props.length; j++) {
				DBField.FieldProperty prop = props[j]; 
				String sprop = SQLiteHelper.fieldPropertyToString(prop); 
				if (sprop != null && sprop.length() > 0) {
					sbuf.append(' '); 
					sbuf.append(sprop); 
				}
			}
		}
		
		sbuf.append(");");
		
		execSQL(sbuf.toString()); 
	}
}
