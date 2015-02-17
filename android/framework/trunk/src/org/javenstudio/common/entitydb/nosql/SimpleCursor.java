package org.javenstudio.common.entitydb.nosql;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.javenstudio.common.entitydb.DBException;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.IQuery;
import org.javenstudio.common.entitydb.ITable;
import org.javenstudio.common.entitydb.db.BaseCursor;
import org.javenstudio.common.entitydb.db.DBOperation;

public class SimpleCursor<K extends IIdentity, T extends IEntity<K>> 
		extends BaseCursor<K,T> {

	private final List<T> mInserts; 
	private final List<T> mUpdates; 
	private final List<T> mDeletes; 
	private T[] mRecords = null; 
	private int mCount = 0; 
	private long mLastTime = 0; 
	
	public SimpleCursor(ITable<K,T> table, IQuery<K,T> query) {
		super(table, query); 
		mInserts = new ArrayList<T>(); 
		mUpdates = new ArrayList<T>(); 
		mDeletes = new ArrayList<T>(); 
	}
	
	private SimpleTable<K,T> getSimpleTable() {
		return (SimpleTable<K,T>)getTable(); 
	}
	
	private boolean ensureQuery() {
		return ensureQuery(false); 
	}
	
	@SuppressWarnings({"unchecked"})
	private boolean ensureQuery(boolean requery) {
		synchronized (this) {
			long lastchange = getNotifiedTime(); 
			boolean updated = false; 
			
			if (mRecords == null || (requery && lastchange > mLastTime)) {
				mLastTime = lastchange; 
				mInserts.clear(); 
				mUpdates.clear(); 
				mDeletes.clear(); 
				
				mRecords = sortEntities(getSimpleTable().requery(getQuery())); 
				if (mRecords == null) 
					mRecords = (T[]) Array.newInstance(getTable().getEntityClass(), 0); 
				mCount = mRecords != null ? mRecords.length : 0; 
				
				return true; 
			} 
			
			if (mInserts.size() > 0) { 
				for (T data : mInserts) { 
					insertEntity(data); 
					updated = true; 
				}
				mInserts.clear(); 
			}
			
			if (mUpdates.size() > 0) { 
				for (T data : mUpdates) { 
					updateEntity(data); 
					updated = true; 
				}
				mUpdates.clear(); 
			}
			
			if (mDeletes.size() > 0) { 
				for (T data : mDeletes) { 
					deleteEntity(data); 
					updated = true; 
				}
				mDeletes.clear(); 
			}
			
			return updated; 
		}
	}
	
	private void updateEntity(T data) {
		if (data == null) return; 
		
		synchronized (this) {
			boolean found = false; 
			final T[] entities = mRecords; 
			
			for (int i=0; entities != null && i < entities.length; i++) {
				if (entities[i] == null) continue; 
				if (entities[i].getIdentity().equals(data.getIdentity())) {
					if (entities[i] != data) 
						entities[i] = data; 
					found = true; 
				}
			}
			
			if (found) {
				mRecords = sortEntities(entities); 
				return; 
			}
			
			insertEntity(data); 
		}
	}
	
	private void deleteEntity(T data) {
		if (data == null) return; 
		
		doInsertOrDeleteEntity(data, false); 
	}
	
	@SuppressWarnings({"unused"})
	private void deleteEntity2(T data) {
		if (data == null) return; 
		
		synchronized (this) {
			if (mRecords == null || mRecords.length == 0) 
				return; 
			
			int pos = -1; 
			for (int i=0; i < mRecords.length; i++) {
				if (mRecords[i] == null) continue; 
				if (mRecords[i] == data || mRecords[i].getIdentity().equals(data.getIdentity())) {
					if (pos < 0) pos = i; 
				} else {
					if (pos >= 0) 
						mRecords[pos++] = mRecords[i]; 
				}
			}

			if (pos >= 0) {
				for (int i=pos; i < mRecords.length; i++) 
					mRecords[i] = null; 
				
				mCount = pos; 
			}
		}
	}
	
	private void insertEntity(T data) {
		if (data == null) return; 
		
		doInsertOrDeleteEntity(data, true); 
	}
	
	@SuppressWarnings({"unchecked"})
	private void doInsertOrDeleteEntity(T data, boolean insert) {
		synchronized (this) {
			final T[] entities = mRecords; 
			Set<T> records = new HashSet<T>(); 
			
			for (int i=0; entities != null && i < entities.length; i++) {
				T entity = entities[i]; 
				if (entity == null) continue; 
				if (!insert && data != null) {
					if (entity == data) continue; 
					if (entity.getIdentity().equals(data.getIdentity())) 
						continue; 
				}
				records.add(entity); 
			}
			if (insert && data != null) 
				records.add(data); 
			
			if (records.size() > 0) {
				T[] contents = (T[]) Array.newInstance(getTable().getEntityClass(), records.size()); 
				mRecords = sortEntities(records.toArray(contents)); 
				
			} else
				mRecords = null; 
			
			mCount = mRecords != null ? mRecords.length : 0; 
		}
	}
	
	@Override
	protected void onNotifyEntityChange(T data, int change, 
			boolean queryMatch, boolean notifyMatch) {
		synchronized (this) {
			switch (change) {
			case DBOperation.ENTITY_INSERT: 
				if (queryMatch) { 
					mInserts.add(data); 
					mLastTime = getNotifiedTime(); 
				}
				break; 
				
			case DBOperation.ENTITY_UPDATE: 
				if (queryMatch)
					mUpdates.add(data); 
				else 
					mDeletes.add(data); 
				mLastTime = getNotifiedTime(); 
				break; 
				
			case DBOperation.ENTITY_DELETE: 
				mDeletes.add(data); 
				mLastTime = getNotifiedTime(); 
				break; 
			}
		}
		super.onNotifyEntityChange(data, change, queryMatch, notifyMatch); 
	}
	
	@Override
	public boolean requery() {
		return super.requery(ensureQuery(true)); 
	}
	
	@Override
	public boolean requery(boolean notify) {
		ensureQuery(true); 
		return super.requery(notify); 
	}
	
	@Override
	public void close() {
		super.close(); 
		
		getSimpleTable().closeCursor(this); 
	}
	
	@Override
	public int getCount() {
		ensureQuery(); 
		
		synchronized (this) {
			return mCount; 
		}
	}
	
	@Override
	public T entityAt(int position) {
		ensureQuery(); 
		
		synchronized (this) {
			if (mRecords == null || mRecords.length == 0) 
				throw new DBException("entity set is empty"); 
			
			if (position < 0 || position >= mCount) 
				throw new DBException("entity position: "+position+" must between 0 and "+mCount); 
			
			return mRecords[position]; 
		}
	}
	
}
