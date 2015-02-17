package org.javenstudio.common.entitydb.nosql;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.javenstudio.common.entitydb.Constants;
import org.javenstudio.common.entitydb.DBException;
import org.javenstudio.common.entitydb.ICursor;
import org.javenstudio.common.entitydb.IDatabase;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.IIdentityGenerator;
import org.javenstudio.common.entitydb.IMapCreator;
import org.javenstudio.common.entitydb.IQuery;
import org.javenstudio.common.entitydb.db.AbstractTable;
import org.javenstudio.common.entitydb.db.DBOperation;
import org.javenstudio.common.util.Logger;

public class SimpleTable<K extends IIdentity, T extends IEntity<K>> extends AbstractTable<K,T> {
	private static Logger LOG = Logger.getLogger(SimpleTable.class);
	
	private final IIdentityGenerator<K> mIdGenerator;
	private final SimpleCache<K,T> mCache; 
	private final List<WeakReference<SimpleCursor<K,T> > > mCursors; 
	private final ReentrantLock mLock;
	private final SimpleDatabase mDatabase; 
	private boolean mEntityLoaded; 
	private long mChangeTime; 
	
	public SimpleTable(SimpleDatabase db, String name, Class<T> entityClass, 
			String identityFieldName, IIdentityGenerator<K> idGenerator, 
			IMapCreator<K,T> mapCreator) {
		super(name, entityClass, identityFieldName); 
		mIdGenerator = idGenerator;
		mCache = new SimpleCache<K,T>(this, mapCreator); 
		mCursors = new ArrayList<WeakReference<SimpleCursor<K,T> > >(); 
		mLock = new ReentrantLock(false); 
		mDatabase = db; 
		mEntityLoaded = false; 
		mChangeTime = System.currentTimeMillis();
		
		if (LOG.isDebugEnabled()) { 
			LOG.debug("SimpleTable: " + getClass().getName() + " name: " + name + " created with" 
					+ " entityClass: " + entityClass.getName() + " identityField: " + identityFieldName 
					+ " identityGenerator: " + (idGenerator != null ? idGenerator.getClass().getName() : "null"));
		}
	}
	
	boolean isEntityDebug() { 
		return Constants.isEntityDebug() && LOG.isDebugEnabled(); 
	}
	void debug(String msg) { 
		LOG.debug(getClass().getSimpleName()+": "+msg); 
	}
	
	long getChangeTime() { return mChangeTime; }
	
	@Override
	public IDatabase getDatabase() {
		return mDatabase; 
	}
	
	@Override
	public K newIdentity(Object value) {
		if (mIdGenerator == null) 
			throw new DBException(getClass().getSimpleName()+" has no IdentityGenerator");
		
		return mIdGenerator.newIdentity(value);
	}
	
	private void loadEntities() {
		loadEntities(false); 
	}
	
	private void loadEntities(boolean force) {
		if (mEntityLoaded && !force) return; 
		
		lock(); 
		try {
			
			mCache.loadAll(); 
			
			mEntityLoaded = true; 
			mChangeTime = System.currentTimeMillis();
			
		} finally {
			unlock(); 
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
	
	@Override
	public int count() {
		loadEntities(); 
		
		lock(); 
		try {

			return mCache.count(); 
			
		} finally {
			unlock(); 
		}
	}
	
	@Override
	public K insert(T data) {
		if (data == null) return null; 
		
		checkEntity(data); 
		loadEntities(); 
		
		final T entity = ((Class<T>)getEntityClass()).cast(data); 
		
		lock(); 
		try {

			entity.setTable(this); 
			onInsertBefore(entity); 

			mCache.insert(entity); 
			entity.saveStreams(false); 
			
			onInsertAfter(entity); 
			if (isEntityDebug()) debug("insert: "+entity); 
			
		} finally {
			unlock(); 
		}
		
		notifyEntityChanged(entity, DBOperation.ENTITY_INSERT); 
		
		return entity.getIdentity(); 
	}
	
	@Override
	public K update(T data) {
		if (data == null) return null; 
		
		checkEntity(data); 
		loadEntities(); 
		
		final T entity = ((Class<T>)getEntityClass()).cast(data); 
		T stored = null; 
		
		lock(); 
		try {

			if (entity.getIdentity() == null) 
				throw new DBException("entity cannot update without identity"); 

			if (entity.getTable() == null) 
				entity.setTable(this); 
			else if (entity.getTable() != this) 
				throw new DBException("entity's table is not this table to update"); 
			
			onUpdateBefore(entity); 
			
			stored = mCache.update(entity); 
			if (stored != null)
				stored.saveStreams(entity, false); 
			
			onUpdateAfter(entity, stored != null); 
			if (isEntityDebug()) debug("update: "+entity+(stored != null ? " true" : " false")); 
			
		} finally {
			unlock(); 
		}
		
		if (stored != null) { 
			notifyEntityChanged(stored, DBOperation.ENTITY_UPDATE); 
			
			return stored.getIdentity(); 
		}
		
		return null; 
	}
	
	@Override
	public boolean contains(K id) {
		loadEntities(); 
		
		lock(); 
		try {
			
			return mCache.contains(id); 
			
		} finally {
			unlock(); 
		}
	}
	
	@Override
	public T query(K id) {
		loadEntities(); 
		
		lock(); 
		try {
			
			T entity = mCache.query(id); 
			if (entity != null && entity.getTable() == null) 
				entity.setTable(this); 
			
			return entity; 
			
		} finally {
			unlock(); 
		}
	}
	
	@Override
	public boolean delete(K id) {
		loadEntities(); 
		
		boolean existed = false; 
		T data = null; 
		
		lock(); 
		try {
			
			data = mCache.query(id); 
			existed = data != null; 
			
			if (data != null) {
				onDeleteBefore(data); 
				
				mCache.remove(id); 
				removeStreams(data); 
				
				onDeleteAfter(data); 
			}
			
			if (isEntityDebug()) debug("remove: "+id+" return "+existed); 
			
		} finally {
			unlock(); 
		}
		
		if (data != null) 
			notifyEntityChanged(data, DBOperation.ENTITY_DELETE); 
		
		return existed; 
	}
	
	protected SimpleCursor<K,T> createSimpleCursor(IQuery<K,T> query) { 
		return new SimpleCursor<K,T>(this, query); 
	}
	
	@Override
	public ICursor<K,T> query(IQuery<K,T> query) {
		if (query == null) return null; 
		
		loadEntities(); 
		
		lock(); 
		try {
			
			onQueryBefore(query); 
			
			SimpleCursor<K,T> cursor = createSimpleCursor(query); 
			mCursors.add(new WeakReference<SimpleCursor<K,T> >(cursor)); 
			
			onQueryAfter(query, cursor); 
			
			return cursor; 
			
		} finally {
			unlock(); 
		}
	}
	
	@Override
	public int queryCount(IQuery<K,T> query) {
		if (query == null) return 0; 
		
		loadEntities(); 
		
		lock(); 
		try {
			
			return mCache.queryCount(query); 
			
		} finally {
			unlock(); 
		}
	}
	
	@Override
	public int deleteMany(IQuery<K,T> query) {
		if (query == null) return 0; 

		loadEntities(); 
		int count = 0; 
		
		lock(); 
		try {
			
			count = mCache.deleteMany(query); 
			
		} finally {
			unlock(); 
		}
		
		if (count > 0) 
			notifyEntitiesChanged(count, DBOperation.ENTITY_DELETE); 
		
		return count; 
	}

	T[] requery(IQuery<K,T> query) {
		loadEntities(); 
		
		lock(); 
		try {

			return mCache.query(query); 
			
		} finally {
			unlock(); 
		}
	}
	
	private List<SimpleCursor<K,T>> getCursors() {
		lock(); 
		try {
			
			List<SimpleCursor<K,T>> cursors = new ArrayList<SimpleCursor<K,T>>(); 
			
			for (int i=0; i < mCursors.size(); ) {
				WeakReference<SimpleCursor<K,T> > ref = mCursors.get(i); 
				SimpleCursor<K,T> cursor = ref.get(); 
				if (cursor == null) {
					mCursors.remove(i); 
				} else {
					cursors.add(cursor); 
					i ++; 
				}
			}
			
			return cursors; 
			
		} finally {
			unlock(); 
		}
	}
	
	@Override
	public void notifyEntitiesChanged(int count, int change) {
		if (count <= 0) return; 
		
		List<SimpleCursor<K,T>> cursors = null; 
		
		lock(); 
		try {
			
			mChangeTime = System.currentTimeMillis();
			cursors = getCursors(); 
			
		} finally {
			unlock(); 
		}
		
		for (int i=0; cursors != null && i < cursors.size(); i++) {
			SimpleCursor<K,T> cursor = cursors.get(i); 
			if (cursor != null) 
				cursor.notifyEntitiesChange(count, change); 
		}
		
		onNotifyEntitiesChanged(count, change); 
	}
	
	@Override
	public void notifyEntityChanged(T data, int change) {
		if (data == null) return; 
		
		if (data.getClass() != getEntityClass()) 
			throw new DBException("SimpleTable: "+getTableName()+" cannot notify entity: "+data.getClass()); 
		
		doNotifyEntityChanged(data, change); 
	}
	
	@Override
	public void notifyEntityChanged(K id, int change) {
		if (id == null) return; 
		
		T data = query(id); 
		if (data == null) {
			LOG.warn("SimpleTable: "+getTableName()+" notifyEntityChanged: entity: "+id+" not found"); 
			return; 
		}
		
		doNotifyEntityChanged(data, change); 
	}
	
	private void doNotifyEntityChanged(T data, int change) {
		if (data == null) return; 
		
		List<SimpleCursor<K,T>> cursors = null; 
		
		lock(); 
		try {
			
			mChangeTime = System.currentTimeMillis();
			cursors = getCursors(); 
			
		} finally {
			unlock(); 
		}
		
		for (int i=0; cursors != null && i < cursors.size(); i++) {
			SimpleCursor<K,T> cursor = cursors.get(i); 
			if (cursor != null) 
				cursor.notifyEntityChange(data, change); 
		}
		
		onNotifyEntityChanged(data, change); 
	}
	
	void closeCursor(SimpleCursor<K,T> cursor) {
		lock(); 
		try {
			
			for (int i=0; i < mCursors.size(); ) {
				WeakReference<SimpleCursor<K,T> > ref = mCursors.get(i); 
				if (ref.get() == null || ref.get() == cursor) 
					mCursors.remove(i); 
				else 
					i ++; 
			}
			
		} finally {
			unlock(); 
		}
	}
	
}
