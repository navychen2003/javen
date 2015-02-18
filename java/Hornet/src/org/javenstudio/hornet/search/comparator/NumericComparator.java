package org.javenstudio.hornet.search.comparator;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IFieldComparator;
import org.javenstudio.common.indexdb.search.FieldComparator;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.hornet.search.cache.FieldCache;

public abstract class NumericComparator<T extends Number> extends FieldComparator<T> {
	
	protected final T mMissingValue;
	protected final String mField;
	protected Bits mDocsWithField;

	public NumericComparator(String field, T missingValue) {
		mField = field;
		mMissingValue = missingValue;
	}

	@Override
	public IFieldComparator<T> setNextReader(IAtomicReaderRef context) throws IOException {
		if (mMissingValue != null) {
			mDocsWithField = FieldCache.DEFAULT.getDocsWithField(context.getReader(), mField);
			// optimization to remove unneeded checks on the bit interface:
			if (mDocsWithField instanceof Bits.MatchAllBits) 
				mDocsWithField = null;
		} else {
			mDocsWithField = null;
		}
		return this;
    }
	
}
