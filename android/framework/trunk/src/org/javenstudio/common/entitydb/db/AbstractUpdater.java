package org.javenstudio.common.entitydb.db;

import org.javenstudio.common.entitydb.DBException;
import org.javenstudio.common.entitydb.EntityException;
import org.javenstudio.common.entitydb.IDatabase;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.IUpdater;

public abstract class AbstractUpdater implements IUpdater {

	private final IDatabase mDatabase; 
	private boolean mSaved = false; 
	
	public AbstractUpdater(IDatabase db) {
		mDatabase = db; 
	} 

	protected IEntity<? extends IIdentity>[] getEntities() { 
		return null;
	}
	
	@Override
	public final void saveOrUpdate() throws EntityException {
		if (mSaved) throw new DBException("already updated"); 
		
		IEntity<? extends IIdentity>[] entities = getEntities(); 
		
		for (int i=0; entities != null && i < entities.length; i++) {
			IEntity<? extends IIdentity> data = entities[i]; 
			if (data == null) continue; 
			
			if (data.getIdentity() == null) { 
				IIdentity id = mDatabase.insert(data); 
				onInserted(data, id); 
			} else { 
				IIdentity id = mDatabase.update(data); 
				onUpdated(data, id); 
			}
		}
		
		mSaved = true; 
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public final void insert() throws EntityException {
		if (mSaved) throw new DBException("already updated"); 
		
		IEntity<? extends IIdentity>[] entities = getEntities(); 
		
		for (int i=0; entities != null && i < entities.length; i++) {
			IEntity<? extends IIdentity> data = entities[i]; 
			if (data == null) continue; 
			
			if (data.getIdentity() != null && 
				mDatabase.getTable(data.getClass()).contains(data.getIdentity())) { 
				throw new EntityException(data.getClass().getName() + " identity=" 
						+ data.getIdentity() + " exist");
			}
		}
		
		for (int i=0; entities != null && i < entities.length; i++) {
			IEntity<? extends IIdentity> data = entities[i]; 
			if (data == null) continue; 
			
			IIdentity id = mDatabase.insert(data); 
			onInserted(data, id); 
		}
		
		mSaved = true; 
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public final void update() throws EntityException {
		if (mSaved) throw new DBException("already updated"); 
		
		IEntity<? extends IIdentity>[] entities = getEntities(); 
		
		for (int i=0; entities != null && i < entities.length; i++) {
			IEntity<? extends IIdentity> data = entities[i]; 
			if (data == null) continue; 
			
			if (data.getIdentity() == null) 
				throw new EntityException(data.getClass().getName()+" identity is null");
			
			if (!mDatabase.getTable(data.getClass()).contains(data.getIdentity())) { 
				throw new EntityException(data.getClass().getName() + " identity=" 
						+ data.getIdentity() + " not found");
			}
		}
		
		for (int i=0; entities != null && i < entities.length; i++) {
			IEntity<? extends IIdentity> data = entities[i]; 
			if (data == null) continue; 
			
			IIdentity id = mDatabase.update(data); 
			onUpdated(data, id); 
		}
		
		mSaved = true; 
	}
	
	@Override
	public final void delete() throws EntityException {
		if (mSaved) throw new DBException("already updated"); 
		
		IEntity<? extends IIdentity>[] entities = getEntities(); 
		
		for (int i=0; entities != null && i < entities.length; i++) {
			IEntity<? extends IIdentity> data = entities[i]; 
			if (data == null) continue; 
			
			if (data.getIdentity() == null) 
				throw new EntityException(data.getClass().getName()+" identity is null");
		}
		
		for (int i=0; entities != null && i < entities.length; i++) {
			IEntity<? extends IIdentity> data = entities[i]; 
			if (data == null) continue; 
			
			if (mDatabase.delete(data)) 
				onDeleted(data); 
		}
		
		mSaved = true; 
	}
	
	@Override
	public final IIdentity delete(IEntity<? extends IIdentity> entity) throws EntityException { 
		if (entity == null) return null;
		
		if (entity.getIdentity() == null) 
			throw new EntityException(entity.getClass().getName()+" identity is null");
		
		if (mDatabase.delete(entity)) 
			return entity.getIdentity();
		
		return null;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public final IIdentity insert(IEntity<? extends IIdentity> entity) throws EntityException { 
		if (entity == null) return null;
		
		if (entity.getIdentity() != null && 
			mDatabase.getTable(entity.getClass()).contains(entity.getIdentity())) { 
			throw new EntityException(entity.getClass().getName() + " identity=" 
					+ entity.getIdentity() + " exist");
		}
		
		return mDatabase.insert(entity); 
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public final IIdentity update(IEntity<? extends IIdentity> entity) throws EntityException { 
		if (entity == null) return null;
		
		if (entity.getIdentity() == null) 
			throw new EntityException(entity.getClass().getName()+" identity is null");
		
		if (!mDatabase.getTable(entity.getClass()).contains(entity.getIdentity())) { 
			throw new EntityException(entity.getClass().getName() + " identity=" 
					+ entity.getIdentity() + " not found");
		}
		
		return mDatabase.update(entity); 
	}
	
	protected void onInserted(IEntity<? extends IIdentity> data, IIdentity id) {}
	protected void onUpdated(IEntity<? extends IIdentity> data, IIdentity id) {}
	protected void onDeleted(IEntity<? extends IIdentity> data) {}
	
}
