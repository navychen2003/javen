package org.javenstudio.cocoka.database.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.javenstudio.common.entitydb.rdb.Database;
import org.javenstudio.common.entitydb.rdb.DatabaseFactory;
import org.javenstudio.common.util.Logger;

public class SQLiteDatabaseFactory implements DatabaseFactory {
	private static Logger LOG = Logger.getLogger(SQLiteDatabaseFactory.class);
	
	private Context mContext = null; 
	
	public SQLiteDatabaseFactory(final Context context) { 
		mContext = context; 
	} 
	
	public final Context getContext() { 
		return mContext;
	}
	
	public Database openReadableDatabase(String path) {
		if (!(mContext == null || path.indexOf('/') >= 0)) 
			path = mContext.getDatabasePath(path).getPath(); 
		
		if (LOG.isDebugEnabled()) LOG.debug("SQLiteDatabase: openReadableDatabase: "+path);

		return new SQLiteDatabaseImpl(
				SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY)); 
	}
	
	public Database openWritableDatabase(String path) {
		if (LOG.isDebugEnabled()) LOG.debug("SQLiteDatabase: openWritableDatabase: "+path); 
		
		return new SQLiteDatabaseImpl((mContext == null || path.indexOf('/') >= 0) ? 
				SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.CREATE_IF_NECESSARY) : 
				mContext.openOrCreateDatabase(path, 0, null)); 
	}
	
	public Database createDatabase() {
		return new SQLiteDatabaseImpl(SQLiteDatabase.create(null)); 
	}
	
}
