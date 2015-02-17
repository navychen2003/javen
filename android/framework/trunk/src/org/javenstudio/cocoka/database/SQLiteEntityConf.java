package org.javenstudio.cocoka.database;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import org.javenstudio.cocoka.database.SQLiteEntityDB.TEntity;
import org.javenstudio.cocoka.database.SQLiteEntityDB.TTable;
import org.javenstudio.common.entitydb.DBException;
import org.javenstudio.common.util.Logger;

public final class SQLiteEntityConf {
	private static final Logger LOG = Logger.getLogger(SQLiteEntityConf.class);

	public static class EntityTable<T extends TEntity> { 
		public final String mTableName; 
		public final Class<? extends TTable<T>> mTableClass; 
		public final Class<T> mEntityClass; 
		public final boolean mCacheEnabled; 
		
		public EntityTable(String tableName, 
				Class<? extends TTable<T>> tableClass, Class<T> entityClass, 
				boolean cacheEnabled) { 
			mTableName = tableName; 
			mTableClass = tableClass; 
			mEntityClass = entityClass; 
			mCacheEnabled = cacheEnabled; 
		}
	}
	
	private final List<EntityTable<? extends TEntity>> mEntityTables = 
			new ArrayList<EntityTable<? extends TEntity>>();
	
	private final SQLiteEntityDB.DatabaseListener mListener;
	private final Context mContext;
	private final String mDBName;
	private final int mVersion;
	private boolean mInitialized = false;
	
	public SQLiteEntityConf(Context context, String dbName, int version) { 
		this(context, dbName, version, null);
	}
	
	public SQLiteEntityConf(Context context, String dbName, int version, 
			SQLiteEntityDB.DatabaseListener listener) { 
		mListener = listener;
		mContext = context;
		mDBName = dbName;
		mVersion = version;
	}
	
	public final Context getContext() { return mContext; }
	public final String getDBName() { return mDBName; }
	public final int getVersion() { return mVersion; }
	
	SQLiteEntityDB.DatabaseListener getDatabaseListener() { 
		return mListener;
	}
	
	public synchronized <T extends TEntity> void registerTable(String tableName, 
			Class<? extends TTable<T>> tableClass, Class<T> entityClass, boolean cacheEnabled) { 
		if (tableName == null || tableClass == null || entityClass == null) 
			return;
		
		if (mInitialized) 
			throw new DBException("SQLiteEntityDB already initialized");
		
		synchronized (mEntityTables) { 
			if (LOG.isDebugEnabled()) { 
				LOG.debug("SQLiteEntityDB: register table: " + tableName 
						+ " tableClass: " + tableClass.getName() + " entityClass: " + entityClass.getName() 
						+ " cacheEnabled: " + cacheEnabled);
			}
			mEntityTables.add(new EntityTable<T>(tableName, tableClass, entityClass, cacheEnabled));
		}
	}
	
	synchronized EntityTable<? extends TEntity>[] getEntityTables() { 
		mInitialized = true;
		
		synchronized (mEntityTables) { 
			return mEntityTables.toArray(new EntityTable<?>[0]);
		}
	}
	
}
