package org.javenstudio.hornet.query;

import java.io.IOException;

import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.common.indexdb.search.SortField;
import org.javenstudio.hornet.search.AdvancedSortField;

public class ValueSourceSortField extends AdvancedSortField {

	private final ValueSource mSource;
	
	public ValueSourceSortField(ValueSource source, boolean reverse) {
		super(source.getDescription(), SortField.Type.REWRITEABLE, reverse);
		mSource = source;
	}

	@Override
	public SortField rewrite(ISearcher searcher) throws IOException {
		ValueSourceContext context = ValueSourceContext.create(searcher);
		mSource.createWeight(context, searcher);
		
		return new AdvancedSortField(getField(), 
				new ValueSourceComparatorSource(context, mSource), 
				getReverse());
	}
	
}
