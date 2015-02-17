package org.javenstudio.common.entitydb;

import java.lang.reflect.Field;

import org.javenstudio.common.entitydb.db.DeleteTrigger;
import org.javenstudio.common.entitydb.db.EntityObserver;
import org.javenstudio.common.entitydb.db.InsertTrigger;
import org.javenstudio.common.entitydb.db.QueryTrigger;
import org.javenstudio.common.entitydb.db.UpdateTrigger;

public interface ITable<K extends IIdentity, T extends IEntity<K>> {

	public IDatabase getDatabase(); 
	public String getTableName(); 
	public String getIdentityFieldName(); 
	
	public Class<? extends IEntity<K>> getEntityClass(); 
	public String[] getEntityFieldNames(); 
	public Field getEntityField(String name); 

	public String[] getColumnNames(); 
	public String getColumnName(int columnIndex); 
	public int getColumnIndex(String columnName); 
	public int getColumnCount(); 
	
	public void setEntityManager(IEntityManager<K,T> loader); 
	public IEntityManager<K,T> getEntityManager(); 
	
	public String getStreamPath(String fieldName, K id); 
	public int scanStreamFields(T data); 
	public int scanStreamFieldsWithCheck(IEntity<K> data); 
	
	public K newIdentity(Object value); 
	public T castEntity(IEntity<K> data); 
	public T castEntityNoThrow(IEntity<K> data); 
	
	public void setInsertTrigger(InsertTrigger<K,T> trigger); 
	public void setUpdateTrigger(UpdateTrigger<K,T> trigger); 
	public void setDeleteTrigger(DeleteTrigger<K,T> trigger); 
	public void setQueryTrigger(QueryTrigger<K,T> trigger); 
	
	public void registerEntityObserver(EntityObserver observer); 
	public void unregisterEntityObserver(EntityObserver observer); 
	
	public void notifyEntitiesChanged(int count, int change); 
	public void notifyEntityChanged(T data, int change); 
	public void notifyEntityChanged(K id, int change); 
	public void notifyEntityChangedWithCheck(IEntity<K> data, int change); 
	
	public K insert(T data); 
	public K update(T data); 
	public K insertWithCheck(IEntity<K> data); 
	public K updateWithCheck(IEntity<K> data); 
	
	public int count(); 
	public int queryCount(IQuery<K,T> query); 
	public boolean contains(K id); 
	
	public T query(K id); 
	public ICursor<K,T> query(IQuery<K,T> query); 
	
	public boolean delete(K id); 
	public int deleteMany(IQuery<K,T> query); 
	
	public void lock(); 
	public void unlock(); 
	
}
