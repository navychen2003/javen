package org.javenstudio.common.util;

public abstract class ContentIterable<E, T> implements EntityIterable<E> {

	private final EntityCursor<T> mCursor; 
	
	public ContentIterable(EntityCursor<T> cursor) { 
		mCursor = cursor; 
	}
	
	protected final EntityCursor<T> getCursor() { 
		return mCursor; 
	}
	
	protected abstract E newContent(T entity); 
	
	public final void close() { mCursor.close(); }
	public final boolean isClosed() { return mCursor.isClosed(); }
	public final void remove() {}
	
	public final int getCount() { return mCursor.getCount(); }
	public final int getPosition() { return mCursor.getPosition(); }
	
	public final boolean isFirst() { return mCursor.isFirst(); }
	public final boolean isLast() { return mCursor.isLast(); }
	public final boolean hasNext() { return mCursor.hasNext(); }
	
	public final boolean moveToFirst() { return mCursor.moveToFirst(); }
	
	@Override 
	public final E entityAt(int position) { 
		if (isClosed()) return null; 
		T entity = mCursor.entityAt(position); 
		if (entity != null) 
			return newContent(entity); 
		return null; 
	}
	
	@Override 
	public final E next() { 
		if (isClosed()) return null; 
		T entity = mCursor.next(); 
		if (entity != null) 
			return newContent(entity); 
		return null; 
	}
	
}
