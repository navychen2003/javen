package org.javenstudio.cocoka.database.sqlite;

import android.content.Context;

import org.javenstudio.common.entitydb.rdb.DBOpenHelper;
import org.javenstudio.common.entitydb.rdb.Database;

public class SQLiteOpenHelper extends DBOpenHelper {

	private final Context mContext;
	
	public SQLiteOpenHelper(SQLiteDatabaseFactory factory, String dbName, int version) {
		super(factory, dbName, version); 
		mContext = factory.getContext();
	}
	
	public final Context getContext() { 
		return mContext;
	}
	
	@Override
	public String getDatabaseDirectory() {
    	return getContext().getFilesDir().getAbsolutePath(); 
    }
	
	@Override
	public void onUpgrade(Database db, int oldVersion, int newVersion) {
		// do nothing
	}
	
}
