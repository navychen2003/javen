package org.javenstudio.hornet.query;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.common.indexdb.IWeight;
import org.javenstudio.common.indexdb.index.segment.ReaderUtil;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.hornet.query.source.QueryValueSource;
import org.javenstudio.hornet.search.AdvancedIndexSearcher;
import org.javenstudio.hornet.util.MutableValue;
import org.javenstudio.hornet.util.MutableValueFloat;

public class QueryDocValues extends FloatDocValues {

	private final IAtomicReaderRef mReaderContext;
	private final ValueSourceContext mContext;
	private final IQuery mQuery;
	private final Bits mAcceptDocs;
	private final IWeight mWeight;
	private final float mDefaultValue;
  
	private IScorer mScorer;
	private int mScorerDoc; // the document the scorer is on
	private boolean mNoMatches = false;

	// the last document requested... start off with high value
	// to trigger a scorer reset on first access.
	private int mLastDocRequested = Integer.MAX_VALUE;
  
	public QueryDocValues(QueryValueSource vs, IAtomicReaderRef readerContext, 
			ValueSourceContext fcontext) throws IOException {
		super(vs);

		mReaderContext = readerContext;
		mAcceptDocs = readerContext.getReader().getLiveDocs();
		mDefaultValue= vs.getDefaultValue();
		mQuery = vs.getQuery();
		mContext = fcontext;

		IWeight w = (fcontext == null) ? null : (IWeight)fcontext.get(vs);
		
		if (w == null) {
			ISearcher weightSearcher;
			if (fcontext == null) {
				weightSearcher = new AdvancedIndexSearcher(
						ReaderUtil.getTopLevel(readerContext));
				
			} else {
				weightSearcher = (ISearcher)fcontext.get("searcher");
				if (weightSearcher == null) {
					weightSearcher = new AdvancedIndexSearcher(
							ReaderUtil.getTopLevel(readerContext));
				}
			}
			
			vs.createWeight(fcontext, weightSearcher);
			w = (IWeight)fcontext.get(vs);
		}
		
		mWeight = w;
	}

	public ValueSourceContext getContext() { 
		return mContext;
	}
	
	@Override
	public float floatVal(int doc) {
		try {
			if (doc < mLastDocRequested) {
				if (mNoMatches) 
					return mDefaultValue;
				
				mScorer = mWeight.getScorer(mReaderContext, true, false, mAcceptDocs);
				
				if (mScorer == null) {
					mNoMatches = true;
					return mDefaultValue;
				}
				
				mScorerDoc = -1;
			}
			
			mLastDocRequested = doc;

			if (mScorerDoc < doc) 
				mScorerDoc = mScorer.advance(doc);
      
			if (mScorerDoc > doc) {
				// query doesn't match this document... either because we hit the
				// end, or because the next doc is after this doc.
				return mDefaultValue;
			}

			// a match!
			return mScorer.getScore();
			
		} catch (IOException e) {
			throw new RuntimeException("caught exception in QueryDocVals(" 
					+ mQuery + ") doc=" + doc, e);
		}
	}

	@Override
	public boolean exists(int doc) {
		try {
			if (doc < mLastDocRequested) {
				if (mNoMatches) 
					return false;
				
				mScorer = mWeight.getScorer(mReaderContext, true, false, mAcceptDocs);
				mScorerDoc = -1;
				
				if (mScorer == null) {
					mNoMatches = true;
					return false;
				}
			}
			
			mLastDocRequested = doc;

			if (mScorerDoc < doc) 
				mScorerDoc = mScorer.advance(doc);
			
			if (mScorerDoc > doc) {
				// query doesn't match this document... either because we hit the
				// end, or because the next doc is after this doc.
				return false;
			}

			// a match!
			return true;
			
		} catch (IOException e) {
			throw new RuntimeException("caught exception in QueryDocVals(" 
					+ mQuery + ") doc=" + doc, e);
		}
	}

	@Override
	public Object objectVal(int doc) {
		try {
			return exists(doc) ? mScorer.getScore() : null;
		} catch (IOException e) {
			throw new RuntimeException("caught exception in QueryDocVals(" 
					+ mQuery + ") doc=" + doc, e);
		}
	}

	@Override
	public ValueFiller getValueFiller() {
		//
		// TODO: if we want to support more than one value-filler or a value-filler in conjunction with
		// the FunctionValues, then members like "scorer" should be per ValueFiller instance.
		// Or we can say that the user should just instantiate multiple FunctionValues.
		//
		return new ValueFiller() {
			private final MutableValueFloat mVal = new MutableValueFloat();

			@Override
			public MutableValue getValue() {
				return mVal;
			}

			@Override
			public void fillValue(int doc) {
				try {
					if (mNoMatches) {
						mVal.set(mDefaultValue);
						mVal.setExists(false);
						return;
					}
					
					mScorer = mWeight.getScorer(mReaderContext, true, false, mAcceptDocs);
					mScorerDoc = -1;
					
					if (mScorer == null) {
						mNoMatches = true;
						mVal.set(mDefaultValue);
						mVal.setExists(false);
						
						return;
					}
					
					mLastDocRequested = doc;

					if (mScorerDoc < doc) 
						mScorerDoc = mScorer.advance(doc);
					
					if (mScorerDoc > doc) {
						// query doesn't match this document... either because we hit the
						// end, or because the next doc is after this doc.
						mVal.set(mDefaultValue);
						mVal.setExists(false);
            
						return;
					}

					// a match!
					mVal.set(mScorer.getScore());
					mVal.setExists(true);
					
				} catch (IOException e) {
					throw new RuntimeException("caught exception in QueryDocVals(" 
							+ mQuery + ") doc=" + doc, e);
				}
			}
		};
	}

	@Override
	public String toString(int doc) {
		return "query(" + mQuery + ",def=" + mDefaultValue + ")=" + floatVal(doc);
	}
	
}
