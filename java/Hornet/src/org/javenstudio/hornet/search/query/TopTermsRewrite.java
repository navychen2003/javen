package org.javenstudio.hornet.search.query;

import java.io.IOException;
import java.util.PriorityQueue;
import java.util.Comparator;

import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.index.term.Term;
import org.javenstudio.common.indexdb.util.ArrayUtil;

/**
 * Base rewrite method for collecting only the top terms
 * via a priority queue.
 * Only public to be accessible by spans package.
 */
public abstract class TopTermsRewrite<Q extends IQuery> 
		extends TermCollectingRewrite<Q> {

	private final int mSize;
  
	/** 
	 * Create a TopTermsBooleanQueryRewrite for 
	 * at most <code>size</code> terms.
	 * <p>
	 * NOTE: if {@link BooleanQuery#getMaxClauseCount} is smaller than 
	 * <code>size</code>, then it will be used instead. 
	 */
	public TopTermsRewrite(int size) {
		mSize = size;
	}
  
	/** return the maximum priority queue size */
	public int getSize() {
		return mSize;
	}
  
	/** 
	 * return the maximum size of the priority queue (for boolean rewrites 
	 * this is BooleanQuery#getMaxClauseCount). 
	 */
	protected abstract int getMaxSize();
  
	@Override
	public final Q rewrite(final IIndexReader reader, final MultiTermQuery query) throws IOException {
		final int maxSize = Math.min(mSize, getMaxSize());
		final PriorityQueue<ScoreTerm> stQueue = new PriorityQueue<ScoreTerm>();
		
		collectTerms(reader, query, new TopTermCollector(stQueue, maxSize));
    
		final Q q = getTopLevelQuery();
		final ScoreTerm[] scoreTerms = stQueue.toArray(new ScoreTerm[stQueue.size()]);
		ArrayUtil.mergeSort(scoreTerms, sScoreTermSortByTermComp);
    
		for (final ScoreTerm st : scoreTerms) {
			final Term term = new Term(query.getFieldName(), st.getBytes());
			assert reader.getDocFreq(term) == st.getTermState().getDocFreq() : 
				"reader DF is " + reader.getDocFreq(term) + " vs " + st.getTermState().getDocFreq() 
				+ " term=" + term;
			
			addClause(q, term, st.getTermState().getDocFreq(), 
					query.getBoost() * st.getBoost(), st.getTermState()); // add to query
		}
		
		return q;
	}

	@Override
	public int hashCode() {
		return 31 * mSize;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final TopTermsRewrite<?> other = (TopTermsRewrite<?>) obj;
		if (mSize != other.mSize) return false;
		return true;
	}
  
	private static final Comparator<ScoreTerm> sScoreTermSortByTermComp = 
			new Comparator<ScoreTerm>() {
		@Override
		public int compare(ScoreTerm st1, ScoreTerm st2) {
			assert st1.getTermComparator() == st2.getTermComparator() :
				"term comparator should not change between segments";
			return st1.getTermComparator().compare(st1.getBytes(), st2.getBytes());
		}
    };

}
