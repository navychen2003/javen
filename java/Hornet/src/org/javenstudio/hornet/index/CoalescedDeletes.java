package org.javenstudio.hornet.index;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.index.BufferedDeletes;
import org.javenstudio.common.indexdb.index.DeletesStream;
import org.javenstudio.common.indexdb.index.FrozenDeletes;
import org.javenstudio.common.indexdb.util.PriorityQueue;

final class CoalescedDeletes {
	
	private final Map<IQuery,Integer> mQueries = new HashMap<IQuery,Integer>();
	private final List<Iterable<ITerm>> mIterables = new ArrayList<Iterable<ITerm>>();

	@Override
	public String toString() {
		// note: we could add/collect more debugging information
		return "CoalescedDeletes{termSets=" + mIterables.size() + ",queries=" + mQueries.size() + "}";
	}

	public void update(FrozenDeletes in) {
		mIterables.add(in.termsIterable());

		for (int queryIdx=0; queryIdx < in.getQueryCount(); queryIdx++) {
			final IQuery query = in.getQueryAt(queryIdx);
			mQueries.put(query, BufferedDeletes.MAX_INT);
		}
	}

	public Iterable<ITerm> termsIterable() {
		return new Iterable<ITerm>() {
				@Override
				public Iterator<ITerm> iterator() {
					ArrayList<Iterator<ITerm>> subs = new ArrayList<Iterator<ITerm>>(mIterables.size());
					for (Iterable<ITerm> iterable : mIterables) {
						subs.add(iterable.iterator());
					}
					return mergedIterator(subs);
				}
			};
	}

	public Iterable<DeletesStream.QueryAndLimit> queriesIterable() {
		return new Iterable<DeletesStream.QueryAndLimit>() {
				@Override
				public Iterator<DeletesStream.QueryAndLimit> iterator() {
					return new Iterator<DeletesStream.QueryAndLimit>() {
							private final Iterator<Map.Entry<IQuery,Integer>> mIter = 
									mQueries.entrySet().iterator();
		
							@Override
							public boolean hasNext() {
								return mIter.hasNext();
							}
		
							@Override
							public DeletesStream.QueryAndLimit next() {
								final Map.Entry<IQuery,Integer> ent = mIter.next();
								return new DeletesStream.QueryAndLimit(ent.getKey(), ent.getValue());
							}
		
							@Override
							public void remove() {
								throw new UnsupportedOperationException();
							}
						};
				}
			};
	}
  
	/** provides a merged view across multiple iterators */
	static Iterator<ITerm> mergedIterator(final List<Iterator<ITerm>> iterators) {
		return new Iterator<ITerm>() {
			private TermMergeQueue mQueue = new TermMergeQueue(iterators.size());
			private SubIterator[] mTop = new SubIterator[iterators.size()];
			private ITerm mCurrent;
			private int mNumTop;
      
			{
				int index = 0;
				for (Iterator<ITerm> iterator : iterators) {
					if (iterator.hasNext()) {
						SubIterator sub = new SubIterator();
						sub.mCurrent = iterator.next();
						sub.mIterator = iterator;
						sub.mIndex = index++;
						mQueue.add(sub);
					}
				}
			}
      
			@Override
			public boolean hasNext() {
				if (mQueue.size() > 0) 
					return true;
        
				for (int i = 0; i < mNumTop; i++) {
					if (mTop[i].mIterator.hasNext()) 
						return true;
				}
				
				return false;
			}
      
			@Override
			public ITerm next() {
				// restore queue
				pushTop();
        
				// gather equal top fields
				if (mQueue.size() > 0) 
					pullTop();
				else 
					mCurrent = null;
				
				return mCurrent;
			}
      
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
      
			private void pullTop() {
				// extract all subs from the queue that have the same top term
				assert mNumTop == 0;
				while (true) {
					mTop[mNumTop++] = mQueue.pop();
					if (mQueue.size() == 0 || !(mQueue.top()).mCurrent.equals(mTop[0].mCurrent)) 
						break;
				}
				mCurrent = mTop[0].mCurrent;
			}
      
			private void pushTop() {
				// call next() on each top, and put back into queue
				for (int i = 0; i < mNumTop; i++) {
					if (mTop[i].mIterator.hasNext()) {
						mTop[i].mCurrent = mTop[i].mIterator.next();
						mQueue.add(mTop[i]);
					} else {
						// no more terms
						mTop[i].mCurrent = null;
					}
				}
				mNumTop = 0;
			}
		};
	}
  
	private static class SubIterator {
		private Iterator<ITerm> mIterator;
		private ITerm mCurrent;
		private int mIndex;
	}
  
	private static class TermMergeQueue extends PriorityQueue<SubIterator> {
		TermMergeQueue(int size) {
			super(size);
		}

		@Override
		protected boolean lessThan(SubIterator a, SubIterator b) {
			final int cmp = a.mCurrent.compareTo(b.mCurrent);
			if (cmp != 0) 
				return cmp < 0;
			else 
				return a.mIndex < b.mIndex;
		}
	}
	
}
