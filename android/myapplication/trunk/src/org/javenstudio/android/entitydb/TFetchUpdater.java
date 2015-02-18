package org.javenstudio.android.entitydb;

import org.javenstudio.cocoka.database.SQLiteEntityDB;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.type.LongIdentity;

public class TFetchUpdater extends SQLiteEntityDB.TUpdater {

	private final TFetch mFetch; 
	private long mFetchKey = -1; 
	
	public TFetchUpdater(TFetch data) {
		super(TDefaultDB.getDatabase()); 
		
		mFetch = data; 
	}
	
	@Override
	protected SQLiteEntityDB.TEntity[] getEntities() {
		return new SQLiteEntityDB.TEntity[]{ mFetch }; 
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
		if (id != null && data == mFetch) {
			long key = ((LongIdentity) id).longValue(); 
			
			mFetchKey = key; 
		}
	}
	
	public final long getFetchKey() { 
		return mFetchKey; 
	}
	
}
