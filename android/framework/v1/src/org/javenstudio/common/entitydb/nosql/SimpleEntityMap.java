package org.javenstudio.common.entitydb.nosql;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.javenstudio.common.entitydb.DBException;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IEntityMap;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.IQuery;

public class SimpleEntityMap<K extends IIdentity, T extends IEntity<K>> implements IEntityMap<K,T> {

	private final SimpleTable<K,T> mTable;
	private final Map<K,T> mMap;
	
	public SimpleEntityMap(SimpleTable<K,T> table) { 
		mMap = createMap();
		mTable = table;
	}
	
	protected Map<K,T> createMap() { 
		return new TreeMap<K,T>();
	}
	
	public void clear() { mMap.clear(); }
	public int size() { return mMap.size(); }
	public boolean containsKey(K key) { return mMap.containsKey(key); }
	public T get(K key) { return mMap.get(key); }
	
	@SuppressWarnings("unchecked")
	public T[] get(IQuery<K,T> query)  { 
		ArrayList<T> results = new ArrayList<T>(); 
		
		synchronized (this) { 
			for (T entity : mMap.values()) {
				if (query.match(entity)) 
					results.add(entity); 
			}
		}
		
		T[] contents = (T[]) Array.newInstance(mTable.getEntityClass(), results.size()); 
		return results.toArray(contents); 
	}

	public int count(IQuery<K,T> query) { 
		int count = 0; 
		
		synchronized (this) { 
			for (T entity : mMap.values()) {
				if (query.match(entity)) 
					count ++; 
			}
		}
		
		return count; 
	}
	
	public int remove(IQuery<K,T> query) { 
		synchronized (this) { 
			int count = 0; 
			IIdentity[] ids = mMap.keySet().toArray(new IIdentity[0]); 
			
			for (int i=0; ids != null && i < ids.length; i++) {
				IIdentity id = ids[i]; 
				T entity = mMap.get(id); 
				
				if (query.match(entity)) {
					count ++; 
					mTable.onDeleteBefore(entity); 
					mMap.remove(id); 
					mTable.onDeleteAfter(entity); 
				}
			}
			
			return count; 
		}
	}
	
	@SuppressWarnings("unchecked")
	public T[] toArray() { 
		synchronized (this) { 
			T[] contents = (T[]) Array.newInstance(mTable.getEntityClass(), mMap.values().size());
			return mMap.values().toArray(contents); 
		}
	}
	
	public T put(K key, T value) { 
		synchronized (this) { 
			if (key != value.getIdentity()) 
				throw new DBException("key not equals value's identity");
			
			if (value.getIdentity() == null) { 
				value.setIdentity(mTable); // new identity
				key = value.getIdentity();
			}
			
			if (mMap.containsKey(key)) 
				throw new DBException("entity: "+key+" already existed"); 
			
			return mMap.put(key, value);
		}
	}
	
	public T remove(K key) { 
		synchronized (this) { 
			return mMap.remove(key);
		}
	}
	
}
