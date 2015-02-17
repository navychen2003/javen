package org.javenstudio.common.entitydb.db;

import java.util.Arrays;
import java.util.Comparator;

import org.javenstudio.common.entitydb.DBException;
import org.javenstudio.common.entitydb.ICursor;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IEntityMatcher;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.IQuery;
import org.javenstudio.common.entitydb.ITable;
import org.javenstudio.common.util.Logger;

public abstract class AbstractCursor<K extends IIdentity, T extends IEntity<K>> 
		implements ICursor<K,T> {
	private static Logger LOG = Logger.getLogger(AbstractCursor.class);

	EntitySetObservable mEntitySetObservable = new EntitySetObservable();
	EntityObservable mEntityObservable = new EntityObservable();
	
	private final ITable<K,T> mTable; 
	private final IQuery<K,T> mQuery; 
	
	protected int mPosition = -1; 
	protected boolean mClosed = false;
	
	private IEntityMatcher<K,T> mNotifyMatcher = null; 
	private long mNotifiedTime = 0; 
	
	public AbstractCursor(ITable<K,T> table, IQuery<K,T> query) {
		mTable = table; 
		mQuery = query; 
	}
	
	@Override
	public ITable<K,T> getTable() {
		return mTable; 
	}
	
	@Override
	public IQuery<K,T> getQuery() {
		return mQuery; 
	}
	
	protected T[] sortEntities(T[] entities) {
		Comparator<T> comparator = getQuery().getComparator(); 
		
		if (entities != null && comparator != null && entities.length > 1) 
			Arrays.sort(entities, comparator); 
		
		return entities; 
	}
	
	public void notifyInvalidated() {
		mEntitySetObservable.notifyInvalidated(); 
	}
	
	@Override
	public void notifyEntitySetChange() {
        mEntitySetObservable.notifyChanged();
    }
	
	protected long getNotifiedTime() {
		return mNotifiedTime; 
	}
	
	@Override
	public final void setNotifyEntityMatcher(IEntityMatcher<K,T> matcher) { 
		mNotifyMatcher = matcher; 
	}
	
	@Override
	public void notifyEntitiesChange(int count, int change) {
		mNotifiedTime = System.currentTimeMillis();
		mEntityObservable.dispatchChange(count, change);
    }
	
	@Override
	public final void notifyEntityChange(T data, int change) {
		IEntityMatcher<K,T> matcher = mNotifyMatcher; 
		boolean queryMatch = getQuery().match(data); 
		boolean notifyMatch = (matcher == null) || matcher.match(data); 
		
		if (queryMatch || notifyMatch) {
			mNotifiedTime = System.currentTimeMillis();
			onNotifyEntityChange(data, change, queryMatch, notifyMatch); 
		}
	}
	
	protected void onNotifyEntityChange(T data, int change, boolean queryMatch, boolean notifyMatch) {
		mEntityObservable.dispatchChange(data, change); 
	}
	
	public EntitySetObservable getEntitySetObservable() {
        return mEntitySetObservable;
    }
	
	@Override
    public void registerEntitySetObserver(EntitySetObserver observer) {
        mEntitySetObservable.registerObserver(observer);
    }

	@Override
    public void unregisterEntitySetObserver(EntitySetObserver observer) {
        mEntitySetObservable.unregisterObserver(observer);
    }
	
	@Override
	public void registerEntityObserver(EntityObserver observer) {
        mEntityObservable.registerObserver(observer);
    }

	@Override
    public void unregisterEntityObserver(EntityObserver observer) {
        // cursor will unregister all observers when it close
        if (!mClosed) {
            mEntityObservable.unregisterObserver(observer);
        }
    }
	
	@Override
    protected void finalize() {
		if (!isClosed()) {
			LOG.warn("Finalizing a Cursor that has not been closed. " +
					"query = " + mQuery.getClass().getName() + 
					" table = "+mTable.getTableName()+" sql = "+mQuery.toSQL()); 
			close(); 
		}
	}
	
	@Override
	public boolean requery() {
		notifyEntitySetChange(); 
		return true; 
	}
	
	@Override
	public boolean requery(boolean notify) {
		if (notify) notifyEntitySetChange(); 
		return true; 
	}
	
	@Override
	public boolean isClosed() { 
		return mClosed; 
	} 
	
	@Override
	public void close() { 
		if (mClosed) return; 
		mClosed = true; 
		mEntityObservable.unregisterAll();
		notifyInvalidated();
	} 

	@Override
	public boolean moveToPosition(int position) {
		synchronized (this) {
			if (position >= -1 && position < getCount()) {
				mPosition = position; 
				return true; 
			}
			return false; 
		}
	}
	
	@Override 
	public int getPosition() {
		synchronized (this) {
			return mPosition; 
		}
	}
	
	@Override
	public boolean hasNext() {
		synchronized (this) {
			int position = mPosition + 1; 
			return position >= 0 && position < getCount(); 
		}
	}
	
	@Override 
	public T next() {
		synchronized (this) {
			return entityAt(++mPosition); 
		}
	}
	
	@Override
	public void remove() {
		throw new DBException("remove not support"); 
	}
	
	@Override
	public T get() { 
		synchronized (this) {
			return entityAt(mPosition); 
		}
	}
	
	@Override
	public boolean move(int offset) { 
		synchronized (this) {
			return moveToPosition(mPosition+offset); 
		}
	} 
	
	@Override
	public boolean moveToFirst()  { return moveToPosition(0); } 
	
	@Override
	public boolean moveToLast()  { 
		synchronized (this) {
			return moveToPosition(getCount()-1); 
		}
	} 
	
	@Override
	public boolean moveToNext()  { 
		synchronized (this) {
			return moveToPosition(mPosition+1); 
		}
	} 
	
	@Override
	public boolean moveToPrevious()  { 
		synchronized (this) {
			return moveToPosition(mPosition-1); 
		}
	} 
	
	@Override
	public boolean isFirst() {
		synchronized (this) {
			return getCount() > 0 && mPosition == 0; 
		}
	}
	
	@Override
	public boolean isLast() {
		synchronized (this) {
			return getCount() > 0 && mPosition == getCount() - 1; 
		}
	}
	
}
