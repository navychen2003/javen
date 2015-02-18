package org.javenstudio.falcon.util;

import java.util.concurrent.atomic.AtomicInteger;

import org.javenstudio.falcon.ErrorException;

/** 
 * Keep track of a reference count on a resource and close it when
 * the count hits zero.
 *
 * By itself, this class could have some race conditions
 * since there is no synchronization between the refcount
 * check and the close. Use in reference counting searchers
 * is safe since the count can only hit zero if it's unregistered (and
 * hence incref() will not be called again on it).
 *
 */
public abstract class RefCounted<Type> {
  
	private final AtomicInteger mRefCount = new AtomicInteger();
	private final Type mResource;

	public RefCounted(Type resource) {
		mResource = resource;
	}

	public final int getRefCount() {
		return mRefCount.get();
	}

	public final Type get() {
		return mResource;
	}
	
	public void increaseRef() {
		mRefCount.incrementAndGet();
	}

	public void decreaseRef() throws ErrorException {
		if (mRefCount.decrementAndGet() == 0) 
			close();
	}

	protected abstract void close() throws ErrorException;
	
	@Override
	public String toString() { 
		return "RefCounted{" + get() + ", refcount=" + getRefCount() + "}";
	}
	
}
