package org.javenstudio.android.entitydb;

import android.content.Context;

import org.javenstudio.cocoka.database.SQLiteEntityConf;
import org.javenstudio.cocoka.database.SQLiteEntityDB;
import org.javenstudio.cocoka.database.sqlite.SQLiteOpenHelper;
import org.javenstudio.common.entitydb.IDatabase;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.ITable;
import org.javenstudio.common.entitydb.rdb.Database;
import org.javenstudio.common.entitydb.type.LongIdentity;
import org.javenstudio.common.util.Logger;

public class TDefaultDB extends SQLiteEntityDB {
	private static final Logger LOG = Logger.getLogger(TDefaultDB.class);
	
	public static final String DB_NAME = "default.db"; 
	public static final int DB_VERSION = 2; 
	
	private static SQLiteEntityConf sEntityConf = null;
	
	static synchronized SQLiteEntityConf getSQLiteEntityConf() { 
		if (sEntityConf == null) 
			throw new RuntimeException("SQLiteEntityConf not initialized");
		return sEntityConf;
	}
	
	//public static void initDatabase(Context context) { 
	//	initDatabase(context, new DefaultListener());
	//}
	
	public static synchronized void initDatabase(Context context, IDatabaseListener listener) { 
		if (sEntityConf != null) return;
		sEntityConf = listener.createEntityConf(context);
	}
	
	static class DefaultListener implements IDatabaseListener {

		@Override
		public SQLiteEntityConf createEntityConf(Context context) { 
			SQLiteEntityConf conf = new SQLiteEntityConf(context, DB_NAME, DB_VERSION, this);
			
			conf.registerTable(THost.Table.TABLE_NAME, THost.Table.class, THost.class, true);
			conf.registerTable(TAccount.Table.TABLE_NAME, TAccount.Table.class, TAccount.class, true);
			conf.registerTable(TUpload.Table.TABLE_NAME, TUpload.Table.class, TUpload.class, true);
			conf.registerTable(TDownload.Table.TABLE_NAME, TDownload.Table.class, TDownload.class, true);
			conf.registerTable(TFetch.Table.TABLE_NAME, TFetch.Table.class, TFetch.class, true);
			
			return conf;
		}
		
		@Override
		public void onDatabaseCreate(SQLiteOpenHelper helper, Database db) {
			if (LOG.isDebugEnabled()) LOG.debug("onDatabaseCreate: db=" + db);
		}

		@Override
		public void onDatabaseUpgrade(SQLiteOpenHelper helper, Database db,
				int oldVersion, int newVersion) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("onDatabaseCreate: db=" + db + " oldVersion=" + oldVersion 
						+ " newVersion=" + newVersion);
			}
			
			if (oldVersion == 1 && newVersion == 2) { 
				db.createTable(helper.getTable(THost.Table.TABLE_NAME));
				db.createTable(helper.getTable(TAccount.Table.TABLE_NAME));
				db.createTable(helper.getTable(TDownload.Table.TABLE_NAME));
			}
		}

		@Override
		public void onDatabaseOpen(SQLiteOpenHelper helper, Database db) {
			if (LOG.isDebugEnabled()) LOG.debug("onDatabaseOpen: db=" + db);
		}
	}
	
	public static IDatabase getDatabase() {
		return SQLiteEntityDB.getDatabase(TDefaultDB.class);
	}
	
	public static <T extends IEntity<LongIdentity>> ITable<LongIdentity, T> getTable(Class<T> entityClass) { 
		return getDatabase().getTable(entityClass);
	}
	
	public static <E extends TEntity> E queryEntity(Class<E> entityClass, long id) { 
		return SQLiteEntityDB.queryEntity(TDefaultDB.class, entityClass, id); 
	}
	
	public TDefaultDB() {}
	
	@Override
	protected SQLiteEntityConf getEntityConf() { 
		return getSQLiteEntityConf();
	}
	
}
