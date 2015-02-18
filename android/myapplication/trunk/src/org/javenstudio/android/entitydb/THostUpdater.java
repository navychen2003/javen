package org.javenstudio.android.entitydb;

import org.javenstudio.cocoka.database.SQLiteEntityDB;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.type.LongIdentity;

public class THostUpdater extends SQLiteEntityDB.TUpdater {

	private final THost mHost; 
	private long mHostKey = -1; 
	
	public THostUpdater(THost data) {
		super(TDefaultDB.getDatabase()); 
		
		mHost = data; 
	}
	
	@Override
	protected SQLiteEntityDB.TEntity[] getEntities() {
		return new SQLiteEntityDB.TEntity[]{ mHost }; 
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
		if (id != null && data == mHost) {
			long key = ((LongIdentity) id).longValue(); 
			
			mHostKey = key; 
		}
	}
	
	public final long getHostKey() { 
		return mHostKey; 
	}
	
}
