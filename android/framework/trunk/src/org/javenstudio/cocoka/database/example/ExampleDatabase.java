package org.javenstudio.cocoka.database.example;

import android.content.Context;

import org.javenstudio.cocoka.database.SQLiteEntityConf;
import org.javenstudio.cocoka.database.SQLiteEntityDB;
import org.javenstudio.common.entitydb.IDatabase;

public class ExampleDatabase extends SQLiteEntityDB {

	public static final String DB_NAME = "sqlite.db"; 
	public static final int DB_VERSION = 1; 
	
	private static SQLiteEntityConf sEntityConf = null;
	
	static synchronized SQLiteEntityConf getSQLiteEntityConf() { 
		if (sEntityConf == null) 
			throw new RuntimeException("SQLiteEntityConf not initialized");
		return sEntityConf;
	}
	
	public static synchronized void initDatabase(Context context) { 
		if (sEntityConf != null) return;
		
		SQLiteEntityConf conf = new SQLiteEntityConf(context, DB_NAME, DB_VERSION);
		conf.registerTable(ExampleEntity.Table.TABLE_NAME, ExampleEntity.Table.class, ExampleEntity.class, true);
		// add other tables
		
		sEntityConf = conf;
	}
	
	public static IDatabase getDatabase() {
		return SQLiteEntityDB.getDatabase(ExampleDatabase.class);
	}
	
	public static <E extends TEntity> E queryEntity(Class<E> entityClass, long id) { 
		return SQLiteEntityDB.queryEntity(ExampleDatabase.class, entityClass, id); 
	}
	
	public ExampleDatabase() {}
	
	@Override
	protected SQLiteEntityConf getEntityConf() { 
		return getSQLiteEntityConf();
	}
	
}
