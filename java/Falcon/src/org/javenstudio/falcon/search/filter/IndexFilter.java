package org.javenstudio.falcon.search.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IDocIdSet;
import org.javenstudio.common.indexdb.IDocIdSetIterator;
import org.javenstudio.common.indexdb.IFilter;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.IWeight;
import org.javenstudio.common.indexdb.search.DocIdSet;
import org.javenstudio.common.indexdb.search.DocIdSetIterator;
import org.javenstudio.common.indexdb.search.Filter;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.falcon.search.hits.DocSet;

public class IndexFilter extends Filter {
	
	private final DocSet mFilter;
	private final IFilter mTopFilter;
	private final List<IWeight> mWeights;

	public IndexFilter(DocSet filter, List<IWeight> weights) {
		mFilter = filter;
		mWeights = weights;
		mTopFilter = (filter == null) ? null : 
			filter.getTopFilter();
	}

	public final DocSet getFilter() { return mFilter; }
	
	@Override
	public IDocIdSet getDocIdSet(IAtomicReaderRef context, 
			Bits acceptDocs) throws IOException {
		IDocIdSet sub = (mTopFilter == null) ? null : 
			mTopFilter.getDocIdSet(context, acceptDocs);
		
		if (mWeights.size() == 0) 
			return sub;
		
		return new FilterSet(sub, context);
	}

	private class FilterSet extends DocIdSet {
		
		private IDocIdSet mDocIdSet;
		private IAtomicReaderRef mContext;

		public FilterSet(IDocIdSet docIdSet, IAtomicReaderRef context) {
			mDocIdSet = docIdSet;
			mContext = context;
		}

		@Override
		public IDocIdSetIterator iterator() throws IOException {
			List<IDocIdSetIterator> iterators = 
					new ArrayList<IDocIdSetIterator>(mWeights.size()+1);
			
			if (mDocIdSet != null) {
				IDocIdSetIterator iter = mDocIdSet.iterator();
				if (iter == null) return null;
				iterators.add(iter);
			}
			
			for (IWeight w : mWeights) {
				IScorer scorer = w.getScorer(mContext, true, false, 
						mContext.getReader().getLiveDocs());
				
				if (scorer == null) return null;
				iterators.add(scorer);
			}
			
			if (iterators.size() == 0) 
				return null;
			
			if (iterators.size() == 1) 
				return iterators.get(0);
			
			if (iterators.size() == 2) 
				return new DualFilterIterator(iterators.get(0), iterators.get(1));
			
			return new FilterIterator(
					iterators.toArray(new DocIdSetIterator[iterators.size()]));
		}

		@Override
		public Bits getBits() throws IOException {
			return null;  // don't use random access
		}
	}

	private static class FilterIterator extends DocIdSetIterator {
		
		private final IDocIdSetIterator[] mIterators;
		private final IDocIdSetIterator mFirst;

		public FilterIterator(IDocIdSetIterator[] iterators) {
			mIterators = iterators;
			mFirst = iterators[0];
		}

		@Override
		public int getDocID() {
			return mFirst.getDocID();
		}

		private int doNext(int doc) throws IOException {
			int which = 0; // index of the iterator with the highest id
			int i = 1;
			
			outer: for(;;) {
				for (; i < mIterators.length; i++) {
					if (i == which) continue;
					
					IDocIdSetIterator iter = mIterators[i];
					int next = iter.advance(doc);
					
					if (next != doc) {
						doc = next;
						which = i;
						i = 0;
						
						continue outer;
					}
				}
				
				return doc;
			}
		}

		@Override
		public int nextDoc() throws IOException {
			return doNext(mFirst.nextDoc());
		}

		@Override
		public int advance(int target) throws IOException {
			return doNext(mFirst.advance(target));
		}
	}

	private static class DualFilterIterator extends DocIdSetIterator {
		
		private final IDocIdSetIterator mIterA;
		private final IDocIdSetIterator mIterB;

		public DualFilterIterator(IDocIdSetIterator a, IDocIdSetIterator b) {
			mIterA = a;
			mIterB = b;
		}

		@Override
		public int getDocID() {
			return mIterA.getDocID();
		}

		@Override
		public int nextDoc() throws IOException {
			int doc = mIterA.nextDoc();
			for (;;) {
				int other = mIterB.advance(doc);
				if (other == doc) return doc;
				
				doc = mIterA.advance(other);
				if (other == doc) return doc;
			}
		}

		@Override
		public int advance(int target) throws IOException {
			int doc = mIterA.advance(target);
			for (;;) {
				int other = mIterB.advance(doc);
				if (other == doc) return doc;
				
				doc = mIterA.advance(other);
				if (other == doc) return doc;
			}
		}
	}

}
