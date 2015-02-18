package org.javenstudio.provider.library.section;

import org.javenstudio.android.app.FilterType;
import org.javenstudio.android.app.SortType;
import org.javenstudio.android.app.ViewType;

public abstract class SectionListFactory {

	public SectionListDataSets createSectionListDataSets(SectionListProvider provider) {
		return new SectionListDataSets(provider, new SectionListCursorFactory());
	}
	
	public SectionListBinder createSectionListBinder(SectionListProvider provider) {
		return new SectionListBinder(provider, this);
	}
	
	public SectionGridBinder createSectionGridBinder(SectionListProvider provider) {
		return new SectionGridBinder(provider, this);
	}
	
	public abstract SectionListItem createEmptyItem(SectionListProvider provider);
	
	public abstract ViewType getViewType();
	public abstract SortType getSortType();
	public abstract FilterType getFilterType();
	
}
