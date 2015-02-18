package org.javenstudio.common.entitydb.wrapper;

import java.util.Map;

import org.javenstudio.common.entitydb.IDatabase;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IEntityMap;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.IIdentityGenerator;
import org.javenstudio.common.entitydb.IMapCreator;
import org.javenstudio.common.entitydb.ITable;
import org.javenstudio.common.entitydb.db.DBFactory;
import org.javenstudio.common.entitydb.db.Databases;
import org.javenstudio.common.entitydb.nosql.SimpleDatabase;
import org.javenstudio.common.entitydb.nosql.SimpleEntity;
import org.javenstudio.common.entitydb.nosql.SimpleEntityMap;
import org.javenstudio.common.entitydb.nosql.SimpleQuery;
import org.javenstudio.common.entitydb.nosql.SimpleTable;

public abstract class TMemoryDB implements DBFactory {

	public static final String IDENTITY_FIELDNAME = "_id"; 
	
	public static IDatabase getDatabase(Class<? extends TMemoryDB> dbClass) {
		return Databases.getWritableDatabase(dbClass.getName()); 
	}
	
	public static class TTable<K extends IIdentity, T extends TEntity<K>> implements IMapCreator<K,T> { 
		private final String mTableName; 
		private final Class<T> mEntityClass; 
		
		public TTable(String tableName, Class<T> entityClass) { 
			mTableName = tableName; 
			mEntityClass = entityClass; 
		}
		
		public final String getTableName() { return mTableName; }
		public final Class<T> getEntityClass() { return mEntityClass; }
		public IIdentityGenerator<K> createIdentityGenerator() { return null; }
		public IEntityMap<K,T> createEntityMap(ITable<K,T> table) { return null; }
		
		private final void createTable(IDatabase db) { 
			db.createTable(getTableName(), getEntityClass(), 
					IDENTITY_FIELDNAME, createIdentityGenerator(), 
					this, true);
		}
	}
	
	public TMemoryDB() {} 
	
	public final String getDatabaseName() { return getClass().getName(); } 
	public final String getDatabasePath() { return "/" + getDatabaseName(); } 
	
	@Override
	public IDatabase openWritableDatabase() {
		return new TMemoryDatabase(getDatabaseName()); 
	}
	
	@Override
	public IDatabase openReadableDatabase() {
		return openWritableDatabase(); 
	}
	
	@Override
    public void onOpenDatabase(IDatabase db) { 
		TTable<? extends IIdentity, ? extends TEntity<?>>[] tables = getTables(); 
		for (int i=0; tables != null && i < tables.length; i++) { 
			TTable<? extends IIdentity, ? extends TEntity<?>> table = tables[i]; 
			if (table != null) table.createTable(db); 
		}
	}
	
	protected abstract <K extends IIdentity> TTable<K, ? extends TEntity<K>>[] getTables();
	
	public static abstract class TEntity<M extends IIdentity> extends SimpleEntity<M> {
		public TEntity() {
			super(); 
		}
		
		public TEntity(M id) {
			super(id);
		}
	}
	
	public static class TMemoryDatabase extends SimpleDatabase { 
		public TMemoryDatabase(String dbname) {
			super(dbname);
		}
		
		@Override
		protected <K extends IIdentity, T extends IEntity<K>> 
			SimpleTable<K,T> createSimpleTable(String name, Class<T> entityClass, 
					String identityFieldName, IIdentityGenerator<K> idGenerator, IMapCreator<K,T> mapCreator) { 
			return new TMemoryTable<K,T>(this, name, 
					entityClass, identityFieldName, idGenerator, mapCreator); 
		}
	}
	
	public static class TMemoryTable<K extends IIdentity, T extends IEntity<K>> extends SimpleTable<K,T> { 
		public TMemoryTable(TMemoryDatabase db, String name, Class<T> entityClass, 
				String identityFieldName, IIdentityGenerator<K> idGenerator, IMapCreator<K,T> mapCreator) {
			super(db, name, entityClass, identityFieldName, idGenerator, mapCreator);
		}
	}
	
	public static class TEntityMap<K extends IIdentity, T extends TEntity<K>> extends SimpleEntityMap<K,T> { 
		public TEntityMap(TMemoryTable<K,T> table) { 
			super(table);
		}
		
		@Override
		protected Map<K,T> createMap() { 
			return super.createMap();
		}
	}
	
	public static abstract class TUpdater<K extends IIdentity> extends Updater { 
		public TUpdater(IDatabase db) {
			super(db);
		}
		
		@Override
		protected TEntity<K>[] getEntities() { return null; }
	}
	
	public static abstract class TQuery<K extends IIdentity, T extends TEntity<K>> extends Query<K,T> { 
		public TQuery(IDatabase db, Class<T> entityClass) {
			super(new SimpleQuery<K,T>(db, entityClass));
		}
	}
	
	public static <M extends IIdentity, E extends TEntity<M>> 
			E queryEntity(IDatabase db, Class<M> identityClass, Class<E> entityClass, M id) { 
		return db.getTable(entityClass).query(id);
	}
	
}