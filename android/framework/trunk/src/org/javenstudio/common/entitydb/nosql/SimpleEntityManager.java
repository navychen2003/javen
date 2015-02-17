package org.javenstudio.common.entitydb.nosql;

import org.javenstudio.common.entitydb.DBException;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IEntityManager;
import org.javenstudio.common.entitydb.IEntityMap;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.IMapCreator;
import org.javenstudio.common.entitydb.IQuery;

public class SimpleEntityManager<K extends IIdentity, T extends IEntity<K>> implements IEntityManager<K,T> {

	private final SimpleTable<K,T> mTable; 
	private IEntityMap<K, T> mMap; 
	
	public SimpleEntityManager(SimpleTable<K,T> table, IMapCreator<K,T> mapCreator) {
		mTable = table; 
		mMap = mapCreator != null ? mapCreator.createEntityMap(table) : null; 
		if (mMap == null) 
			mMap = new SimpleEntityMap<K, T>(table); 
	}
	
	@Override 
	public boolean isTableCached() {
		return true; 
	}
	
	@Override 
	public String getStreamPath(String fieldName, K id) {
		return mTable.getDefaultStreamPath(fieldName, id); 
	}
	
	@Override 
	public int scanStreamFields(T data) { 
		// not implemented 
		return 0; 
	}
	
	@Override 
	public void clear() { mMap.clear(); } 
	
	@Override 
	public int count() { return mMap.size(); } 
	
	@Override 
	public boolean contains(K id) { 
		return mMap.containsKey(id); 
	} 
	
	@Override 
	public T query(K id) { 
		return mMap.get(id); 
	} 
	
	@Override
	public T[] query(IQuery<K,T> query) {
		return mMap.get(query);
	}
	
	@Override
	public int queryCount(IQuery<K,T> query) {
		return mMap.count(query);
	}
	
	@Override 
	public int deleteMany(IQuery<K,T> query) {
		return mMap.remove(query);
	}
	
	@Override 
	public T[] queryAll() {
		return mMap.toArray();
	}
	
	@Override 
	public K insert(T stored) {
		if (stored == null) 
			throw new DBException("insert with null parameters");
		
		synchronized (this) { 
			if (stored.getIdentity() != null && mMap.containsKey(stored.getIdentity())) 
				throw new DBException("entity: "+stored.getIdentity()+" already existed"); 
			
			mMap.put(stored.getIdentity(), stored); 
			
			if (stored.getIdentity() == null) 
				throw new DBException("entity insert return null identity");
		}
		
		return stored.getIdentity(); 
	}
	
	@Override 
	public boolean update(K id, T input) {
		if (id == null || input == null) 
			throw new DBException("update with null parameters");
		
		synchronized (this) { 
			T stored = mMap.get(id); 
			
			if (stored != null) {
				stored.updateFrom(input); 
				return true; 
			}
			
			return false; 
		}
	}
	
	@Override 
	public boolean remove(K id) { 
		if (id == null) 
			throw new DBException("remove with null parameters");
		
		synchronized (this) { 
			return mMap.remove(id) != null; 
		}
	} 
	
}
