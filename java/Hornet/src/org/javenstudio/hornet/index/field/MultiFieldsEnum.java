package org.javenstudio.hornet.index.field;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import org.javenstudio.common.indexdb.IFieldsEnum;
import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.common.indexdb.index.field.Fields;
import org.javenstudio.common.indexdb.index.field.FieldsEnum;
import org.javenstudio.common.indexdb.index.segment.ReaderSlice;
import org.javenstudio.common.indexdb.util.PriorityQueue;

/**
 * Exposes flex API, merged from flex API of sub-segments.
 * This does a merge sort, by field name, of the
 * sub-readers.
 *
 */
public final  class MultiFieldsEnum extends FieldsEnum {
	
	private final FieldMergeQueue mQueue;
	private final Fields mFields;

	// Holds sub-readers containing field we are currently
	// on, popped from queue.
	private final FieldsEnumWithSlice[] mTop;

	private int mNumTop;
	private String mCurrentField;

	/** 
	 * The subs array must be newly initialized FieldsEnum
	 *  (ie, {@link FieldsEnum#next} has not been called. 
	 */
	public MultiFieldsEnum(MultiFields fields, IFieldsEnum[] subs, 
			ReaderSlice[] subSlices) {
		mFields = fields;
		mQueue = new FieldMergeQueue(subs.length);
		mTop = new FieldsEnumWithSlice[subs.length];
		
		List<FieldsEnumWithSlice> enumWithSlices = new ArrayList<FieldsEnumWithSlice>();

		// Init q
		for (int i=0; i < subs.length; i++) {
			assert subs[i] != null;
			final String field = subs[i].next();
			
			if (field != null) {
				// this FieldsEnum has at least one field
				final FieldsEnumWithSlice sub = new FieldsEnumWithSlice(subs[i], subSlices[i], i);
				enumWithSlices.add(sub);
				sub.mCurrent = field;
				mQueue.add(sub);
			}
		}
	}

	@Override
	public String next() {
		// restore queue
		for (int i=0; i < mNumTop; i++) {
			mTop[i].mCurrent = mTop[i].mFields.next();
			if (mTop[i].mCurrent != null) {
				mQueue.add(mTop[i]);
			} else {
				// no more fields in this sub-reader
			}
		}

		mNumTop = 0;

		// gather equal top fields
		if (mQueue.size() > 0) {
			while (true) {
				mTop[mNumTop++] = mQueue.pop();
				if (mQueue.size() == 0 || !(mQueue.top()).mCurrent.equals(mTop[0].mCurrent)) 
					break;
			}
			mCurrentField = mTop[0].mCurrent;
			
		} else {
			mCurrentField = null;
		}

		return mCurrentField;
	}

	@Override
	public ITerms getTerms() throws IOException {
		// Ask our parent MultiFields:
		return mFields.getTerms(mCurrentField);
	}

	public final static class FieldsEnumWithSlice {
		//public static final FieldsEnumWithSlice[] EMPTY_ARRAY = new FieldsEnumWithSlice[0];
		
		private final IFieldsEnum mFields;
		private final ReaderSlice mSlice;
		private final int mIndex;
		private String mCurrent;

		public FieldsEnumWithSlice(IFieldsEnum fields, ReaderSlice slice, int index) {
			mSlice = slice;
			mIndex = index;
			assert slice.getLength() >= 0: "length=" + slice.getLength();
			mFields = fields;
		}
		
		public final ReaderSlice getSlice() { return mSlice; }
		public final int getIndex() { return mIndex; }
	}

	private final static class FieldMergeQueue extends PriorityQueue<FieldsEnumWithSlice> {
		FieldMergeQueue(int size) {
			super(size);
		}

		@Override
		protected final boolean lessThan(FieldsEnumWithSlice fieldsA, FieldsEnumWithSlice fieldsB) {
			// No need to break ties by field name: TermsEnum handles that
			return fieldsA.mCurrent.compareTo(fieldsB.mCurrent) < 0;
		}
	}
	
}

