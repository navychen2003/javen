package org.javenstudio.hornet.search.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.javenstudio.common.indexdb.IAtomicReader;
import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IDocsAndPositionsEnum;
import org.javenstudio.common.indexdb.IExplanation;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.IIndexReaderRef;
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
import org.javenstudio.common.indexdb.search.ComplexExplanation;
import org.javenstudio.common.indexdb.search.Explanation;
import org.javenstudio.common.indexdb.search.Query;
import org.javenstudio.common.indexdb.search.TermStatistics;
import org.javenstudio.common.indexdb.search.Weight;
import org.javenstudio.common.indexdb.util.ArrayUtil;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.indexdb.util.StringHelper;
import org.javenstudio.hornet.index.term.TermContext;

/**
 * MultiPhraseQuery is a generalized version of PhraseQuery, with an added
 * method {@link #add(Term[])}.
 * To use this class, to search for the phrase "Microsoft app*" first use
 * add(Term) on the term "Microsoft", then find all terms that have "app" as
 * prefix using IndexReader.terms(Term), and use MultiPhraseQuery.add(Term[]
 * terms) to add them to the query.
 *
 */
public class MultiPhraseQuery extends Query {
  
	private ArrayList<ITerm[]> mTermArrays = new ArrayList<ITerm[]>();
	private ArrayList<Integer> mPositions = new ArrayList<Integer>();

	private String mField;
	private int mSlop = 0;

	/** 
	 * Sets the phrase slop for this query.
	 * @see PhraseQuery#setSlop(int)
	 */
	public void setSlop(int s) { mSlop = s; }

	/** 
	 * Sets the phrase slop for this query.
	 * @see PhraseQuery#getSlop()
	 */
	public int getSlop() { return mSlop; }

	/** 
	 * Add a single term at the next position in the phrase.
	 * @see PhraseQuery#add(Term)
	 */
	public void add(ITerm term) { add(new ITerm[]{term}); }

	/** 
	 * Add multiple terms at the next position in the phrase.  Any of the terms
	 * may match.
	 *
	 * @see PhraseQuery#add(Term)
	 */
	public void add(ITerm[] terms) {
		int position = 0;
		if (mPositions.size() > 0)
			position = mPositions.get(mPositions.size()-1).intValue() + 1;

		add(terms, position);
	}

	/**
	 * Allows to specify the relative position of terms within the phrase.
	 * 
	 * @see PhraseQuery#add(Term, int)
	 */
	public void add(ITerm[] terms, int position) {
		if (mTermArrays.size() == 0)
			mField = terms[0].getField();

		for (int i = 0; i < terms.length; i++) {
			if (!terms[i].getField().equals(mField)) {
				throw new IllegalArgumentException(
						"All phrase terms must be in the same field (" + mField + "): " + terms[i]);
			}
		}

		mTermArrays.add(terms);
		mPositions.add(Integer.valueOf(position));
	}

	/**
	 * Returns a List of the terms in the multiphrase.
	 * Do not modify the List or its contents.
	 */
	public List<ITerm[]> getTermArrays() {
		return Collections.unmodifiableList(mTermArrays);
	}

	/**
	 * Returns the relative positions of terms in this phrase.
	 */
	public int[] getPositions() {
		int[] result = new int[mPositions.size()];
		for (int i = 0; i < mPositions.size(); i++) {
			result[i] = mPositions.get(i).intValue(); 
		}
		return result;
	}

	@Override
	public void extractTerms(Set<ITerm> terms) {
		for (final ITerm[] arr : mTermArrays) {
			for (final ITerm term: arr) {
				terms.add(term);
			}
		}
	}

	private class MultiPhraseWeight extends Weight {
		
		private final Map<ITerm,ITermContext> mTermContexts = 
				new HashMap<ITerm,ITermContext>();
		
		private final ISimilarity mSimilarity;
		private final ISimilarityWeight mStats;
    
		public MultiPhraseWeight(ISearcher searcher) throws IOException {
			mSimilarity = searcher.getSimilarity();
			final IIndexReaderRef context = searcher.getTopReaderContext();
      
			// compute idf
			ArrayList<ITermStatistics> allTermStats = new ArrayList<ITermStatistics>();
			
			for (final ITerm[] terms: mTermArrays) {
				for (ITerm term: terms) {
					ITermContext termContext = mTermContexts.get(term);
					if (termContext == null) {
						termContext = TermContext.build(context, term, true);
						mTermContexts.put(term, termContext);
					}
					allTermStats.add(searcher.getTermStatistics(term, termContext));
				}
			}
			
			mStats = mSimilarity.computeWeight(getBoost(),
					searcher.getCollectionStatistics(mField), 
					allTermStats.toArray(new TermStatistics[allTermStats.size()]));
		}

		@Override
		public Query getQuery() { return MultiPhraseQuery.this; }

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
			assert !mTermArrays.isEmpty();
			
			final IAtomicReader reader = context.getReader();
			final Bits liveDocs = acceptDocs;
      
			PostingsAndFreq[] postingsFreqs = new PostingsAndFreq[mTermArrays.size()];

			final ITerms fieldTerms = reader.getTerms(mField);
			if (fieldTerms == null) 
				return null;

			// Reuse single TermsEnum below:
			final ITermsEnum termsEnum = fieldTerms.iterator(null);

			for (int pos=0; pos < postingsFreqs.length; pos++) {
				ITerm[] terms = mTermArrays.get(pos);

				final IDocsAndPositionsEnum postingsEnum;
				int docFreq;

				if (terms.length > 1) {
					postingsEnum = new UnionDocsAndPositionsEnum(liveDocs, 
							context, terms, mTermContexts, termsEnum);

					// coarse -- this overcounts since a given doc can
					// have more than one term:
					docFreq = 0;
					
					for (int termIdx=0; termIdx < terms.length; termIdx++) {
						final ITerm term = terms[termIdx];
						
						ITermState termState = mTermContexts.get(term).get(context.getOrd());
						if (termState == null) {
							// Term not in reader
							continue;
						}
						
						termsEnum.seekExact(term.getBytes(), termState);
						docFreq += termsEnum.getDocFreq();
					}

					if (docFreq == 0) {
						// None of the terms are in this reader
						return null;
					}
					
				} else {
					final ITerm term = terms[0];
					
					ITermState termState = mTermContexts.get(term).get(context.getOrd());
					if (termState == null) {
						// Term not in reader
						return null;
					}
					
					termsEnum.seekExact(term.getBytes(), termState);
					postingsEnum = termsEnum.getDocsAndPositions(liveDocs, null, 0);

					if (postingsEnum == null) {
						// term does exist, but has no positions
						assert termsEnum.getDocs(liveDocs, null, 0) != null: 
							"termstate found but no term exists in reader";
						
						throw new IllegalStateException("field \"" + term.getField() 
								+ "\" was indexed without position data; cannot run PhraseQuery (term=" 
								+ term.getText() + ")");
					}

					docFreq = termsEnum.getDocFreq();
				}

				postingsFreqs[pos] = new PostingsAndFreq(postingsEnum, docFreq, 
						mPositions.get(pos).intValue(), terms);
			}

			// sort by increasing docFreq order
			if (mSlop == 0) 
				ArrayUtil.mergeSort(postingsFreqs);

			if (mSlop == 0) {
				ExactPhraseScorer s = new ExactPhraseScorer(this, postingsFreqs, 
						mSimilarity.getExactSimilarityScorer(mStats, context));
				
				if (s.hasNoDocs()) 
					return null;
				else 
					return s;
				
			} else {
				return new SloppyPhraseScorer(this, postingsFreqs, 
						mSlop, mSimilarity.getSloppySimilarityScorer(mStats, context));
			}
		}

		@Override
		public IExplanation explain(IAtomicReaderRef context, int doc) throws IOException {
			IScorer scorer = getScorer(context, true, false, context.getReader().getLiveDocs());
			if (scorer != null) {
				int newDoc = scorer.advance(doc);
				
				if (newDoc == doc) {
					float freq = scorer.getFreq();
					
					ISloppySimilarityScorer docScorer = mSimilarity.getSloppySimilarityScorer(mStats, context);
					
					ComplexExplanation result = new ComplexExplanation();
					result.setDescription("weight(" + getQuery() + " in " + doc + ") [" 
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
	public Query rewrite(IIndexReader reader) {
		if (mTermArrays.isEmpty()) {
			BooleanQuery bq = new BooleanQuery();
			bq.setBoost(getBoost());
			return bq;
			
		} else if (mTermArrays.size() == 1) { // optimize one-term case
			ITerm[] terms = mTermArrays.get(0);
			
			BooleanQuery boq = new BooleanQuery(true);
			for (int i=0; i<terms.length; i++) {
				boq.add(new TermQuery(terms[i]), BooleanClause.Occur.SHOULD);
			}
			
			boq.setBoost(getBoost());
			return boq;
			
		} else {
			return this;
		}
	}

	@Override
	public Weight createWeight(ISearcher searcher) throws IOException {
		return new MultiPhraseWeight(searcher);
	}

	/** Prints a user-readable version of this query. */
	@Override
	public final String toString(String f) {
		StringBuilder buffer = new StringBuilder();
		
		if (mField == null || !mField.equals(f)) {
			buffer.append(mField);
			buffer.append(":");
		}

		buffer.append("\"");
		
		Iterator<ITerm[]> iter = mTermArrays.iterator();
		int k = 0;
		int lastPos = -1;
		boolean first = true;
		
		while (iter.hasNext()) {
			ITerm[] terms = iter.next();
			int position = mPositions.get(k);
			if (first) {
				first = false;
				
			} else {
				buffer.append(" ");
				for (int j=1; j < (position-lastPos); j++) {
					buffer.append("? ");
				}
			}
			
			if (terms.length > 1) {
				buffer.append("(");
				for (int j = 0; j < terms.length; j++) {
					buffer.append(terms[j].getText());
					if (j < terms.length-1)
						buffer.append(" ");
				}
				buffer.append(")");
				
			} else {
				buffer.append(terms[0].getText());
			}
			
			lastPos = position;
			++ k;
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
		if (!(o instanceof MultiPhraseQuery)) return false;
		
		MultiPhraseQuery other = (MultiPhraseQuery)o;
		
		return this.getBoost() == other.getBoost()
				&& this.mSlop == other.mSlop
				&& termArraysEquals(this.mTermArrays, other.mTermArrays)
				&& this.mPositions.equals(other.mPositions);
	}

	/** Returns a hash code value for this object.*/
	@Override
	public int hashCode() {
		return Float.floatToIntBits(getBoost())
				^ mSlop
				^ termArraysHashCode()
				^ mPositions.hashCode()
				^ 0x4AC65113;
	}
  
	// Breakout calculation of the termArrays hashcode
	private int termArraysHashCode() {
		int hashCode = 1;
		for (final ITerm[] termArray: mTermArrays) {
			hashCode = 31 * hashCode + (termArray == null ? 0 : Arrays.hashCode(termArray));
		}
		return hashCode;
	}

	// Breakout calculation of the termArrays equals
	private boolean termArraysEquals(List<ITerm[]> termArrays1, List<ITerm[]> termArrays2) {
		if (termArrays1.size() != termArrays2.size()) 
			return false;
		
		ListIterator<ITerm[]> iterator1 = termArrays1.listIterator();
		ListIterator<ITerm[]> iterator2 = termArrays2.listIterator();
		
		while (iterator1.hasNext()) {
			ITerm[] termArray1 = iterator1.next();
			ITerm[] termArray2 = iterator2.next();
			
			if (!(termArray1 == null ? termArray2 == null : Arrays.equals(termArray1, termArray2))) 
				return false;
		}
		
		return true;
	}
	
}
