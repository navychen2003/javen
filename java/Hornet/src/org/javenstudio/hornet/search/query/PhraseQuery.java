package org.javenstudio.hornet.search.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import org.javenstudio.common.indexdb.IAtomicReader;
import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IDocsAndPositionsEnum;
import org.javenstudio.common.indexdb.IExplanation;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.IIndexReaderRef;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.common.indexdb.ISimilarityWeight;
import org.javenstudio.common.indexdb.ISimilarity;
import org.javenstudio.common.indexdb.ISloppySimilarityScorer;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.ITermContext;
import org.javenstudio.common.indexdb.ITermState;
import org.javenstudio.common.indexdb.ITermStatistics;
import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.index.term.Term;
import org.javenstudio.common.indexdb.search.ComplexExplanation;
import org.javenstudio.common.indexdb.search.Explanation;
import org.javenstudio.common.indexdb.search.Query;
import org.javenstudio.common.indexdb.search.Weight;
import org.javenstudio.common.indexdb.util.ArrayUtil;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.indexdb.util.StringHelper;
import org.javenstudio.hornet.index.term.TermContext;

/** 
 * A Query that matches documents containing a particular sequence of terms.
 * A PhraseQuery is built by QueryParser for input like <code>"new york"</code>.
 * 
 * <p>This query may be combined with other terms or queries with a {@link BooleanQuery}.
 */
public class PhraseQuery extends Query {
  
	private ArrayList<Term> mTerms = new ArrayList<Term>(4);
	private ArrayList<Integer> mPositions = new ArrayList<Integer>(4);
	
	private String mField = null;
	private int mMaxPosition = 0;
	private int mSlop = 0;

	/** Constructs an empty phrase query. */
	public PhraseQuery() {}

	/** 
	 * Sets the number of other words permitted between words in query phrase.
	 * If zero, then this is an exact phrase search.  For larger values this works
	 * like a <code>WITHIN</code> or <code>NEAR</code> operator.
	 *
	 * <p>The slop is in fact an edit-distance, where the units correspond to
	 * moves of terms in the query phrase out of position.  For example, to switch
	 * the order of two words requires two moves (the first move places the words
	 * atop one another), so to permit re-orderings of phrases, the slop must be
	 * at least two.
	 *
	 * <p>More exact matches are scored higher than sloppier matches, thus search
	 * results are sorted by exactness.
	 *
	 * <p>The slop is zero by default, requiring exact matches.
	 */
	public void setSlop(int s) { mSlop = s; }
	
	/** Returns the slop.  See setSlop(). */
	public int getSlop() { return mSlop; }

	/**
	 * Adds a term to the end of the query phrase.
	 * The relative position of the term is the one immediately after the last term added.
	 */
	public void add(Term term) {
		int position = 0;
		if (mPositions.size() > 0)
			position = mPositions.get(mPositions.size()-1).intValue() + 1;

		add(term, position);
	}

	/**
	 * Adds a term to the end of the query phrase.
	 * The relative position of the term within the phrase is specified explicitly.
	 * This allows e.g. phrases with more than one term at the same position
	 * or phrases with gaps (e.g. in connection with stopwords).
	 */
	public void add(Term term, int position) {
		if (mTerms.size() == 0) {
			mField = term.getField();
			
		} else if (!term.getField().equals(mField)) {
			throw new IllegalArgumentException("All phrase terms must be in the same field: " + term);
		}

		mTerms.add(term);
		mPositions.add(Integer.valueOf(position));
		
		if (position > mMaxPosition) 
			mMaxPosition = position;
	}

	/** Returns the set of terms in this phrase. */
	public Term[] getTerms() {
		return mTerms.toArray(new Term[0]);
	}

	/** Returns the relative positions of terms in this phrase. */
	public int[] getPositions() {
		int[] result = new int[mPositions.size()];
		for (int i = 0; i < mPositions.size(); i++) {
			result[i] = mPositions.get(i).intValue();
		}
		return result;
	}

	@Override
	public IQuery rewrite(IIndexReader reader) throws IOException {
		if (mTerms.isEmpty()) {
			BooleanQuery bq = new BooleanQuery();
			bq.setBoost(getBoost());
			return bq;
			
		} else if (mTerms.size() == 1) {
			TermQuery tq = new TermQuery(mTerms.get(0));
			tq.setBoost(getBoost());
			return tq;
			
		} else
			return super.rewrite(reader);
	}

	private class PhraseWeight extends Weight {
		
		private final ISimilarity mSimilarity;
		private final ISimilarityWeight mStats;
		private transient ITermContext mStates[];

		public PhraseWeight(ISearcher searcher) throws IOException {
			mSimilarity = searcher.getSimilarity();
			
			final IIndexReaderRef context = searcher.getTopReaderContext();
			mStates = new ITermContext[mTerms.size()];
			
			ITermStatistics termStats[] = new ITermStatistics[mTerms.size()];
			for (int i = 0; i < mTerms.size(); i++) {
				final Term term = mTerms.get(i);
				mStates[i] = TermContext.build(context, term, true);
				termStats[i] = searcher.getTermStatistics(term, mStates[i]);
			}
			
			mStats = mSimilarity.computeWeight(getBoost(), 
					searcher.getCollectionStatistics(mField), termStats);
		}

		@Override
		public String toString() { 
			return "weight(" + PhraseQuery.this + ")"; 
		}

		@Override
		public Query getQuery() { return PhraseQuery.this; }

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
			assert !mTerms.isEmpty();
			
			final IAtomicReader reader = context.getReader();
			final Bits liveDocs = acceptDocs;
			PostingsAndFreq[] postingsFreqs = new PostingsAndFreq[mTerms.size()];

			final ITerms fieldTerms = reader.getTerms(mField);
			if (fieldTerms == null) 
				return null;

			// Reuse single TermsEnum below:
			final ITermsEnum te = fieldTerms.iterator(null);
      
			for (int i = 0; i < mTerms.size(); i++) {
				final Term t = mTerms.get(i);
				final ITermState state = mStates[i].get(context.getOrd());
				if (state == null) { 
					/** term doesnt exist in this segment */
					//assert termNotInReader(reader, t): "no termstate found but term exists in reader";
					return null;
				}
				
				te.seekExact(t.getBytes(), state);
				IDocsAndPositionsEnum postingsEnum = te.getDocsAndPositions(liveDocs, null, 0);

				// PhraseQuery on a field that did not index positions.
				if (postingsEnum == null) {
					assert te.seekExact(t.getBytes(), false) : "termstate found but no term exists in reader";
					
					// term does exist, but has no positions
					throw new IllegalStateException("field \"" + t.getField() 
							+ "\" was indexed without position data; cannot run PhraseQuery (term=" 
							+ t.getText() + ")");
				}
				
				postingsFreqs[i] = new PostingsAndFreq(postingsEnum, 
						te.getDocFreq(), mPositions.get(i).intValue(), t);
			}

			// sort by increasing docFreq order
			if (mSlop == 0) 
				ArrayUtil.mergeSort(postingsFreqs);

			if (mSlop == 0) { // optimize exact case
				ExactPhraseScorer s = new ExactPhraseScorer(this, postingsFreqs, 
						mSimilarity.getExactSimilarityScorer(mStats, context));
				
				if (s.hasNoDocs()) 
					return null;
				else 
					return s;
				
			} else {
				return new SloppyPhraseScorer(this, postingsFreqs, mSlop, 
						mSimilarity.getSloppySimilarityScorer(mStats, context));
			}
		}
    
		// only called from assert
		@SuppressWarnings("unused")
		private boolean termNotInReader(IAtomicReader reader, Term term) throws IOException {
			return reader.getDocFreq(term) == 0;
		}

		@Override
		public IExplanation explain(IAtomicReaderRef context, int doc) throws IOException {
			IScorer scorer = getScorer(context, true, false, context.getReader().getLiveDocs());
			if (scorer != null) {
				int newDoc = scorer.advance(doc);
				if (newDoc == doc) {
					float freq = scorer.getFreq();
					ISloppySimilarityScorer docScorer = 
							mSimilarity.getSloppySimilarityScorer(mStats, context);
					
					ComplexExplanation result = new ComplexExplanation();
					result.setDescription("weight("+getQuery()+" in "+doc+") [" 
							+ mSimilarity.getClass().getSimpleName() + "], result of:");
					
					IExplanation scoreExplanation = docScorer.explain(doc, 
							new Explanation(freq, "phraseFreq=" + freq));
					result.addDetail(scoreExplanation);
					result.setValue(scoreExplanation.getValue());
					result.setMatch(true);
					
					return result;
				}
			}
      
			return new ComplexExplanation(false, 0.0f, "no matching term");
		}
	}

	@Override
	public Weight createWeight(ISearcher searcher) throws IOException {
		return new PhraseWeight(searcher);
	}

	/** @see Query#getExtractTerms(Set) */
	@Override
	public void extractTerms(Set<ITerm> queryTerms) {
		queryTerms.addAll(mTerms);
	}

	/** Prints a user-readable version of this query. */
	@Override
	public String toString(String f) {
		StringBuilder buffer = new StringBuilder();
		
		if (mField != null && !mField.equals(f)) {
			buffer.append(mField);
			buffer.append(":");
		}

		buffer.append("\"");
		
		String[] pieces = new String[mMaxPosition + 1];
		
		for (int i = 0; i < mTerms.size(); i++) {
			int pos = mPositions.get(i).intValue();
			
			String s = pieces[pos];
			if (s == null) 
				s = (mTerms.get(i)).getText();
			else 
				s = s + "|" + (mTerms.get(i)).getText();
      
			pieces[pos] = s;
		}
		
		for (int i = 0; i < pieces.length; i++) {
			if (i > 0) 
				buffer.append(' ');
      
			String s = pieces[i];
			if (s == null) 
				buffer.append('?');
			else 
				buffer.append(s);
		}
		
		buffer.append("\"");

		if (mSlop != 0) {
			buffer.append("~");
			buffer.append(mSlop);
		}

		buffer.append(StringHelper.toBoostString(getBoost()));

		return buffer.toString();
	}

	/** Returns true if <code>o</code> is equal to this. */
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof PhraseQuery))
			return false;
		
		PhraseQuery other = (PhraseQuery)o;
		
		return (this.getBoost() == other.getBoost())
				&& (this.mSlop == other.mSlop)
				&&  this.mTerms.equals(other.mTerms)
				&& this.mPositions.equals(other.mPositions);
	}

	/** Returns a hash code value for this object.*/
	@Override
	public int hashCode() {
		return Float.floatToIntBits(getBoost())
				^ mSlop
				^ mTerms.hashCode()
				^ mPositions.hashCode();
	}

}
