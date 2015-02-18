package org.javenstudio.hornet.query;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IDocTermsIndex;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.CharsRef;
import org.javenstudio.common.indexdb.util.UnicodeUtil;
import org.javenstudio.hornet.search.cache.FieldCache;
import org.javenstudio.hornet.util.MutableValue;
import org.javenstudio.hornet.util.MutableValueStr;

/**
 * Internal class, subject to change.
 * Serves as base class for FunctionValues based on DocTermsIndex.
 */
public abstract class DocTermsIndexDocValues extends FunctionValues {
	
	protected final MutableValueStr mValue = new MutableValueStr();
	protected final BytesRef mSpare = new BytesRef();
	protected final CharsRef mSpareChars = new CharsRef();
	protected final IDocTermsIndex mTermsIndex;
	protected final ValueSource mSource;

	public DocTermsIndexDocValues(ValueSource vs, IAtomicReaderRef context, 
			String field) throws IOException {
		try {
			mTermsIndex = FieldCache.DEFAULT.getTermsIndex(context.getReader(), field);
		} catch (RuntimeException e) {
			throw new DocTermsIndexException(field, e);
		}
		mSource = vs;
	}

	public IDocTermsIndex getDocTermsIndex() {
		return mTermsIndex;
	}

	protected abstract String toTerm(String readableValue);

	@Override
	public boolean exists(int doc) {
		return mTermsIndex.getOrd(doc) != 0;
	}

	@Override
	public boolean bytesVal(int doc, BytesRef target) {
		int ord = mTermsIndex.getOrd(doc);
		if (ord == 0) {
			target.mLength = 0;
			return false;
		}
		
		mTermsIndex.lookup(ord, target);
		
		return true;
	}

	@Override
	public String stringVal(int doc) {
		int ord = mTermsIndex.getOrd(doc);
		if (ord == 0) 
			return null;
		
		mTermsIndex.lookup(ord, mSpare);
		UnicodeUtil.UTF8toUTF16(mSpare, mSpareChars);
		
		return mSpareChars.toString();
	}

	@Override
	public boolean boolVal(int doc) {
		return exists(doc);
	}

	@Override
	public abstract Object objectVal(int doc); // force subclasses to override

	@Override
	public ValueSourceScorer getRangeScorer(IIndexReader reader, String lowerVal, String upperVal, 
			boolean includeLower, boolean includeUpper) {
		// TODO: are lowerVal and upperVal in indexed form or not?
		lowerVal = lowerVal == null ? null : toTerm(lowerVal);
		upperVal = upperVal == null ? null : toTerm(upperVal);

		final BytesRef spare = new BytesRef();

		int lower = Integer.MIN_VALUE;
		if (lowerVal != null) {
			lower = mTermsIndex.binarySearch(new BytesRef(lowerVal), spare);
			if (lower < 0) 
				lower = -lower-1;
			else if (!includeLower) 
				lower ++;
		}

		int upper = Integer.MAX_VALUE;
		if (upperVal != null) {
			upper = mTermsIndex.binarySearch(new BytesRef(upperVal), spare);
			if (upper < 0) 
				upper = -upper-2;
			else if (!includeUpper) 
				upper --;
		}

		final int ll = lower;
		final int uu = upper;

		return new ValueSourceScorer(reader, this) {
				@Override
				public boolean matchesValue(int doc) {
					int ord = mTermsIndex.getOrd(doc);
					return ord >= ll && ord <= uu;
				}
			};
	}

	@Override
	public String toString(int doc) {
		return mSource.getDescription() + '=' + stringVal(doc);
	}

	@Override
	public ValueFiller getValueFiller() {
		return new ValueFiller() {
				private final MutableValueStr mVal = new MutableValueStr();
	
				@Override
				public MutableValue getValue() {
					return mVal;
				}
	
				@Override
				public void fillValue(int doc) {
					int ord = mTermsIndex.getOrd(doc);
					mVal.setExists(ord != 0);
					mVal.set(mTermsIndex.lookup(ord, mVal.get()));
				}
			};
	}

}
