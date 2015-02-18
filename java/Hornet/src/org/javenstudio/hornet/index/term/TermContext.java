package org.javenstudio.hornet.index.term;

import java.io.IOException;
import java.util.Arrays;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IFields;
import org.javenstudio.common.indexdb.IIndexReaderRef;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.ITermContext;
import org.javenstudio.common.indexdb.ITermState;
import org.javenstudio.common.indexdb.index.term.Term;
import org.javenstudio.common.indexdb.index.term.TermState;
import org.javenstudio.common.indexdb.index.term.Terms;
import org.javenstudio.common.indexdb.index.term.TermsEnum;
import org.javenstudio.common.indexdb.util.BytesRef;

/**
 * Maintains a {@link IndexReader} {@link TermState} view over
 * {@link IndexReader} instances containing a single term. The
 * {@link TermContext} doesn't track if the given {@link TermState}
 * objects are valid, neither if the {@link TermState} instances refer to the
 * same terms in the associated readers.
 */
public final class TermContext implements ITermContext {
	
	private final IIndexReaderRef mTopReaderContext; // for asserting!
	private final TermState[] mStates;
	private int mDocFreq;
	private long mTotalTermFreq;
  
	/**
	 * Creates an empty {@link TermContext} from a {@link IIndexReaderRef}
	 */
	public TermContext(IIndexReaderRef context) {
		assert context != null && context.isTopLevel();
		
		mTopReaderContext = context;
		mDocFreq = 0;
		
		final int len;
		if (context.getLeaves() == null) 
			len = 1;
		else 
			len = context.getLeaves().size();
		
		mStates = new TermState[len];
	}
  
	/**
	 * Creates a {@link TermContext} with an initial {@link TermState},
	 * {@link IndexReader} pair.
	 */
	public TermContext(IIndexReaderRef context, TermState state, 
			int ord, int docFreq, long totalTermFreq) {
		this(context);
		register(state, ord, docFreq, totalTermFreq);
	}

	/**
	 * Creates a {@link TermContext} from a top-level {@link IIndexReaderRef} and the
	 * given {@link Term}. This method will lookup the given term in all context's leaf readers 
	 * and register each of the readers containing the term in the returned {@link TermContext}
	 * using the leaf reader's ordinal.
	 * <p>
	 * Note: the given context must be a top-level context.
	 */
	public static TermContext build(IIndexReaderRef context, ITerm term, boolean cache)
			throws IOException {
		assert context != null && context.isTopLevel();
		
		final String field = term.getField();
		final BytesRef bytes = term.getBytes();
		final TermContext perReaderTermState = new TermContext(context);
		
		for (final IAtomicReaderRef ctx : context.getLeaves()) {
			final IFields fields = (IFields)ctx.getReader().getFields();
			if (fields != null) {
				final Terms terms = (Terms)fields.getTerms(field);
				if (terms != null) {
					final TermsEnum termsEnum = (TermsEnum)terms.iterator(null);
					if (termsEnum.seekExact(bytes, cache)) { 
						final TermState termState = (TermState)termsEnum.getTermState();
						perReaderTermState.register(termState, ctx.getOrd(), 
								termsEnum.getDocFreq(), termsEnum.getTotalTermFreq());
					}
				}
			}
		}
		
		return perReaderTermState;
	}

	@Override
	public final IIndexReaderRef getTopReader() { 
		return mTopReaderContext; 
	}
	
	/**
	 * Clears the {@link TermContext} internal state and removes all
	 * registered {@link TermState}s
	 */
	@Override
	public void clear() {
		mDocFreq = 0;
		Arrays.fill(mStates, null);
	}

	/**
	 * Registers and associates a {@link TermState} with an leaf ordinal. The leaf ordinal
	 * should be derived from a {@link IIndexReaderRef}'s leaf ord.
	 */
	public void register(TermState state, final int ord, final int docFreq, final long totalTermFreq) {
		assert state != null : "state must not be null";
		assert ord >= 0 && ord < mStates.length;
		assert mStates[ord] == null : "state for ord: " + ord
				+ " already registered";
		
		mDocFreq += docFreq;
		mStates[ord] = state;
		
		if (mTotalTermFreq >= 0 && totalTermFreq >= 0)
			mTotalTermFreq += totalTermFreq;
		else
			mTotalTermFreq = -1;
	}

	/**
	 * Returns the {@link TermState} for an leaf ordinal or <code>null</code> if no
	 * {@link TermState} for the ordinal was registered.
	 * 
	 * @param ord
	 *          the readers leaf ordinal to get the {@link TermState} for.
	 * @return the {@link TermState} for the given readers ord or <code>null</code> if no
	 *         {@link TermState} for the reader was registered
	 */
	@Override
	public ITermState get(int ord) {
		assert ord >= 0 && ord < mStates.length;
		return mStates[ord];
	}

	/**
	 *  Returns the accumulated document frequency of all {@link TermState}
	 *         instances passed to {@link #register(TermState, int, int, long)}.
	 * @return the accumulated document frequency of all {@link TermState}
	 *         instances passed to {@link #register(TermState, int, int, long)}.
	 */
	@Override
	public int getDocFreq() {
		return mDocFreq;
	}
  
	/**
	 *  Returns the accumulated term frequency of all {@link TermState}
	 *         instances passed to {@link #register(TermState, int, int, long)}.
	 * @return the accumulated term frequency of all {@link TermState}
	 *         instances passed to {@link #register(TermState, int, int, long)}.
	 */
	@Override
	public long getTotalTermFreq() {
		return mTotalTermFreq;
	}
  
	/** expert: only available for queries that want to lie about docfreq */
	@Override
	public void setDocFreq(int docFreq) {
		mDocFreq = docFreq;
	}
	
}