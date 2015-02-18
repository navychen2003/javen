package org.javenstudio.android.entitydb;

import android.content.Context;

import org.javenstudio.cocoka.database.SQLiteEntityConf;
import org.javenstudio.cocoka.database.SQLiteEntityDB;

public interface IDatabaseListener extends SQLiteEntityDB.DatabaseListener {

	public SQLiteEntityConf createEntityConf(Context context);
	
}
