package org.javenstudio.common.entitydb.example;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.javenstudio.common.entitydb.IDatabase;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.ITable;
import org.javenstudio.common.entitydb.type.LongIdentity;
import org.javenstudio.common.entitydb.wrapper.SimpleMemoryDB;

public class TestDatabase extends SimpleMemoryDB {

	public static IDatabase getDatabase() {
		return getDatabase(TestDatabase.class); 
	}
	
	public TestDatabase() {} 
	
    @Override
    protected TTable<? extends TEntity>[] getTables() { 
    	return new TTable<?>[] { 
    			new TestEntity.Table()
    		};
    }
    
    public static <K extends IIdentity, E extends IEntity<K>> ITable<K,E> getTable(Class<E> entityClass) { 
		return getDatabase().getTable(entityClass);
	}
    
    public static <E extends TEntity> E queryEntity(Class<E> entityClass, long id) { 
    	return queryEntity(getDatabase(), entityClass, id);
    }
	
	public static class EntityTreeMap<T extends TEntity> extends SimpleMemoryDB.TEntityMap<T> { 
		public EntityTreeMap(SimpleMemoryDB.TMemoryTable<LongIdentity, T> table) { 
			super(table);
		}
		
		@Override
		protected Map<LongIdentity, T> createMap() { 
			return new TreeMap<LongIdentity, T>();
		}
	}
    
	public static class EntityHashMap<T extends TEntity> extends SimpleMemoryDB.TEntityMap<T> { 
		public EntityHashMap(SimpleMemoryDB.TMemoryTable<LongIdentity, T> table) { 
			super(table);
		}
		
		@Override
		protected Map<LongIdentity, T> createMap() { 
			return new HashMap<LongIdentity, T>();
		}
	}
	
}
