package org.javenstudio.common.entitydb.wrapper;

import org.javenstudio.common.entitydb.ICursor;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IEntityMatcher;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.db.EntityObserver;
import org.javenstudio.common.entitydb.db.EntitySetObserver;

public class Cursor<K extends IIdentity, T extends IEntity<K>> {

	private final ICursor<K,T> mCursor; 
	
	public Cursor(ICursor<K,T> cursor) { 
		mCursor = cursor;
	}
	
	public final boolean requery() { 
		return mCursor.requery();
	}
	
	public final boolean requery(boolean notify) { 
		return mCursor.requery(notify);
	}
	
	public final boolean hasNext() { 
		return mCursor.hasNext();
	}
	
	public final T next() { 
		return mCursor.next();
	}
	
	public final boolean isClosed() { 
		return mCursor.isClosed();
	}
	
	public final void close() { 
		mCursor.close();
	}
	
	public final int getCount() { 
		return mCursor.getCount();
	}
	
	public final int getPosition() { 
		return mCursor.getPosition();
	}
	
	public final T entityAt(int position) { 
		return mCursor.entityAt(position);
	}
	
	public final void registerEntitySetObserver(EntitySetObserver observer) { 
		mCursor.registerEntitySetObserver(observer);
	}
	
	public final void unregisterEntitySetObserver(EntitySetObserver observer) { 
		mCursor.unregisterEntitySetObserver(observer);
	}
	
	public final void registerEntityObserver(EntityObserver observer) { 
		mCursor.registerEntityObserver(observer);
	}
	
	public final void unregisterEntityObserver(EntityObserver observer) { 
		mCursor.unregisterEntityObserver(observer);
	}
	
	public final void setNotifyEntityMatcher(IEntityMatcher<K,T> matcher) { 
		mCursor.setNotifyEntityMatcher(matcher);
	}
	
}
