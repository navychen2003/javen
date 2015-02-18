package org.javenstudio.android.entitydb;

import org.javenstudio.cocoka.database.SQLiteEntityDB;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.type.LongIdentity;

public class TUploadUpdater extends SQLiteEntityDB.TUpdater {

	private final TUpload mUpload; 
	private long mUploadKey = -1; 
	
	public TUploadUpdater(TUpload data) {
		super(TDefaultDB.getDatabase()); 
		
		mUpload = data; 
	}
	
	@Override
	protected SQLiteEntityDB.TEntity[] getEntities() {
		return new SQLiteEntityDB.TEntity[]{ mUpload }; 
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
		if (id != null && data == mUpload) {
			long key = ((LongIdentity) id).longValue(); 
			
			mUploadKey = key; 
		}
	}
	
	public final long getUploadKey() { 
		return mUploadKey; 
	}
	
}
