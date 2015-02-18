package org.javenstudio.hornet.search.query;

import java.io.IOException;
import java.util.Set;

import org.javenstudio.common.indexdb.IAtomicReader;
import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IDocsEnum;
import org.javenstudio.common.indexdb.IExactSimilarityScorer;
import org.javenstudio.common.indexdb.IExplanation;
import org.javenstudio.common.indexdb.IIndexReaderRef;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.common.indexdb.ISimilarityWeight;
import org.javenstudio.common.indexdb.ISimilarity;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.ITermContext;
import org.javenstudio.common.indexdb.ITermState;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.IWeight;
import org.javenstudio.common.indexdb.index.segment.ReaderUtil;
import org.javenstudio.common.indexdb.search.ComplexExplanation;
import org.javenstudio.common.indexdb.search.Explanation;
import org.javenstudio.common.indexdb.search.Query;
import org.javenstudio.common.indexdb.search.Weight;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.StringHelper;
import org.javenstudio.hornet.search.scorer.MatchOnlyTermScorer;
import org.javenstudio.hornet.search.scorer.TermScorer;

/** 
 * A Query that matches documents containing a term.
 * This may be combined with other terms with a {@link BooleanQuery}.
 */
public class TermQuery extends Query {
	
	private final ITermContext mPerReaderTermState;
	private final ITerm mTerm;
	private final int mDocFreq;

	final class TermWeight extends Weight {
		private final ISimilarity mSimilarity;
		private final ISimilarityWeight mStats;
		private final ITermContext mTermStates;

		public TermWeight(ISearcher searcher, ITermContext termStates)
				throws IOException {
			assert termStates != null : "TermContext must not be null";
			mTermStates = termStates;
			mSimilarity = searcher.getSimilarity();
			mStats = mSimilarity.computeWeight(getBoost(), 
					searcher.getCollectionStatistics(mTerm.getField()), 
					searcher.getTermStatistics(mTerm, termStates));
		}

		@Override
		public String toString() { return "weight(" + TermQuery.this + ")"; }

		@Override
		public Query getQuery() { return TermQuery.this; }

		@Override
		public float getValueForNormalization() {
			return mStats.getValueForNormalization();
		}

		@Override
		public void normalize(float queryNorm, float topLevelBoost) {
			mStats.normalize(queryNorm, topLevelBoost);
		}

		@Override
		public IScorer getScorer(IAtomicReaderRef context, boolean scoreDocsInOrder,
				boolean topScorer, Bits acceptDocs) throws IOException {
			assert mTermStates.getTopReader() == ReaderUtil.getTopLevel(context) : 
				"The top-reader used to create Weight (" + mTermStates.getTopReader() + ") " + 
				"is not the same as the current reader's top-reader (" + 
				ReaderUtil.getTopLevel(context);
			
			final ITermsEnum termsEnum = getTermsEnum(context);
			if (termsEnum == null) 
				return null;
			
			IDocsEnum docs = termsEnum.getDocs(acceptDocs, null);
			if (docs != null) {
				return new TermScorer(this, docs, createDocScorer(context));
				
			} else {
				// Index does not store freq info
				docs = termsEnum.getDocs(acceptDocs, null, 0);
				assert docs != null;
				
				return new MatchOnlyTermScorer(this, docs, createDocScorer(context));
			}
		}
    
		/** Creates an {@link ExactSimScorer} for this {@link TermWeight}*/
		IExactSimilarityScorer createDocScorer(IAtomicReaderRef context) throws IOException {
			return mSimilarity.getExactSimilarityScorer(mStats, context);
		}
    
		/**
		 * Returns a {@link TermsEnum} positioned at this weights Term or null if
		 * the term does not exist in the given context
		 */
		ITermsEnum getTermsEnum(IAtomicReaderRef context) throws IOException {
			final ITermState state = mTermStates.get(context.getOrd());
			if (state == null) { // term is not present in that reader
				assert termNotInReader(context.getReader(), mTerm.getField(), mTerm.getBytes()) : 
					"no termstate found but term exists in reader term=" + mTerm;
				return null;
			}
			
			final ITermsEnum termsEnum = context.getReader().getTerms(mTerm.getField()).iterator(null);
			termsEnum.seekExact(mTerm.getBytes(), state);
			
			return termsEnum;
		}
    
		private boolean termNotInReader(IAtomicReader reader, String field, BytesRef bytes) 
				throws IOException {
			// only called from assert
			return reader.getDocFreq(field, bytes) == 0;
		}
    
		@Override
		public IExplanation explain(IAtomicReaderRef context, int doc) throws IOException {
			IScorer scorer = getScorer(context, true, false, context.getReader().getLiveDocs());
			if (scorer != null) {
				int newDoc = scorer.advance(doc);
				if (newDoc == doc) {
					float freq = scorer.getFreq();
					IExactSimilarityScorer docScorer = mSimilarity.getExactSimilarityScorer(mStats, context);
					
					ComplexExplanation result = new ComplexExplanation();
					result.setDescription("weight("+getQuery()+" in "+doc+") [" + mSimilarity.getClass().getSimpleName() + "], result of:");
					
					IExplanation scoreExplanation = docScorer.explain(doc, new Explanation(freq, "termFreq=" + freq));
					result.addDetail(scoreExplanation);
					result.setValue(scoreExplanation.getValue());
					result.setMatch(true);
					
					return result;
				}
			}
			return new ComplexExplanation(false, 0.0f, "no matching term");      
		}
	}

	/** Constructs a query for the term <code>t</code>. */
	public TermQuery(ITerm t) {
		this(t, -1);
	}

	/** 
	 * Expert: constructs a TermQuery that will use the
	 *  provided docFreq instead of looking up the docFreq
	 *  against the searcher. 
	 */
	public TermQuery(ITerm t, int docFreq) {
		mTerm = t;
		mDocFreq = docFreq;
		mPerReaderTermState = null;
	}
  
	/** 
	 * Expert: constructs a TermQuery that will use the
	 *  provided docFreq instead of looking up the docFreq
	 *  against the searcher. 
	 */
	public TermQuery(ITerm t, ITermContext states) {
		assert states != null;
		mTerm = t;
		mDocFreq = states.getDocFreq();
		mPerReaderTermState = states;
	}

	/** Returns the term of this query. */
	public ITerm getTerm() { return mTerm; }

	@Override
	public IWeight createWeight(ISearcher searcher) throws IOException {
		final IIndexReaderRef context = searcher.getTopReaderContext();
		final ITermContext termState;
		if (mPerReaderTermState == null || mPerReaderTermState.getTopReader() != context) {
			// make TermQuery single-pass if we don't have a PRTS or if the context differs!
			termState = searcher.getContext().buildTermContext(context, mTerm, true); // cache term lookups!
		} else {
			// PRTS was pre-build for this IS
			termState = this.mPerReaderTermState;
		}

		// we must not ignore the given docFreq - if set use the given value (lie)
		if (mDocFreq != -1)
			termState.setDocFreq(mDocFreq);
    
		return new TermWeight(searcher, termState);
	}

	@Override
	public void extractTerms(Set<ITerm> terms) {
		terms.add(getTerm());
	}

	/** Prints a user-readable version of this query. */
	@Override
	public String toString(String field) {
		StringBuilder buffer = new StringBuilder();
		if (!mTerm.getField().equals(field)) {
			buffer.append(mTerm.getField());
			buffer.append(":");
		}
		buffer.append(mTerm.getText());
		buffer.append(StringHelper.toBoostString(getBoost()));
		return buffer.toString();
	}

	/** Returns true iff <code>o</code> is equal to this. */
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof TermQuery))
			return false;
		TermQuery other = (TermQuery)o;
		return (this.getBoost() == other.getBoost()) && this.mTerm.equals(other.mTerm);
	}

	/** Returns a hash code value for this object.*/
	@Override
	public int hashCode() {
		return Float.floatToIntBits(getBoost()) ^ mTerm.hashCode();
	}

}
