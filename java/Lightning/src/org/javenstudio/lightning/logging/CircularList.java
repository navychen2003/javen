package org.javenstudio.lightning.logging;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * FIFO Circular List.
 * 
 * Once the size is reached, it will overwrite previous entries
 */
public class CircularList<T> implements Iterable<T> {
	
	private T[] mData;
	private int mHead = 0;
	private int mTail = 0;
	private int mSize = 0;

	@SuppressWarnings("unchecked")
  	public CircularList(int size) {
		mData = (T[])new Object[size];
	}

	@SuppressWarnings("unchecked")
	public synchronized void resize(int newsize) {
		if (newsize == mSize) 
			return;
    
		T[] vals = (T[])new Object[newsize];
		int i = 0;
		
		if (newsize > mSize) {
			for (i=0; i < mSize; i++) {
				vals[i] = mData[convert(i)];
			}
		} else {
			int off = mSize - newsize;
			for (i=0; i < newsize; i++) {
				vals[i] = mData[convert(i+off)];
			}
		}
		
		mData = vals;
		mHead = 0;
		mTail = i;
	}

	private int convert(int index) {
		return (index + mHead) % mData.length;
	}

	public boolean isEmpty() {
		return mHead == mTail; // or size == 0
	}

	public int size() {
		return mSize;
	}
  
	public int getBufferSize() {
		return mData.length;
	}

	private void checkIndex(int index) {
		if (index >= mSize || index < 0) {
			throw new IndexOutOfBoundsException(
					"Index: " + index + ", Size: " + mSize);
		}
	}

	public T get(int index) {
		checkIndex(index);
		return mData[convert(index)];
	}

	public synchronized void add(T o) {
		mData[mTail] = o;
		mTail = (mTail+1)%mData.length;
		
    	if (mSize == mData.length) 
    		mHead = (mHead+1)%mData.length;
    
    	mSize ++;
    	
    	if (mSize > mData.length) 
    		mSize = mData.length;
	}

	public synchronized void clear() {
		for (int i=0; i < mData.length; i++) {
			mData[i] = null;  // for GC
		}
		mHead = mTail = mSize = 0;
	}

	public List<T> toList() {
		List<T> list = new ArrayList<T>(mSize);
		for (int i=0; i < mSize; i++) {
			list.add(mData[convert(i)] );
		}
		return list;
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append( "[" );
		for (int i=0; i < mSize; i++) {
			if (i > 0) 
				str.append(",");
			
			str.append(mData[convert(i)]);
		}
		str.append("]");
		return str.toString();
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			int mIdx = 0;

			@Override
			public boolean hasNext() {
				return mIdx < mSize;
			}

			@Override
			public T next() {
				return get(mIdx++);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
}
