package org.javenstudio.provider.app.anybox.library;

import org.javenstudio.provider.library.list.LibraryFactory;
import org.javenstudio.provider.library.section.SectionGridBinder;
import org.javenstudio.provider.library.section.SectionListBinder;
import org.javenstudio.provider.library.section.SectionListProvider;

public abstract class AnyboxLibraryFactory extends LibraryFactory {

	@Override
	public SectionListBinder createSectionListBinder(SectionListProvider provider) {
		return new AnyboxLibraryListBinder((AnyboxLibraryProvider)provider, this);
	}
	
	@Override
	public SectionGridBinder createSectionGridBinder(SectionListProvider provider) {
		return new AnyboxLibraryGridBinder((AnyboxLibraryProvider)provider, this);
	}
	
}
