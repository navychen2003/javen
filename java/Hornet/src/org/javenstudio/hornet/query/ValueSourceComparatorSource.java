package org.javenstudio.hornet.query;

import java.io.IOException;

import org.javenstudio.common.indexdb.IFieldComparator;
import org.javenstudio.common.indexdb.search.FieldComparatorSource;

public class ValueSourceComparatorSource extends FieldComparatorSource {

	private final ValueSourceContext mContext;
	private final ValueSource mSource;
	
	public ValueSourceComparatorSource(ValueSourceContext context, 
			ValueSource source) { 
		mContext = context;
		mSource = source;
	}
	
	@Override
	public IFieldComparator<?> newComparator(String fieldname, int numHits,
			int sortPos, boolean reversed) throws IOException {
		return new ValueSourceComparator(mContext, mSource, numHits);
	}

}
