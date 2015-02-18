package org.javenstudio.hornet.search.query;

import java.io.IOException;
import java.util.Comparator;

import org.javenstudio.common.indexdb.IDocsAndPositionsEnum;
import org.javenstudio.common.indexdb.IDocsEnum;
import org.javenstudio.common.indexdb.ITermState;
import org.javenstudio.common.indexdb.index.term.Term;
import org.javenstudio.common.indexdb.index.term.Terms;
import org.javenstudio.common.indexdb.index.term.TermsEnum;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.UnicodeUtil;

/** 
 * Subclass of TermsEnum for enumerating all terms that are similar
 * to the specified filter term.
 *
 * <p>Term enumerations are always ordered by
 * {@link #getComparator}.  Each term in the enumeration is
 * greater than all that precede it.</p>
 */
public class FuzzyTermsEnum extends TermsEnum {
	
	// TODO: chicken-and-egg
	protected final Comparator<BytesRef> mTermComparator = 
			BytesRef.getUTF8SortedAsUnicodeComparator();
	
	protected TermsEnum mActualEnum;
	//private BoostAttribute actualBoostAtt;
  
	//private final BoostAttribute boostAtt =
	//  attributes().addAttribute(BoostAttribute.class);
  
	//private final MaxNonCompetitiveBoostAttribute maxBoostAtt;
	//private final LevenshteinAutomataAttribute dfaAtt;
  
	protected float mBottom;
	protected BytesRef mBottomTerm;
  
	protected final float mMinSimilarity;
	protected final float mScaleFactor;
  
	protected final int mTermLength;
  
	protected int mMaxEdits;
	protected final boolean mRaw;

	protected final Terms mTerms;
	protected final Term mTerm;
	protected final int mTermText[];
	protected final int mRealPrefixLength;
  
	protected final boolean mTranspositions;
	
	private BytesRef mQueuedBottom = null;
  
	/**
	 * Constructor for enumeration of all terms from specified <code>reader</code> which share a prefix of
	 * length <code>prefixLength</code> with <code>term</code> and which have a fuzzy similarity &gt;
	 * <code>minSimilarity</code>.
	 * <p>
	 * After calling the constructor the enumeration is already pointing to the first 
	 * valid term if such a term exists. 
	 * 
	 * @param terms Delivers terms.
	 * @param atts {@link AttributeSource} created by the rewrite method of {@link MultiTermQuery}
	 * thats contains information about competitive boosts during rewrite. It is also used
	 * to cache DFAs between segment transitions.
	 * @param term Pattern term.
	 * @param minSimilarity Minimum required similarity for terms from the reader. Pass an integer value
	 *        representing edit distance. Passing a fraction is deprecated.
	 * @param prefixLength Length of required common prefix. Default value is 0.
	 * @throws IOException if there is a low-level IO error
	 */
	public FuzzyTermsEnum(Terms terms, Term term, final float minSimilarity, 
			final int prefixLength, boolean transpositions) throws IOException {
		if (minSimilarity >= 1.0f && minSimilarity != (int)minSimilarity)
			throw new IllegalArgumentException("fractional edit distances are not allowed");
		
		if (minSimilarity < 0.0f)
			throw new IllegalArgumentException("minimumSimilarity cannot be less than 0");
		
		if (prefixLength < 0)
			throw new IllegalArgumentException("prefixLength cannot be less than 0");
		
		mTerms = terms;
		mTerm = term;

		// convert the string into a utf32 int[] representation for fast comparisons
		final String utf16 = term.getText();
		mTermText = new int[utf16.codePointCount(0, utf16.length())];
		
		for (int cp, i = 0, j = 0; i < utf16.length(); i += Character.charCount(cp)) {
			mTermText[j++] = cp = utf16.codePointAt(i);
		}
		
    	mTermLength = mTermText.length;
    	//this.dfaAtt = atts.addAttribute(LevenshteinAutomataAttribute.class);

    	//The prefix could be longer than the word.
    	//It's kind of silly though.  It means we must match the entire word.
    	mRealPrefixLength = prefixLength > mTermLength ? mTermLength : prefixLength;
    	
    	// if minSimilarity >= 1, we treat it as number of edits
    	if (minSimilarity >= 1f) {
    		mMinSimilarity = 0; // just driven by number of edits
    		mMaxEdits = (int) minSimilarity;
    		mRaw = true;
    		
    	} else {
    		mMinSimilarity = minSimilarity;
    		// calculate the maximum k edits for this similarity
    		mMaxEdits = initialMaxDistance(mMinSimilarity, mTermLength);
    		mRaw = false;
    	}
    	
    	if (transpositions && mMaxEdits > FuzzyQuery.MAXIMUM_SUPPORTED_DISTANCE) {
    		throw new UnsupportedOperationException("with transpositions enabled, distances > " 
    				+ FuzzyQuery.MAXIMUM_SUPPORTED_DISTANCE + " are not supported ");
    	}
    	
    	mTranspositions = transpositions;
    	mScaleFactor = 1.0f / (1.0f - mMinSimilarity);

    	//this.maxBoostAtt = atts.addAttribute(MaxNonCompetitiveBoostAttribute.class);
    	//bottom = maxBoostAtt.getMaxNonCompetitiveBoost();
    	//bottomTerm = maxBoostAtt.getCompetitiveTerm();
    	bottomChanged(null, true);
	}
  
	/**
	 * return an automata-based enum for matching up to editDistance from
	 * lastTerm, if possible
	 */
	protected TermsEnum getAutomatonEnum(int editDistance, BytesRef lastTerm)
			throws IOException {
		//final List<CompiledAutomaton> runAutomata = initAutomata(editDistance);
		//if (editDistance < runAutomata.size()) {
		//  final CompiledAutomaton compiled = runAutomata.get(editDistance);
		//  return new AutomatonFuzzyTermsEnum(terms.intersect(compiled, lastTerm == null ? null : compiled.floor(lastTerm, new BytesRef())),
		//                                     runAutomata.subList(0, editDistance + 1).toArray(new CompiledAutomaton[editDistance + 1]));
		//} else {
			return null;
		//}
	}

  /** initialize levenshtein DFAs up to maxDistance, if possible *//*
  private List<CompiledAutomaton> initAutomata(int maxDistance) {
    final List<CompiledAutomaton> runAutomata = dfaAtt.automata();
    if (runAutomata.size() <= maxDistance && 
        maxDistance <= FuzzyQuery.MAXIMUM_SUPPORTED_DISTANCE) {
      LevenshteinAutomata builder = 
        new LevenshteinAutomata(UnicodeUtil.newString(termText, realPrefixLength, termText.length - realPrefixLength), transpositions);

      for (int i = runAutomata.size(); i <= maxDistance; i++) {
        Automaton a = builder.toAutomaton(i);
        // constant prefix
        if (realPrefixLength > 0) {
          Automaton prefix = BasicAutomata.makeString(
            UnicodeUtil.newString(termText, 0, realPrefixLength));
          a = BasicOperations.concatenate(prefix, a);
        }
        runAutomata.add(new CompiledAutomaton(a, true, false));
      }
    }
    return runAutomata;
  }
*/
	
	/** swap in a new actual enum to proxy to */
	protected void setEnum(TermsEnum actualEnum) {
		mActualEnum = actualEnum;
		//this.actualBoostAtt = actualEnum.attributes().addAttribute(BoostAttribute.class);
	}
  
	/**
	 * fired when the max non-competitive boost has changed. this is the hook to
	 * swap in a smarter actualEnum
	 */
	private void bottomChanged(BytesRef lastTerm, boolean init) throws IOException {
		int oldMaxEdits = mMaxEdits;
    
		// true if the last term encountered is lexicographically equal or after the bottom term in the PQ
		boolean termAfter = mBottomTerm == null || 
				(lastTerm != null && mTermComparator.compare(lastTerm, mBottomTerm) >= 0);

		// as long as the max non-competitive boost is >= the max boost
		// for some edit distance, keep dropping the max edit distance.
		while (mMaxEdits > 0 && (termAfter ? mBottom >= calculateMaxBoost(mMaxEdits) : 
			mBottom > calculateMaxBoost(mMaxEdits))) {
			mMaxEdits --;
		}
    
		if (oldMaxEdits != mMaxEdits || init) { 
			// the maximum n has changed
			maxEditDistanceChanged(lastTerm, mMaxEdits, init);
		}
	}
  
	protected void maxEditDistanceChanged(BytesRef lastTerm, int maxEdits, boolean init)
			throws IOException {
		TermsEnum newEnum = getAutomatonEnum(maxEdits, lastTerm);
		
		// instead of assert, we do a hard check in case someone uses our enum directly
		// assert newEnum != null;
		if (newEnum == null) {
			assert maxEdits > FuzzyQuery.MAXIMUM_SUPPORTED_DISTANCE;
			throw new IllegalArgumentException("maxEdits cannot be > LevenshteinAutomata.MAXIMUM_SUPPORTED_DISTANCE");
		}
		
		setEnum(newEnum);
	}

	// for some raw min similarity and input term length, the maximum # of edits
	private int initialMaxDistance(float minimumSimilarity, int termLen) {
		return (int) ((1D-minimumSimilarity) * termLen);
	}
  
	// for some number of edits, the maximum possible scaled boost
	private float calculateMaxBoost(int nEdits) {
		final float similarity = 1.0f - ((float) nEdits / (float) (mTermLength));
		return (similarity - mMinSimilarity) * mScaleFactor;
	}

	@Override
	public BytesRef next() throws IOException {
		if (mQueuedBottom != null) {
			bottomChanged(mQueuedBottom, false);
			mQueuedBottom = null;
		}
    
		BytesRef term = mActualEnum.next();
		//boostAtt.setBoost(actualBoostAtt.getBoost());
    
		final float bottom = 0; //maxBoostAtt.getMaxNonCompetitiveBoost();
		final BytesRef bottomTerm = null; //maxBoostAtt.getCompetitiveTerm();
		
		if (term != null && (bottom != mBottom || bottomTerm != mBottomTerm)) {
			mBottom = bottom;
			mBottomTerm = bottomTerm;
			
			// clone the term before potentially doing something with it
			// this is a rare but wonderful occurrence anyway
			mQueuedBottom = BytesRef.deepCopyOf(term);
		}
    
		return term;
	}
  
	// proxy all other enum calls to the actual enum
	@Override
	public int getDocFreq() throws IOException {
		return mActualEnum.getDocFreq();
	}

	@Override
	public long getTotalTermFreq() throws IOException {
		return mActualEnum.getTotalTermFreq();
	}
  
	@Override
	public IDocsEnum getDocs(Bits liveDocs, IDocsEnum reuse, int flags) throws IOException {
		return mActualEnum.getDocs(liveDocs, reuse, flags);
	}
  
	@Override
	public IDocsAndPositionsEnum getDocsAndPositions(Bits liveDocs,
			IDocsAndPositionsEnum reuse, int flags) throws IOException {
		return mActualEnum.getDocsAndPositions(liveDocs, reuse, flags);
	}
  
	@Override
	public void seekExact(BytesRef term, ITermState state) throws IOException {
		mActualEnum.seekExact(term, state);
	}
  
	@Override
	public ITermState getTermState() throws IOException {
		return mActualEnum.getTermState();
	}
  
	@Override
	public Comparator<BytesRef> getComparator() {
		return mActualEnum.getComparator();
	}
  
	@Override
	public long getOrd() throws IOException {
		return mActualEnum.getOrd();
	}
  
	@Override
	public boolean seekExact(BytesRef text, boolean useCache) throws IOException {
		return mActualEnum.seekExact(text, useCache);
	}

	@Override
	public SeekStatus seekCeil(BytesRef text, boolean useCache) throws IOException {
		return mActualEnum.seekCeil(text, useCache);
	}
  
	@Override
	public void seekExact(long ord) throws IOException {
		mActualEnum.seekExact(ord);
	}
  
	@Override
	public BytesRef getTerm() throws IOException {
		return mActualEnum.getTerm();
	}

	/**
	 * Implement fuzzy enumeration with Terms.intersect.
	 * <p>
	 * This is the fastest method as opposed to LinearFuzzyTermsEnum:
	 * as enumeration is logarithmic to the number of terms (instead of linear)
	 * and comparison is linear to length of the term (rather than quadratic)
	 */
	@SuppressWarnings("unused")
	private class AutomatonFuzzyTermsEnum extends FilteredTermsEnum {
		//private final ByteRunAutomaton matchers[];
    
		private final BytesRef mTermRef = null;
    
		//private final BoostAttribute boostAtt =
		//  attributes().addAttribute(BoostAttribute.class);
    
		public AutomatonFuzzyTermsEnum(TermsEnum tenum) {
			super(tenum, false);
			
			//this.matchers = new ByteRunAutomaton[compiled.length];
			//for (int i = 0; i < compiled.length; i++)
			//  this.matchers[i] = compiled[i].runAutomaton;
			//termRef = new BytesRef(term.text());
		}

		/** finds the smallest Lev(n) DFA that accepts the term. */
		@Override
		protected AcceptStatus accept(BytesRef term) { 
			int ed = 0; //matchers.length - 1;
      
			// we are wrapping either an intersect() TermsEnum or an AutomatonTermsENum,
			// so we know the outer DFA always matches.
			// now compute exact edit distance
			while (ed > 0) {
				if (matches(term, ed - 1)) 
					ed--;
				else 
					break;
			}
      
			// scale to a boost and return (if similarity > minSimilarity)
			if (ed == 0) { // exact match
				//boostAtt.setBoost(1.0F);
				return AcceptStatus.YES;
				
			} else {
				final int codePointCount = UnicodeUtil.codePointCount(term);
				final float similarity = 1.0f - ((float) ed / (float) 
						(Math.min(codePointCount, mTermLength)));
				
				if (similarity > mMinSimilarity) {
					//boostAtt.setBoost((similarity - minSimilarity) * scale_factor);
					return AcceptStatus.YES;
					
				} else {
					return AcceptStatus.NO;
				}
			}
		}
    
		/** returns true if term is within k edits of the query term */
		final boolean matches(BytesRef term, int k) {
			return false; //k == 0 ? term.equals(termRef) : matchers[k].run(term.bytes, term.offset, term.length);
		}
	}

	public float getMinSimilarity() { return mMinSimilarity; }
	public float getScaleFactor() { return mScaleFactor; }
  
	/**
	 * reuses compiled automata across different segments,
	 * because they are independent of the index
	 */
	//public static interface LevenshteinAutomataAttribute extends Attribute {
	//  public List<CompiledAutomaton> automata();
	//}
    
  /** 
   * Stores compiled automata as a list (indexed by edit distance)
   *//*
  public static final class LevenshteinAutomataAttributeImpl extends AttributeImpl implements LevenshteinAutomataAttribute {
    private final List<CompiledAutomaton> automata = new ArrayList<CompiledAutomaton>();
      
    public List<CompiledAutomaton> automata() {
      return automata;
    }

    @Override
    public void clear() {
      automata.clear();
    }

    @Override
    public int hashCode() {
      return automata.hashCode();
    }

    @Override
    public boolean equals(Object other) {
      if (this == other)
        return true;
      if (!(other instanceof LevenshteinAutomataAttributeImpl))
        return false;
      return automata.equals(((LevenshteinAutomataAttributeImpl) other).automata);
    }

    @Override
    public void copyTo(AttributeImpl target) {
      final List<CompiledAutomaton> targetAutomata =
        ((LevenshteinAutomataAttribute) target).automata();
      targetAutomata.clear();
      targetAutomata.addAll(automata);
    }
  }*/
	
}
