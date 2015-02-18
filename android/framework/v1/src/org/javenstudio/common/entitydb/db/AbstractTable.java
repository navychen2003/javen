package org.javenstudio.common.entitydb.db;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

import org.javenstudio.common.entitydb.DBException;
import org.javenstudio.common.entitydb.ICursor;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IEntityManager;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.IQuery;
import org.javenstudio.common.entitydb.IStreamStore;
import org.javenstudio.common.entitydb.ITable;
import org.javenstudio.common.entitydb.type.StreamType;
import org.javenstudio.common.util.Logger;

public abstract class AbstractTable<K extends IIdentity, T extends IEntity<K>> implements ITable<K,T> {
	private static Logger LOG = Logger.getLogger(AbstractTable.class);
	
	EntityObservable mEntityObservable = new EntityObservable();
	
	private final Class<T> mEntityClass; 
	private final String[] mEntityFieldNames; 
	private final String[] mColumnNames; 
	private final String mTableName; 
	private final String mIdentityFieldName;
	
	private IEntityManager<K,T> mLoader = null; 
	private InsertTrigger<K,T> mInsertTrigger = null; 
	private UpdateTrigger<K,T> mUpdateTrigger = null; 
	private DeleteTrigger<K,T> mDeleteTrigger = null; 
	private QueryTrigger<K,T> mQueryTrigger = null; 
	
	public AbstractTable(String name, Class<T> entityClass, String identityFieldName) { 
		mEntityClass = entityClass; 
		mTableName = name; 
		mIdentityFieldName = identityFieldName;
		
		if (name == null || name.length() == 0) 
			throw new DBException("table name is null"); 
		
		if (entityClass == null || !IEntity.class.isAssignableFrom(entityClass)) 
			throw new DBException("entity class: "+entityClass+" is not a IEntity class");

		Field[] fields = entityClass.getDeclaredFields(); 
		if (fields != null) {
			mEntityFieldNames = new String[fields.length]; 
			mColumnNames = new String[fields.length+1]; 
			mColumnNames[0] = identityFieldName; 
			for (int i=0; i < fields.length; i++) {
				Field field = fields[i]; 
				field.setAccessible(true); // private accessable
				mEntityFieldNames[i] = field.getName(); 
				mColumnNames[i+1] = field.getName(); 
			}
		} else {
			mEntityFieldNames = null; 
			mColumnNames = null; 
			throw new DBException("entity class: "+entityClass+" has no fields"); 
		}
	}
	
	@Override
	public final String getTableName() {
		return mTableName; 
	}
	
	@Override
	public final Class<T> getEntityClass() {
		return mEntityClass; 
	}
	
	@Override
	public final String getIdentityFieldName() { 
		return mIdentityFieldName;
	}
	
	@Override
	public final String[] getEntityFieldNames() {
		return mEntityFieldNames; 
	}
	
	@Override 
	public final String[] getColumnNames() {
		return mColumnNames; 
	}
	
	@Override
	public final String getColumnName(int columnIndex) {
		return columnIndex >= 0 && columnIndex < mColumnNames.length ? mColumnNames[columnIndex] : null; 
	}
	
	@Override 
	public final int getColumnIndex(String columnName) {
		if (columnName == null || columnName.length() == 0) 
			return -1; 
		
		for (int i=0; i < mColumnNames.length; i++) {
			String name = mColumnNames[i]; 
			if (name.equals(columnName)) 
				return i; 
		}
		
		return -1; 
	}
	
	@Override 
	public final int getColumnCount() {
		return mColumnNames.length; 
	}
	
	@Override
	public final Field getEntityField(String name) {
		try {
			return mEntityClass.getField(name); 
		} catch (NoSuchFieldException e) {
			return null; 
		}
	}
	
	protected void checkEntity(IEntity<K> data) {
		if (data == null) throw new DBException("entity is null"); 
		
		if (getEntityClass() != data.getClass()) 
			throw new DBException("entity is not a "+getEntityClass()); 
	}
	
	protected boolean checkEntityNoThrow(IEntity<K> data) { 
		if (data != null && getEntityClass() == data.getClass()) 
			return true;
		return false;
	}
	
	@Override
	public T castEntity(IEntity<K> data) { 
		checkEntity(data);
		final T entity = ((Class<T>)getEntityClass()).cast(data); 
		return entity;
	}
	
	@Override
	public T castEntityNoThrow(IEntity<K> data) { 
		if (checkEntityNoThrow(data)) { 
			final T entity = ((Class<T>)getEntityClass()).cast(data); 
			return entity;
		}
		return null;
	}
	
	@Override
	public K insertWithCheck(IEntity<K> data) { 
		if (data == null) return null; 
		
		checkEntity(data); 
		final T entity = ((Class<T>)getEntityClass()).cast(data); 
		
		return insert(entity);
	}
	
	@Override
	public K updateWithCheck(IEntity<K> data) { 
		if (data == null) return null; 
		
		checkEntity(data); 
		final T entity = ((Class<T>)getEntityClass()).cast(data); 
		
		return update(entity);
	}
	
	@Override
	public void notifyEntityChangedWithCheck(IEntity<K> data, int change) { 
		if (data == null) return; 
		
		checkEntity(data); 
		final T entity = ((Class<T>)getEntityClass()).cast(data); 
		
		notifyEntityChanged(entity, change);
	}
	
	@Override
	public int scanStreamFieldsWithCheck(IEntity<K> data) { 
		if (data == null) return 0; 
		
		checkEntity(data); 
		final T entity = ((Class<T>)getEntityClass()).cast(data); 
		
		return scanStreamFields(entity);
	}
	
	@Override
	public int scanStreamFields(T data) { 
		if (data == null) return 0; 
		
		IEntityManager<K,T> manager = getEntityManager(); 
		if (manager != null) { 
			checkEntity(data); 
			final T entity = ((Class<T>)getEntityClass()).cast(data); 
			
			return manager.scanStreamFields(entity); 
		}
		
		return 0; 
	}
	
	protected void removeStreams(T data) {
		if (data == null) return; 
		
		String[] names = data.getStreamFieldNames(); 
		for (int i=0; names != null && i < names.length; i++) {
			String name = names[i]; 
			StreamType type = data.getAsStreamType(name); 
			if (type == null) continue; 
			
			String filepath = type.getFilePath(); 
			if (filepath != null) {
				File file = new File(filepath); 
				if (file.exists() && file.delete()) {
					LOG.info("AbstractTable.removeStreams: removed stream: " + 
						data.getClass().getSimpleName()+"="+data.getIdentity()+" field="+name+" path="+filepath); 
				}
			}
		}
	}
	
	@Override 
	public String getStreamPath(String fieldName, K id) {
		if (id == null || fieldName == null || fieldName.length() == 0) 
			throw new DBException("entity id or field name input null"); 
		
		IEntityManager<K,T> delegated = getEntityManager(); 
		if (delegated != null) 
			return delegated.getStreamPath(fieldName, id); 
		
		return getDefaultStreamPath(fieldName, id); 
	}
	
	public String getDefaultStreamPath(String fieldName, K id) {
		try {
			IStreamStore store = getDatabase().getStreamStore(); 
			if (store == null) 
				throw new IOException("stream store not set");
			
			String filename = getDatabase().getDatabaseName() + "_" + getTableName() + "_" + fieldName + "_" + id + ".dat";
			return store.createFilePath(filename); 
			
		} catch (IOException e) {
			throw new DBException("new stream path error: "+e); 
		}
	}
	
	@Override
	public void setEntityManager(IEntityManager<K,T> loader) {
		mLoader = loader; 
	}
	
	@Override
	public IEntityManager<K,T> getEntityManager() {
		return mLoader; 
	}
	
	@Override
	public void setInsertTrigger(InsertTrigger<K,T> trigger) {
		mInsertTrigger = trigger; 
	}
	
	@Override
	public void setUpdateTrigger(UpdateTrigger<K,T> trigger) {
		mUpdateTrigger = trigger; 
	}
	
	@Override
	public void setDeleteTrigger(DeleteTrigger<K,T> trigger) {
		mDeleteTrigger = trigger; 
	}
	
	@Override
	public void setQueryTrigger(QueryTrigger<K,T> trigger) {
		mQueryTrigger = trigger; 
	}
	
	@Override
	public void registerEntityObserver(EntityObserver observer) {
        mEntityObservable.registerObserver(observer);
    }

	@Override
    public void unregisterEntityObserver(EntityObserver observer) {
        mEntityObservable.unregisterObserver(observer);
    }
	
	protected void onNotifyEntitiesChanged(int count, int change) {
		mEntityObservable.dispatchChange(count, change); 
	}
	
	protected void onNotifyEntityChanged(IEntity<K> data, int change) {
		mEntityObservable.dispatchChange(data, change); 
	}
	
	protected void onInsertBefore(T data) {
		InsertTrigger<K,T> trigger = mInsertTrigger; 
		if (trigger != null) 
			trigger.onInsertBefore(this, data); 
	}
	
	protected void onInsertAfter(T data) {
		InsertTrigger<K,T> trigger = mInsertTrigger; 
		if (trigger != null) 
			trigger.onInsertAfter(this, data); 
	}
	
	protected void onUpdateBefore(T data) {
		UpdateTrigger<K,T> trigger = mUpdateTrigger; 
		if (trigger != null) 
			trigger.onUpdateBefore(this, data); 
	}
	
	protected void onUpdateAfter(T data, boolean updated) {
		UpdateTrigger<K,T> trigger = mUpdateTrigger; 
		if (trigger != null) 
			trigger.onUpdateAfter(this, data, updated); 
	}
	
	public void onDeleteBefore(T data) {
		DeleteTrigger<K,T> trigger = mDeleteTrigger; 
		if (trigger != null) 
			trigger.onDeleteBefore(this, data); 
	}
	
	public void onDeleteAfter(T data) {
		DeleteTrigger<K,T> trigger = mDeleteTrigger; 
		if (trigger != null) 
			trigger.onDeleteAfter(this, data); 
	}
	
	protected void onQueryBefore(IQuery<K,T> query) {
		QueryTrigger<K,T> trigger = mQueryTrigger; 
		if (trigger != null) 
			trigger.onQueryBefore(this, query); 
	}
	
	protected void onQueryAfter(IQuery<K,T> query, ICursor<K,T> cursor) {
		QueryTrigger<K,T> trigger = mQueryTrigger; 
		if (trigger != null) 
			trigger.onQueryAfter(this, query, cursor); 
	}
	
}
