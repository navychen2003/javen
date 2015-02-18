package org.javenstudio.falcon.search.comparator;

import java.io.IOException;

import org.javenstudio.common.indexdb.IFieldComparator;
import org.javenstudio.common.indexdb.search.FieldComparatorSource;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.UnicodeUtil;

public class MissingStringLastSource extends FieldComparatorSource {
	
	private final BytesRef mMissingValueProxy;

	public MissingStringLastSource() {
		this(UnicodeUtil.BIG_TERM);
	}

	/** 
	 * Creates a {@link FieldComparatorSource} that sorts null last in a normal ascending sort.
	 * <tt>missingValueProxy</tt> as the value to return from FieldComparator.value()
	 *
	 * @param missingValueProxy   The value returned when sortValue() is called for 
	 * a document missing the sort field.
	 * This value is *not* normally used for sorting.
	 */
	public MissingStringLastSource(BytesRef missingValueProxy) {
		mMissingValueProxy = missingValueProxy;
	}

	@Override
	public IFieldComparator<?> newComparator(String fieldname, int numHits, 
			int sortPos, boolean reversed) throws IOException {
		return new TermOrdValComparator(numHits, fieldname, 
				sortPos, reversed, mMissingValueProxy);
	}
  
}
