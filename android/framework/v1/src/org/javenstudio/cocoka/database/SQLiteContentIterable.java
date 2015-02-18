package org.javenstudio.cocoka.database;

import org.javenstudio.cocoka.Implements;
import org.javenstudio.cocoka.database.SQLiteEntityDB;
import org.javenstudio.common.entitydb.ICursor;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.db.EntityObserver;
import org.javenstudio.common.entitydb.db.EntitySetObserver;
import org.javenstudio.common.entitydb.type.LongIdentity;
import org.javenstudio.common.util.ContentIterable;
import org.javenstudio.common.util.EntityCursor;

public abstract class SQLiteContentIterable<E, T extends SQLiteEntityDB.TEntity> 
		extends ContentIterable<E, T> {

	public SQLiteContentIterable(EntityCursor<T> cursor) { 
		super(cursor); 
	}
	
	@SuppressWarnings("unchecked")
	protected ICursor<LongIdentity, T> getCursorImpl() { 
		return (ICursor<LongIdentity, T>)getCursor();
	}
	
	public final void registerObserver(final SQLiteCursorObserver observer) { 
		if (observer == null) return; 
		
		getCursorImpl().registerEntitySetObserver(new EntitySetObserver() {
				@Override 
				public void onChanged() {
					observer.notifyDataSetChanged(); 
				}
			});
		
		getCursorImpl().registerEntityObserver(
			new EntityObserver(Implements.getEntityObserverHandler()) {
				@Override 
				public void onChange(IEntity<?> data, int change) {
					observer.requery(); 
				} 
				@Override 
				public void onChange(int count, int change) {
					observer.requery(); 
				}
			});
	}
	
	public final boolean requery() { 
		return getCursorImpl().requery(); 
	}
	
	public final boolean requery(boolean notify) { 
		return getCursorImpl().requery(notify); 
	}
	
	public final void registerEntitySetObserver(EntitySetObserver observer) { 
		getCursorImpl().registerEntitySetObserver(observer); 
	}
	
	public final void registerEntityObserver(EntityObserver observer) { 
		getCursorImpl().registerEntityObserver(observer); 
	}
	
}
