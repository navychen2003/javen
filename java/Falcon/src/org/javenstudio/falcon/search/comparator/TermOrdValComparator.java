package org.javenstudio.falcon.search.comparator;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReader;
import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IIntsReader;
import org.javenstudio.common.indexdb.index.term.DocTermsIndex;
import org.javenstudio.common.indexdb.search.FieldComparator;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.search.cache.FieldCache;

// Copied from TermOrdValComparator and modified since 
// the indexdb version couldn't be extended.
public class TermOrdValComparator extends FieldComparator<BytesRef> {

	public static final int NULL_ORD = Integer.MAX_VALUE;

	protected final BytesRef mNullVal;
	protected final int[] mOrds;
	protected final BytesRef[] mValues;
	protected final int[] mReaderGen;
	protected final String mField;
	
	protected DocTermsIndex mTermsIndex;
	protected PerSegmentComparator mCurrent;

	public TermOrdValComparator(int numHits, String field, int sortPos, 
			boolean reversed, BytesRef nullVal) {
		mOrds = new int[numHits];
		mValues = new BytesRef[numHits];
		mReaderGen = new int[numHits];
		mField = field;
		mNullVal = nullVal;
	}

	@Override
	public int compare(int slot1, int slot2) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setBottom(int slot) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int compareBottom(int doc) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void copy(int slot, int doc) {
		throw new UnsupportedOperationException();
	}

	@Override
	public BytesRef getValue(int slot) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int compareValues(BytesRef first, BytesRef second) {
		if (first == null) {
			if (second == null) 
				return 0;
			else 
				return 1;
			
		} else if (second == null) {
			return -1;
			
		} else {
			return first.compareTo(second);
		}
	}

	@Override
	public FieldComparator<BytesRef> setNextReader(IAtomicReaderRef context) throws IOException {
		return TermOrdValComparator.createComparator(context.getReader(), this);
	}

	@Override
	public int compareDocToValue(int doc, BytesRef docValue) {
		throw new UnsupportedOperationException();
	}
	
	static FieldComparator<BytesRef> createComparator(IAtomicReader reader, 
			TermOrdValComparator parent) throws IOException {
		parent.mTermsIndex = (DocTermsIndex)FieldCache.DEFAULT.getTermsIndex(reader, parent.mField);
		
		final IIntsReader docToOrd = parent.mTermsIndex.getDocToOrd();
		PerSegmentComparator perSegComp = null;
		
		if (docToOrd.hasArray()) {
			final Object arr = docToOrd.getArray();
			if (arr instanceof byte[]) {
				perSegComp = new ByteOrdComparator((byte[]) arr, parent);
			} else if (arr instanceof short[]) {
				perSegComp = new ShortOrdComparator((short[]) arr, parent);
			} else if (arr instanceof int[]) {
				perSegComp = new IntOrdComparator((int[]) arr, parent);
			}
		}

		if (perSegComp == null) 
			perSegComp = new AnyOrdComparator(docToOrd, parent);

		if (perSegComp.mBottomSlot != -1) 
			perSegComp.setBottom(perSegComp.mBottomSlot);
    
		parent.mCurrent = perSegComp;
		
		return perSegComp;
	}
  
}
