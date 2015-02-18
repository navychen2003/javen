package org.javenstudio.common.entitydb.wrapper;

import org.javenstudio.common.entitydb.EntityException;
import org.javenstudio.common.entitydb.IDatabase;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.IUpdater;
import org.javenstudio.common.entitydb.db.AbstractUpdater;

public abstract class Updater {

	private final IUpdater mUpdater; 
	
	public Updater(IDatabase db) { 
		mUpdater = new AbstractUpdater(db) { 
				public IEntity<? extends IIdentity>[] getEntities() { 
					return Updater.this.getEntities();
				}
				protected void onInserted(IEntity<? extends IIdentity> data, IIdentity id) { 
					Updater.this.onInserted(data, id);
				}
				protected void onUpdated(IEntity<? extends IIdentity> data, IIdentity id) { 
					Updater.this.onUpdated(data, id);
				}
				protected void onDeleted(IEntity<? extends IIdentity> data) { 
					Updater.this.onDeleted(data);
				}
			};
	}
	
	protected IEntity<? extends IIdentity>[] getEntities() { 
		return null;
	}
	
	public final void saveOrUpdate() throws EntityException { mUpdater.saveOrUpdate(); }
	
	public final void insert() throws EntityException { mUpdater.insert(); }
	public final void update() throws EntityException { mUpdater.update(); }
	public final void delete() throws EntityException { mUpdater.delete(); }
	
	public final IIdentity insert(IEntity<? extends IIdentity> entity) throws EntityException { 
		return mUpdater.insert(entity);
	}
	
	public final IIdentity update(IEntity<? extends IIdentity> entity) throws EntityException { 
		return mUpdater.update(entity);
	}
	
	public final IIdentity delete(IEntity<? extends IIdentity> entity) throws EntityException { 
		return mUpdater.delete(entity);
	}
	
	protected void onInserted(IEntity<? extends IIdentity> data, IIdentity id) { 
		// do nothing
	}
	
	protected void onUpdated(IEntity<? extends IIdentity> data, IIdentity id) { 
		// do nothing
	}
	
	protected void onDeleted(IEntity<? extends IIdentity> data) { 
		// do nothing
	}
	
}
