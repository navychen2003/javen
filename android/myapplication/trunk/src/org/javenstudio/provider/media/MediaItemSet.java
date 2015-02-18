package org.javenstudio.provider.media;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public class MediaItemSet<T> implements Collection<T> {

	private final Set<T> mItems;
	private final int mCount;
	
	public MediaItemSet(int count, Comparator<T> comparator) { 
		mCount = count > 0 ? count : 1;
		mItems = new TreeSet<T>(comparator);
	}
	
	protected boolean isAddable(T item) { return item != null; }
	protected boolean isFull() { return size() >= mCount; }

	public final int getCapacity() { return mCount; }
	
	@Override
	public final boolean add(T object) {
		if (object == null || isFull()) 
			return false;
		
		if (isAddable(object)) 
			return mItems.add(object);
		
		return false;
	}

	@Override
	public final boolean addAll(Collection<? extends T> collection) {
		if (collection == null) return false;
		
		boolean added = false;
		
		for (T item : collection) { 
			if (add(item))
				added = true;
		}
		
		return added;
	}

	@Override
	public final void clear() {
		mItems.clear();
	}

	@Override
	public final boolean contains(Object object) {
		return mItems.contains(object);
	}

	@Override
	public final boolean containsAll(Collection<?> collection) {
		return mItems.containsAll(collection);
	}

	@Override
	public final boolean isEmpty() {
		return mItems.isEmpty();
	}

	@Override
	public final Iterator<T> iterator() {
		return mItems.iterator();
	}

	@Override
	public final boolean remove(Object object) {
		return mItems.remove(object);
	}

	@Override
	public final boolean removeAll(Collection<?> collection) {
		return mItems.removeAll(collection);
	}

	@Override
	public final boolean retainAll(Collection<?> collection) {
		return mItems.retainAll(collection);
	}

	@Override
	public final int size() {
		return mItems.size();
	}

	@Override
	public final Object[] toArray() {
		return mItems.toArray();
	}

	@Override
	public final <K> K[] toArray(K[] array) {
		return mItems.toArray(array);
	}
	
}
