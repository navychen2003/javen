package org.javenstudio.common.entitydb.nosql;

import org.javenstudio.common.entitydb.DBException;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IEntityManager;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.IMapCreator;
import org.javenstudio.common.entitydb.IQuery;

public class SimpleCache<K extends IIdentity, T extends IEntity<K>> {

	private final SimpleTable<K,T> mTable; 
	private final SimpleEntityManager<K,T> mCacheImpl; 
	
	public SimpleCache(SimpleTable<K,T> table, IMapCreator<K,T> mapCreator) {
		mTable = table; 
		mCacheImpl = new SimpleEntityManager<K,T>(table, mapCreator); 
	}
	
	private IEntityManager<K,T> getDelegated() {
		IEntityManager<K,T> delegated = mTable.getEntityManager(); 
		return delegated != null ? delegated  : mCacheImpl; 
	}
	
	public void loadAll() {
		final IEntityManager<K,T> delegated = getDelegated(); 
		
		if (delegated == mCacheImpl || !delegated.isTableCached()) 
			return; 
		
		mCacheImpl.clear(); 
		
		T[] entities = delegated.queryAll(); 
		for (int i=0; entities != null && i< entities.length; i++) {
			T data = entities[i]; 
			if (data.getIdentity() == null) 
				throw new DBException("entity has null identity"); 
			
			if (mCacheImpl.contains(data.getIdentity())) 
				throw new DBException("entity: "+data.getIdentity()+" already existed"); 
			
			if (data.getTable() == null) 
				data.setTable(mTable); 
			
			mCacheImpl.insert(data); 
		}
	}
	
	public void clear() {
		final IEntityManager<K,T> delegated = getDelegated(); 
		
		delegated.clear(); 
		
		if (delegated != mCacheImpl) 
			mCacheImpl.clear(); 
	}
	
	public int count() {
		final IEntityManager<K,T> delegated = getDelegated(); 
		
		if (delegated.isTableCached()) 
			return mCacheImpl.count(); 
		
		return delegated.count(); 
	}
	
	public boolean contains(K id) {
		final IEntityManager<K,T> delegated = getDelegated(); 
		
		if (delegated.isTableCached()) 
			return mCacheImpl.contains(id); 
		
		return delegated.contains(id); 
	}
	
	public T query(K id) {
		final IEntityManager<K,T> delegated = getDelegated(); 
		
		if (delegated.isTableCached()) 
			return mCacheImpl.query(id); 
		
		return delegated.query(id); 
	}
	
	public T[] query(IQuery<K,T> query) {
		final IEntityManager<K,T> delegated = getDelegated(); 
		
		if (delegated.isTableCached()) 
			return mCacheImpl.query(query); 
		
		return delegated.query(query); 
	}
	
	public T[] queryAll() {
		final IEntityManager<K,T> delegated = getDelegated(); 
		
		if (delegated.isTableCached()) 
			return mCacheImpl.queryAll(); 
		
		return delegated.queryAll(); 
	}
	
	public int queryCount(IQuery<K,T> query) {
		final IEntityManager<K,T> delegated = getDelegated(); 
		
		if (delegated.isTableCached()) 
			return mCacheImpl.queryCount(query); 
		
		return delegated.queryCount(query); 
	}
	
	public int deleteMany(IQuery<K,T> query) {
		final IEntityManager<K,T> delegated = getDelegated(); 
		
		int count = delegated.deleteMany(query); 
		
		if (delegated != mCacheImpl && delegated.isTableCached()) {
			int cnt = mCacheImpl.deleteMany(query); 
			if (cnt != count) 
				throw new DBException("deleteMany count in sqlite("+count+") not equals to cache("+cnt+")"); 
		}
		
		return count; 
	}
	
	public void insert(T data) {
		final IEntityManager<K,T> delegated = getDelegated(); 
		K id = delegated.insert(data); 
		
		if (data.getIdentity() == null) 
			data.setIdentity(id); 
		
		if (delegated != mCacheImpl && delegated.isTableCached()) 
			mCacheImpl.insert(data); 
	}
	
	public T update(T input) {
		final IEntityManager<K,T> delegated = getDelegated(); 
		boolean result = delegated.update(input.getIdentity(), input); 
		
		T stored = null; 
		if (result) {
			if (delegated != mCacheImpl && delegated.isTableCached()) {
				result = mCacheImpl.update(input.getIdentity(), input); 
				if (result) 
					stored = mCacheImpl.query(input.getIdentity()); 
			}
		}
		
		//if (result == false) 
		//	//throw new DBException("entity: "+input.getIdentity()+" not exist"); 
		
		// maybe only save streams
		if (stored == null) 
			stored = delegated.query(input.getIdentity()); 
		
		return stored; 
	}
	
	public void remove(K id) {
		final IEntityManager<K,T> delegated = getDelegated(); 
		delegated.remove(id); 
		
		if (delegated != mCacheImpl && delegated.isTableCached()) 
			mCacheImpl.remove(id); 
	}
	
}
