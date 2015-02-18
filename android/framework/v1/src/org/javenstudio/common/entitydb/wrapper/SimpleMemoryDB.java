package org.javenstudio.common.entitydb.wrapper;

import java.util.Map;

import org.javenstudio.common.entitydb.ICursor;
import org.javenstudio.common.entitydb.IDatabase;
import org.javenstudio.common.entitydb.IEntityMap;
import org.javenstudio.common.entitydb.IIdentityGenerator;
import org.javenstudio.common.entitydb.ITable;
import org.javenstudio.common.entitydb.type.LongIdentity;

public abstract class SimpleMemoryDB extends TMemoryDB {

	public static class TTable<T extends TEntity> extends TMemoryDB.TTable<LongIdentity, T> { 
		public TTable(String tableName, Class<T> entityClass) { 
			super(tableName, entityClass);
		}
		
		@Override
		public IIdentityGenerator<LongIdentity> createIdentityGenerator() { 
			return new LongIdentity.Generator();
		}
		
		@Override
		public IEntityMap<LongIdentity, T> createEntityMap(ITable<LongIdentity, T> table) { 
			return null; 
		}
	}
	
	public SimpleMemoryDB() {} 
	
	@Override
	@SuppressWarnings("unchecked")
	protected abstract TTable<? extends TEntity>[] getTables();
	
	public static abstract class TEntity extends TMemoryDB.TEntity<LongIdentity> {
		public TEntity() {
			super(); 
		}
		
		public TEntity(long id) {
			super(id > 0 ? new LongIdentity(id) : null);
		}
		
		public final long getId() { 
			LongIdentity id = getIdentity();
			return id != null ? id.longValue() : -1;
		}
	}
	
	public static class TEntityMap<T extends TEntity> extends TMemoryDB.TEntityMap<LongIdentity, T> { 
		public TEntityMap(TMemoryTable<LongIdentity, T> table) { 
			super(table);
		}
		
		@Override
		protected Map<LongIdentity, T> createMap() { 
			return super.createMap();
		}
	}
	
	public static abstract class TUpdater extends TMemoryDB.TUpdater<LongIdentity> { 
		public TUpdater(IDatabase db) {
			super(db);
		}
		
		@Override
		protected TEntity[] getEntities() { return null; }
	}
	
	public static class TCursor<T extends TEntity> extends Cursor<LongIdentity, T> { 
		public TCursor(ICursor<LongIdentity, T> cursor) { 
			super(cursor);
		}
	}
	
	public static abstract class TQuery<T extends TEntity> extends TMemoryDB.TQuery<LongIdentity, T> { 
		public TQuery(IDatabase db, Class<T> entityClass) {
			super(db, entityClass);
		}
		
		public final TCursor<T> query() { 
			return new TCursor<T>(queryCursor());
		}
	}
	
	public static <E extends TEntity> E queryEntity(IDatabase db, Class<E> entityClass, long id) { 
		if (id <= 0) return null;
		return db.getTable(entityClass).query(new LongIdentity(id));
	}
	
}
