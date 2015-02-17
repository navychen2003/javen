package org.javenstudio.common.entitydb.example;

import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.wrapper.SimpleMemoryDB;
import org.javenstudio.common.entitydb.wrapper.SimpleMemoryDB.TEntity;

public class TestEntityUpdater extends SimpleMemoryDB.TUpdater {

	private TestEntity[] mEntities = null; 
	private int mInsertCount = 0;
	private int mUpdateCount = 0;
	private int mDeleteCount = 0;
	
	public TestEntityUpdater(TestEntity... data) {
		super(TestDatabase.getDatabase()); 
		mEntities = data;
	}
	
	@Override
	protected TEntity[] getEntities() {
		return mEntities; 
	}
	
	@Override
	protected void onInserted(IEntity<? extends IIdentity> data, IIdentity id) { 
		if (data != null && id != null) 
			mInsertCount ++;
	}
	
	@Override
	protected void onUpdated(IEntity<? extends IIdentity> data, IIdentity id) { 
		if (data != null && id != null) 
			mUpdateCount ++;
	}
	
	@Override
	protected void onDeleted(IEntity<? extends IIdentity> data) { 
		if (data != null) 
			mDeleteCount ++;
	}
	
	public final int getInsertCount() { return mInsertCount; }
	public final int getUpdateCount() { return mUpdateCount; }
	public final int getDeleteCount() { return mDeleteCount; }
	
}
