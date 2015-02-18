package org.javenstudio.android.entitydb;

import org.javenstudio.cocoka.database.SQLiteEntityDB;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.type.LongIdentity;

public class TAccountUpdater extends SQLiteEntityDB.TUpdater {

	private final TAccount mAccount; 
	private long mAccountKey = -1; 
	
	public TAccountUpdater(TAccount data) {
		super(TDefaultDB.getDatabase()); 
		
		mAccount = data; 
	}
	
	@Override
	protected SQLiteEntityDB.TEntity[] getEntities() {
		return new SQLiteEntityDB.TEntity[]{ mAccount }; 
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
		if (id != null && data == mAccount) {
			long key = ((LongIdentity) id).longValue(); 
			
			mAccountKey = key; 
		}
	}
	
	public final long getAccountKey() { 
		return mAccountKey; 
	}
	
}
