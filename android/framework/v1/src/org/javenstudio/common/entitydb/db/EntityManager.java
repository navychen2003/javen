package org.javenstudio.common.entitydb.db;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.entitydb.DBException;
import org.javenstudio.common.entitydb.IDatabase;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IEntityManager;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.IQuery;
import org.javenstudio.common.entitydb.ITable;
import org.javenstudio.common.entitydb.rdb.DBCursor;
import org.javenstudio.common.entitydb.rdb.DBField;
import org.javenstudio.common.entitydb.rdb.DBOpenHelper;
import org.javenstudio.common.entitydb.rdb.DBTable;
import org.javenstudio.common.util.Logger;

public class EntityManager<K extends IIdentity, T extends IEntity<K>> implements IEntityManager<K,T> {
	private static Logger LOG = Logger.getLogger(EntityManager.class);
	
	private Map<K, WeakReference<T> > mEntityRefs = null; 
	private final IDatabase mEntityDatabase;
	private final DBOpenHelper mDBOpenHelper;
	private final String mTableName; 
	private final boolean mCacheEnabled; 
	private ITable<K,T> mEntityTable = null; 
	private DBTable<K,T> mDBTable = null; 
	
	public EntityManager(IDatabase db, DBOpenHelper helper, String tableName, boolean cacheEnabled) {
		mEntityDatabase = db;
		mDBOpenHelper = helper;
		mTableName = tableName; 
		mCacheEnabled = cacheEnabled; 
		
		if (LOG.isDebugEnabled()) { 
			LOG.debug("EntityManager: " + getClass().getName() + " created for table: " + tableName 
					+ " cacheEnabled: " + cacheEnabled);
		}
	}
	
	protected final DBOpenHelper getDBOpenHelper() { 
		return mDBOpenHelper;
	}
	
	protected final IDatabase getEntityDatabase() { 
		return mEntityDatabase;
	}
	
	private void ensureOpen() {
		synchronized (this) {
			if (mEntityTable == null || mDBTable == null) {
				mEntityTable = getEntityDatabase().getTable(mTableName); 
				mDBTable = getDBOpenHelper().getTable(mTableName); 
				
				if (!isTableCached() && mEntityRefs == null) {
					mEntityRefs = new HashMap<K, WeakReference<T> >(); 
				}
			}
		}
	}
	
	@Override 
	public boolean isTableCached() {
		return mCacheEnabled; 
	}
	
	@Override 
	public String getStreamPath(String fieldName, IIdentity id) {
		ensureOpen(); 
		
		synchronized (this) {
			return mDBTable.getStreamPath(fieldName, id); 
		}
	}
	
	@Override 
	public void clear() { } 
	
	@Override 
	public int count() {
		ensureOpen(); 
		
		synchronized (this) {
			return mDBTable.queryCount(null); 
		}
	}
	
	@Override
	public boolean contains(K id) {
		ensureOpen(); 
		
		synchronized (this) {
			DBCursor cursor = mDBTable.queryById(id); 
			try {
				return cursor.moveToFirst(); 
			} finally {
				cursor.close(); 
			}
		}
	}
	
	@Override
	public T query(K id) {
		ensureOpen(); 
		
		synchronized (this) {
			DBCursor cursor = mDBTable.queryById(id); 
			try {
				if (cursor.moveToFirst()) 
					return newEntityFromCursor(cursor); 
				else
					return null; 
			} finally {
				cursor.close(); 
			}
		}
	}
	
	@Override 
	public K insert(T data) {
		ensureOpen(); 
		
		synchronized (this) {
			//K id = mEntityTable.newIdentity(mDBTable.insert(data)); 
			K id = mDBTable.insert(data); 
			if (id != null) 
				removeEntityReference(id); 
			
			return id; 
		}
	}
	
	@Override 
	public boolean update(K id, T data) {
		ensureOpen(); 
		
		synchronized (this) {
			boolean updated = mDBTable.update(id, data) > 0; 
			if (updated) 
				removeEntityReference(id); 
			
			return updated; 
		}
	}
	
	@Override 
	public boolean remove(K id) {
		ensureOpen(); 
		
		synchronized (this) {
			boolean deleted = mDBTable.delete(id) > 0; 
			if (deleted) 
				removeEntityReference(id); 
			
			return deleted; 
		}
	}
	
	@Override 
	public int deleteMany(IQuery<K,T> query) {
		ensureOpen(); 
		
		synchronized (this) {
			String whereClause = query.toWhereSQL(); 
			if (whereClause == null || whereClause.length() == 0) 
				throw new DBException("delete many should has where clause"); 
			
			int count = mDBTable.deleteMany(whereClause); 
			if (count > 0) 
				removeEntityReference(null); 
			
			return count; 
		}
	}
	
	@Override 
	public T[] query(IQuery<K,T> query) {
		return queryWhere(query.toWhereSQL()); 
	}
	
	@Override 
	public T[] queryAll() {
		return queryWhere(null); 
	}
	
	@Override 
	public int queryCount(IQuery<K,T> query) {
		ensureOpen(); 
		
		synchronized (this) {
			return mDBTable.queryCount(query.toWhereSQL()); 
		}
	}
	
	@SuppressWarnings({"unchecked"})
	private T[] queryWhere(String whereClause) {
		ensureOpen(); 
		
		synchronized (this) {
			DBCursor cursor = mDBTable.query(whereClause); 
			boolean res = cursor.moveToFirst(); 
			try {
				if (res) {
					int count = cursor.getCount(); 
					if (count > 0) {
						T[] contents = (T[]) Array.newInstance(mDBTable.getEntityClass(), count);
						int idx = 0; 
						
						while (res) {
							contents[idx++] = newEntityFromCursor(cursor); 
							res = cursor.moveToNext(); 
						}
						
						if (idx != count) 
							throw new DBException("fetched "+idx+" entities not equals count "+count); 
						
						return contents; 
					}
				}
			} finally {
				cursor.close(); 
			}
			return null; 
		}
	}
	
	@Override 
	public int scanStreamFields(T data) { 
		if (data == null) return 0; 
		
		return scanEntityStreamFields(mDBTable, data); 
	}
	
	private void removeEntityReference(IIdentity id) {
		synchronized (this) {
			if (mEntityRefs != null) {
				if (id != null) { 
					if (mEntityRefs.containsKey(id)) { 
						mEntityRefs.remove(id); 
						if (LOG.isDebugEnabled()) { 
							LOG.debug("EntityManager.removeEntityReference: clear table: " + 
									mEntityTable.getTableName() + " cached entity: " + id); 
						} 
					}
					
				} else { 
					mEntityRefs.clear(); 
					if (LOG.isDebugEnabled()) { 
						LOG.debug("EntityManager.removeEntityReference: clear table: " + 
								mEntityTable.getTableName() + " entity cache"); 
					} 
				}
			}
		}
	}
	
	private T newEntityFromCursor(DBCursor cursor) {
		ensureOpen(); 
		
		synchronized (this) {
			K id = newIdentityFromCursor(mEntityTable, cursor); 
			if (id == null) return null; 
			
			T entity = null; 
			if (mEntityRefs != null) {
				WeakReference<T> ref = mEntityRefs.get(id); 
				if (ref != null) 
					entity = ref.get(); 
			}
			
			if (entity == null) { 
				entity = newEntityFromCursor(mEntityTable, mDBTable, cursor, id); 
				
				if (mEntityRefs != null) {
					mEntityRefs.put(id, new WeakReference<T>(entity)); 
				}
			} 
			
			return entity; 
		}
	}
	
	public static <M extends IIdentity, E extends IEntity<M>> 
			E newEntityFromCursor(ITable<M,E> entityTable, DBTable<M,E> dbTable, DBCursor cursor) {
		return newEntityFromCursor(entityTable, dbTable, cursor, null); 
	}
	
	public static <M extends IIdentity, E extends IEntity<M>> 
			M newIdentityFromCursor(ITable<M,E> entityTable, DBCursor cursor) {
		if (entityTable == null || cursor == null || cursor.isClosed()) 
			return null; 
		
		//return entityTable.newIdentity(
		//		cursor.getLong(cursor.getColumnIndex(BaseColumns._ID)));
		
		return entityTable.newIdentity(
				cursor.getIdentityValue(entityTable.getIdentityFieldName()));
	}
	
	@SuppressWarnings({"unchecked"})
	public static <M extends IIdentity, E extends IEntity<M>> 
			E newEntityFromCursor(ITable<M,E> entityTable, DBTable<M,E> dbTable, DBCursor cursor, M id) {
		if (entityTable == null || dbTable == null || cursor == null || cursor.isClosed()) 
			return null; 
		
		E data = null; 
		try {
			data = (E)entityTable.getEntityClass().newInstance(); 
		} catch (Exception e) {
			throw new DBException("create entity error: "+e); 
		}
		
		if (id == null) 
			id = newIdentityFromCursor(entityTable, cursor); 
		
		data.setIdentity(id); 
		data.setTable(entityTable); 
		
		String[] names = dbTable.getColumnNames(); 
		for (int i=0; names != null && i < names.length; i++) {
			String name = names[i]; 
			DBField dbfield = dbTable.getField(name); 
			Field field = entityTable.getEntityField(name); 
			if (field == null || dbfield == null) 
				continue; 
			
			try {
				if (dbfield.getType() == DBField.FieldType.INTEGER) {
					long value = cursor.getLong(cursor.getColumnIndex(name)); 
					if (field.getType() == Long.class) { 
						field.set(data, new Long(value)); 
					} else if (field.getType() == Integer.class) { 
						field.set(data, new Integer((int)value)); 
					}
					
				} else if (dbfield.getType() == DBField.FieldType.FLOAT) { 
					Float value = cursor.getFloat(cursor.getColumnIndex(name)); 
					field.set(data, value); 
					
				} else if (dbfield.getType() == DBField.FieldType.DOUBLE) { 
					Double value = cursor.getDouble(cursor.getColumnIndex(name)); 
					field.set(data, value); 
					
				} else if (dbfield.getType() == DBField.FieldType.TEXT) {
					String value = cursor.getString(cursor.getColumnIndex(name)); 
					field.set(data, value); 
					
				} else if (dbfield.getType() == DBField.FieldType.BYTEARRAY) {
					byte[] value = cursor.getBlob(cursor.getColumnIndex(name)); 
					field.set(data, value); 
					
				}
				
			} catch (Exception e) {
				throw new DBException("set entity field: "+name+" error: "+e); 
			}
		}
		
		scanEntityStreamFields(dbTable, data); 
		
		if (LOG.isDebugEnabled()) { 
			LOG.debug("EntityManager.newEntityFromCursor: " + data); 
		} 
		
		return data; 
	}
	
	public static <M extends IIdentity, E extends IEntity<M>> 
			int scanEntityStreamFields(DBTable<M,E> dbTable, E data) { 
		if (dbTable == null || data == null) 
			return 0; 
		
		String[] streamNames = dbTable.getStreamFieldNames(); 
		int count = 0; 
		
		for (int i=0; streamNames != null && i < streamNames.length; i++) {
			String name = streamNames[i]; 
			String filepath = dbTable.getStreamPath(name, data.getIdentity()); 
			File file = new File(filepath); 
			data.removeStream(name); 
			if (file.exists()) {
				data.setAsStream(name, filepath); 
				count ++; 
				if (LOG.isDebugEnabled()) { 
					LOG.debug("EntityManager.newEntityFromCursor: " + 
							"find stream field(\"" + name + "\") file: " + filepath); 
				} 
			}
		}
		
		return count; 
	}
	
}
