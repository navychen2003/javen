package org.javenstudio.cocoka.net.metrics;

import org.javenstudio.common.entitydb.IDatabase;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.ITable;
import org.javenstudio.common.entitydb.wrapper.SimpleMemoryDB;

public class TMetricsDB extends SimpleMemoryDB {

	public static IDatabase getDatabase() {
		return getDatabase(TMetricsDB.class); 
	}
	
	public TMetricsDB() {} 
	
    @Override
    protected TTable<? extends TEntity>[] getTables() { 
    	return new TTable<?>[] { 
    			new TMetrics.Table(), 
    			new TMetricsRecord.Table()
    		};
    }
    
    public static <K extends IIdentity, E extends IEntity<K>> ITable<K,E> getTable(Class<E> entityClass) { 
		return getDatabase().getTable(entityClass);
	}
    
    public static <E extends TEntity> E queryEntity(Class<E> entityClass, long id) { 
    	return queryEntity(getDatabase(), entityClass, id);
    }
    
}
