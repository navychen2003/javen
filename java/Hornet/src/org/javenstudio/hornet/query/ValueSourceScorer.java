package org.javenstudio.hornet.query;

import java.io.IOException;

import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.search.Scorer;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.hornet.index.field.MultiFields;

/**
 * {@link Scorer} which returns the result of {@link FunctionValues#floatVal(int)} as
 * the score for a document.
 */
public class ValueSourceScorer extends Scorer {

	protected final IIndexReader mReader;
	protected final FunctionValues mValues;
	protected final Bits mLiveDocs;
	protected final int mMaxDoc;
	protected boolean mCheckDeletes;
	protected int mDoc = -1;
  
	protected ValueSourceScorer(IIndexReader reader, FunctionValues values) {
		super(null);
		mReader = reader;
		mMaxDoc = reader.getMaxDoc();
		mValues = values;
		setCheckDeletes(true);
		mLiveDocs = MultiFields.getLiveDocs(reader);
	}

	public IIndexReader getReader() {
		return mReader;
	}

	public void setCheckDeletes(boolean checkDeletes) {
		mCheckDeletes = checkDeletes && mReader.hasDeletions();
	}

	public boolean matches(int doc) {
		return (!mCheckDeletes || mLiveDocs.get(doc)) && matchesValue(doc);
	}

	public boolean matchesValue(int doc) {
		return true;
	}

	@Override
	public int getDocID() {
		return mDoc;
	}

	@Override
	public int nextDoc() throws IOException {
		for (; ;) {
			mDoc ++;
			
			if (mDoc >= mMaxDoc) 
				return mDoc = NO_MORE_DOCS;
			
			if (matches(mDoc)) 
				return mDoc;
		}
	}

	@Override
	public int advance(int target) throws IOException {
		// also works fine when target==NO_MORE_DOCS
		mDoc = target - 1;
		return nextDoc();
	}

	@Override
	public float getScore() throws IOException {
		return mValues.floatVal(mDoc);
	}

	@Override
	public float getFreq() throws IOException {
		return 1;
	}
	
}
