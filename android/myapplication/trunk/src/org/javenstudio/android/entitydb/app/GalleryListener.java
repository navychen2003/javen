package org.javenstudio.android.entitydb.app;

import android.content.Context;

import org.javenstudio.android.entitydb.IDatabaseListener;
import org.javenstudio.android.entitydb.TAccount;
import org.javenstudio.android.entitydb.TDefaultDB;
import org.javenstudio.android.entitydb.TDownload;
import org.javenstudio.android.entitydb.TFetch;
import org.javenstudio.android.entitydb.THost;
import org.javenstudio.android.entitydb.TUpload;
import org.javenstudio.cocoka.database.SQLiteEntityConf;
import org.javenstudio.cocoka.database.sqlite.SQLiteOpenHelper;
import org.javenstudio.common.entitydb.rdb.Database;
import org.javenstudio.common.util.Logger;

public class GalleryListener implements IDatabaseListener {
	private static final Logger LOG = Logger.getLogger(GalleryListener.class);
	
	@Override
	public SQLiteEntityConf createEntityConf(Context context) { 
		SQLiteEntityConf conf = new SQLiteEntityConf(context, TDefaultDB.DB_NAME, TDefaultDB.DB_VERSION, this);
		
		conf.registerTable(THost.Table.TABLE_NAME, THost.Table.class, THost.class, true);
		conf.registerTable(TAccount.Table.TABLE_NAME, TAccount.Table.class, TAccount.class, true);
		conf.registerTable(TUpload.Table.TABLE_NAME, TUpload.Table.class, TUpload.class, true);
		conf.registerTable(TDownload.Table.TABLE_NAME, TDownload.Table.class, TDownload.class, true);
		conf.registerTable(TFetch.Table.TABLE_NAME, TFetch.Table.class, TFetch.class, true);
		
		return conf;
	}
	
	@Override
	public void onDatabaseCreate(SQLiteOpenHelper helper, Database db) {
		if (LOG.isDebugEnabled())
			LOG.debug("onDatabaseCreate: db=" + db + " version=" + db.getVersion());
	}

	@Override
	public void onDatabaseUpgrade(SQLiteOpenHelper helper, Database db,
			int oldVersion, int newVersion) {
		if (LOG.isDebugEnabled()) { 
			LOG.debug("onDatabaseUpgrade: db=" + db + " oldVersion=" + oldVersion 
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
		if (LOG.isDebugEnabled())
			LOG.debug("onDatabaseOpen: db=" + db + " version=" + db.getVersion());
	}
	
}
