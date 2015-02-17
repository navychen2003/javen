package org.javenstudio.cocoka.database;

import android.content.Context;

import org.javenstudio.common.entitydb.DBException;
import org.javenstudio.common.entitydb.EntityException;
import org.javenstudio.common.entitydb.ICursor;
import org.javenstudio.common.entitydb.IDatabase;
import org.javenstudio.common.entitydb.db.DBFactory;
import org.javenstudio.common.entitydb.db.Databases;
import org.javenstudio.common.entitydb.rdb.DBManagerHelper;
import org.javenstudio.common.entitydb.rdb.DBOpenHelper;
import org.javenstudio.common.entitydb.rdb.Database;
import org.javenstudio.common.entitydb.type.LongIdentity;
import org.javenstudio.common.entitydb.wrapper.Cursor;
import org.javenstudio.common.entitydb.wrapper.Query;
import org.javenstudio.common.entitydb.wrapper.Updater;
import org.javenstudio.common.util.Logger;
import org.javenstudio.cocoka.database.sqlite.SQLiteEntityDatabase;
import org.javenstudio.cocoka.database.sqlite.SQLiteDatabaseFactory;
import org.javenstudio.cocoka.database.sqlite.SQLiteEntity;
import org.javenstudio.cocoka.database.sqlite.SQLiteOpenHelper;
import org.javenstudio.cocoka.database.sqlite.SQLiteQuery;
import org.javenstudio.cocoka.database.sqlite.SQLiteTable;

public abstract class SQLiteEntityDB implements DBFactory {
	private static Logger LOG = Logger.getLogger(SQLiteEntityDB.class);

	public static interface DatabaseListener { 
		public void onDatabaseCreate(SQLiteOpenHelper helper, Database db);
		public void onDatabaseUpgrade(SQLiteOpenHelper helper, Database db, int oldVersion, int newVersion);
		public void onDatabaseOpen(SQLiteOpenHelper helper, Database db);
	}
	
	public static IDatabase getDatabase(Class<? extends SQLiteEntityDB> dbClass) {
		return Databases.getWritableDatabase(dbClass.getName()); 
	}
	
	public SQLiteEntityDB() {} 
	
	protected abstract SQLiteEntityConf getEntityConf();
	
	private SQLiteEntityConf getConf() { 
		SQLiteEntityConf conf = getEntityConf();
		if (conf == null) 
			throw new DBException("SQLiteEntityConf is null");
		
		return conf;
	}
	
	public final Context getContext() { return getConf().getContext(); }
	public final String getDatabaseName() { return getConf().getDBName(); }
	public final String getDatabasePath() { return "/" + getDatabaseName(); }
	public final int getDatabaseVersion() { return getConf().getVersion(); }
	
	@Override
	public synchronized IDatabase openWritableDatabase() {
		final Context context = getContext();
		final String dbName = getDatabaseName(); 
		final int version = getDatabaseVersion(); 
		
		if (context == null || dbName == null || dbName.length() == 0) 
			throw new DBException("SQLiteEntityDB cannot open with null Context or dbName");
		
		try { 
			if (LOG.isDebugEnabled()) { 
				LOG.debug("SQLiteEntityDB: openWritableDatabase: " + getDatabaseName() 
						+ " dbName: " + dbName + " version: " + version);
			}
			
			return new SQLiteEntityDatabase(getDatabaseName(), 
					createDBManagerHelper(context, this, dbName, version)); 
			
		} catch (Throwable e) { 
			throw new DBException("open database: "+getDatabaseName()+" failed: "+e.toString(), e); 
		}
	}
	
	@Override
	public IDatabase openReadableDatabase() {
		return openWritableDatabase(); 
	}
	
	SQLiteEntityConf.EntityTable<? extends TEntity>[] getEntityTables() { 
		return getConf().getEntityTables();
	}
	
	@Override
    public synchronized void onOpenDatabase(IDatabase db) {
		final SQLiteEntityConf.EntityTable<? extends TEntity>[] tables = getEntityTables();
		
		for (int i=0; tables != null && i < tables.length; i++) { 
			SQLiteEntityConf.EntityTable<? extends TEntity> table = tables[i]; 
			if (table == null) 
				throw new DBException("EntityTable config is null");
			
			db.createTable(table.mTableName, table.mEntityClass, 
					Constants.IDENTITY_FIELDNAME, null, null, table.mCacheEnabled); 
		}
    }
	
	static DBManagerHelper createDBManagerHelper(
			final Context context, final SQLiteEntityDB db, final String dbName, final int version) { 
		final TDBOpenHelper helper = 
				newDBOpenHelper(TDBOpenHelper.class, 
						new TDatabaseFactory(context, db), dbName, version); 
		
		return new DBManagerHelper() { 
				@Override
				protected DBOpenHelper getDBOpenHelperInstance() { 
					return helper; 
				}
			};
	}
	
	static class TDatabaseFactory extends SQLiteDatabaseFactory { 
		private final SQLiteEntityDB mEntityDB;
		
		public TDatabaseFactory(Context context, SQLiteEntityDB db) { 
			super(context);
			mEntityDB = db;
		}
		
		public final SQLiteEntityDB getEntityDB() { 
			return mEntityDB;
		}
	}
	
	public static class TDBOpenHelper extends SQLiteOpenHelper {
		public TDBOpenHelper(SQLiteDatabaseFactory factory, String dbName, int version) {
			super(factory, dbName, version); 
			
			final TDatabaseFactory dbFactory = (TDatabaseFactory)factory;
			final SQLiteEntityDB entityDB = dbFactory.getEntityDB();
			final SQLiteEntityConf.EntityTable<? extends TEntity>[] tables = entityDB.getEntityTables();
			
			synchronized (entityDB) { 
				for (int i=0; tables != null && i < tables.length; i++) { 
					SQLiteEntityConf.EntityTable<? extends TEntity> table = tables[i]; 
					if (table == null) 
						throw new DBException("EntityTable config is null");
					
					registerTable(table.mTableName, newTable(table.mTableClass, this)); 
				}
			}
		}
		
		@Override
		public void onCreate(Database db) {
			super.onCreate(db); 
			
			final TDatabaseFactory dbFactory = (TDatabaseFactory)getFactory();
			final DatabaseListener listener = dbFactory.getEntityDB().getConf().getDatabaseListener();
			if (listener != null) 
				listener.onDatabaseCreate(this, db);
		}
		
		@Override
		public void onUpgrade(Database db, int oldVersion, int newVersion) {
			super.onUpgrade(db, oldVersion, newVersion);
			
			final TDatabaseFactory dbFactory = (TDatabaseFactory)getFactory();
			final DatabaseListener listener = dbFactory.getEntityDB().getConf().getDatabaseListener();
			if (listener != null) 
				listener.onDatabaseUpgrade(this, db, oldVersion, newVersion);
		}
		
		@Override
		public void onOpen(Database db) { 
			super.onOpen(db);
			
			final TDatabaseFactory dbFactory = (TDatabaseFactory)getFactory();
			final DatabaseListener listener = dbFactory.getEntityDB().getConf().getDatabaseListener();
			if (listener != null) 
				listener.onDatabaseOpen(this, db);
		}
	}
	
	public static abstract class TTable<T extends TEntity> extends SQLiteTable<T> { 
		public static final String _ID = Constants.IDENTITY_FIELDNAME; 
		
		public TTable(DBOpenHelper helper, String tableName, Class<T> clazz) { 
    		super(helper, tableName, clazz); 
    	}
	}
	
	public static abstract class TEntity extends SQLiteEntity {
		public TEntity() {
			super(); 
		}
		
		public TEntity(long id) {
			super(id); 
		}
	}
	
	public static abstract class TUpdater extends Updater { 
		public TUpdater(IDatabase db) {
			super(db);
		}
		
		@Override
		protected TEntity[] getEntities() { return null; }
		
		public void commitUpdate() { 
			try {
				saveOrUpdate();
			} catch (EntityException e) {
				throw new DBException("entity update error", e);
			}
		}
		
		public void commitDelete() { 
			try {
				delete();
			} catch (EntityException e) {
				throw new DBException("entity delete error", e);
			}
		}
	}
	
	public static class TCursor<T extends TEntity> extends Cursor<LongIdentity, T> { 
		public TCursor(ICursor<LongIdentity, T> cursor) { 
			super(cursor);
		}
	}
	
	public static abstract class TQuery<T extends TEntity> extends Query<LongIdentity, T> { 
		public TQuery(IDatabase db, Class<T> entityClass) {
			super(new SQLiteQuery<T>(db, entityClass));
		}
		
		public final TCursor<T> query() { 
			return new TCursor<T>(queryCursor());
		}
	}
	
	public static <E extends TEntity> E queryEntity(Class<? extends SQLiteEntityDB> dbClass, Class<E> entityClass, long id) { 
		if (id <= 0) return null;
		return SQLiteTable.getTable(getDatabase(dbClass), entityClass).query(new LongIdentity(id)); 
	}
	
    static TDBOpenHelper newDBOpenHelper(Class<?> clazz, SQLiteDatabaseFactory factory, String dbName, int version) {
    	if (clazz == null || factory == null) 
    		throw new DBException("DBOpenHelper class or SQLiteDatabaseFactory is null");
    	if (dbName == null || dbName.length() == 0 || version < 0) 
    		throw new DBException("Database name: "+dbName+" version: "+version+" input error");
    	
    	if (LOG.isDebugEnabled()) { 
    		LOG.debug("SQLiteEntityDB: create DBOpenHelper: " + clazz.getName() 
    				+ " dbName: " + dbName + " version: " + version);
    	}
    	
        Object o = null;
        try {
            // and invoke "newInstance" class method and instantiate store object.
            java.lang.reflect.Constructor<?> m =
                clazz.getConstructor(SQLiteDatabaseFactory.class, String.class, int.class);
            o = m.newInstance(factory, dbName, version);
        } catch (Exception e) {
            throw new DBException("can not instantiate TDBOpenHelper object for " + clazz, e);
        }
        
        if (!(o instanceof TDBOpenHelper)) {
            throw new DBException(clazz + " create incompatible object");
        }
        
        return (TDBOpenHelper) o;
    }
	
    static TTable<?> newTable(Class<?> clazz, TDBOpenHelper helper) {
    	if (clazz == null || helper == null) 
    		throw new DBException("Table class or TDBOpenHelper is null");
    	
        Object o = null;
        try {
            // and invoke "newInstance" class method and instantiate store object.
            java.lang.reflect.Constructor<?> m =
                clazz.getConstructor(TDBOpenHelper.class);
            o = m.newInstance(helper);
        } catch (Exception e) {
            throw new DBException("can not instantiate table object for " + clazz, e);
        }
        
        if (!(o instanceof TTable)) {
            throw new DBException(clazz + " create incompatible object");
        }
        
        return (TTable<?>) o;
    }
    
}
