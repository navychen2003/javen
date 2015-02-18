package org.javenstudio.android.entitydb;

import org.javenstudio.cocoka.database.SQLiteEntityDB;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.type.LongIdentity;

public class TDownloadUpdater extends SQLiteEntityDB.TUpdater {

	private final TDownload mDownload; 
	private long mDownloadKey = -1; 
	
	public TDownloadUpdater(TDownload data) {
		super(TDefaultDB.getDatabase()); 
		
		mDownload = data; 
	}
	
	@Override
	protected SQLiteEntityDB.TEntity[] getEntities() {
		return new SQLiteEntityDB.TEntity[]{ mDownload }; 
	}
	
	@Override
	protected void onInserted(IEntity<? extends IIdentity> data, IIdentity id) {
		onInsertOrUpdated(data, id);
	}
	
	@Override
	protected void onUpdated(IEntity<? extends IIdentity> data, IIdentity id) {
		onInsertOrUpdated(data, id);
	}
	
	private void onInsertOrUpdated(IEntity<? extends IIdentity> data, IIdentity id) {
		if (id != null && data == mDownload) {
			long key = ((LongIdentity) id).longValue(); 
			
			mDownloadKey = key; 
		}
	}
	
	public final long getDownloadKey() { 
		return mDownloadKey; 
	}
	
}
