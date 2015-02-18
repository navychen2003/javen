package org.javenstudio.common.entitydb.nosql;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.javenstudio.common.entitydb.DBException;
import org.javenstudio.common.entitydb.IDatabaseManager;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.IIdentityGenerator;
import org.javenstudio.common.entitydb.IMapCreator;
import org.javenstudio.common.entitydb.ITable;
import org.javenstudio.common.entitydb.db.AbstractDatabase;
import org.javenstudio.common.entitydb.db.EntityManager;
import org.javenstudio.common.util.Logger;

public class SimpleDatabase extends AbstractDatabase {
	private static Logger LOG = Logger.getLogger(SimpleDatabase.class);

	private final Map<String, SimpleTable<? extends IIdentity, ? extends IEntity<?>> > mTablesByName; 
	private final Map<Class<? extends IEntity<?>>, SimpleTable<? extends IIdentity, ? extends IEntity<?>> > mTablesByClass; 
	private final ReentrantLock mLock;

	public SimpleDatabase(String dbname) {
		this(dbname, null); 
	}
	
	public SimpleDatabase(String dbname, IDatabaseManager manager) {
		super(dbname, manager); 
		mTablesByName = new HashMap<String, SimpleTable<? extends IIdentity, ? extends IEntity<?>> >(); 
		mTablesByClass = new HashMap<Class<? extends IEntity<?>>, SimpleTable<? extends IIdentity, ? extends IEntity<?>> >(); 
		mLock = new ReentrantLock(false); 
		
		if (LOG.isDebugEnabled()) { 
			LOG.debug("SimpleDatabase: " + getClass().getName() + " created with " + 
					((manager == null) ? ("no database manager") : 
						("database manager: " + manager.getClass().getName())));
		}
	}
	
	@Override
	public void lock() {
		final ReentrantLock lock = this.mLock;
		lock.lock();
	}
	
	@Override
	public void unlock() {
		final ReentrantLock lock = this.mLock;
		lock.unlock();
	}
	
	protected <K extends IIdentity, T extends IEntity<K>> 
			SimpleTable<K,T> createSimpleTable(String name, Class<T> entityClass, 
					String identityFieldName, IIdentityGenerator<K> idGenerator, 
					IMapCreator<K,T> mapCreator) { 
		return new SimpleTable<K,T>(this, name, 
				entityClass, identityFieldName, idGenerator, mapCreator); 
	}
	
	@Override
	public <K extends IIdentity, T extends IEntity<K>> 
			ITable<K,T> createTable(String name, Class<T> entityClass, String identityFieldName, 
					IIdentityGenerator<K> idGenerator, IMapCreator<K,T> mapCreator, boolean cacheEnabled) {
		if (name == null || name.length() == 0) 
			throw new DBException("table name is null"); 
		
		if (entityClass == null || !IEntity.class.isAssignableFrom(entityClass)) 
			throw new DBException("entity class: "+entityClass+" is not a IEntity class");
		
		lock(); 
		try {
			if (mTablesByName.containsKey(name) || mTablesByClass.containsKey(entityClass)) 
				throw new DBException("table: "+name+" already existed"); 
			
			SimpleTable<K,T> table = createSimpleTable(name, 
					entityClass, identityFieldName, idGenerator, mapCreator); 
			if (table == null) 
				throw new DBException("entity table create null instance for class: "+entityClass.getName());
			
			final IDatabaseManager manager = getDatabaseManager();
			if (manager != null) { 
				EntityManager<K,T> entityManager = manager.createEntityManager(this, name, cacheEnabled);
				if (entityManager == null) { 
					throw new DBException("Create null EntityManager for table: " + name 
							+ " class: " + table.getClass().getName() + " by " + manager.getClass().getName());
				}
				
				table.setEntityManager(entityManager);
			}
			
			mTablesByName.put(name, table); 
			mTablesByClass.put(entityClass, table); 
			
			return (ITable<K,T>)table; 
		} finally {
			unlock(); 
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <K extends IIdentity, T extends IEntity<K>> ITable<K,T> getTable(String name) {
		lock(); 
		try {
			SimpleTable<? extends IIdentity, ? extends IEntity<?>> table = mTablesByName.get(name); 
			if (table == null) 
				throw new DBException("table: "+name+" not existed"); 
			
			return (ITable<K,T>)table; 
		} finally {
			unlock(); 
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <K extends IIdentity, T extends IEntity<K>> ITable<K,T> getTable(Class<T> entityClass) {
		lock(); 
		try {
			SimpleTable<? extends IIdentity, ? extends IEntity<?>> table = mTablesByClass.get(entityClass); 
			if (table == null) 
				throw new DBException("table for entity: "+entityClass+" not existed"); 
			
			return (ITable<K,T>)table; 
		} finally {
			unlock(); 
		}
	}
	
}
