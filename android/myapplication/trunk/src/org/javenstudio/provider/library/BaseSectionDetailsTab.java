package org.javenstudio.provider.library;

import org.javenstudio.provider.library.details.SectionDetailsTab;
import org.javenstudio.provider.library.details.SectionInfoItem;

public abstract class BaseSectionDetailsTab extends SectionDetailsTab {

	public BaseSectionDetailsTab(SectionInfoItem item, String name) { 
		this(item, name, 0);
	}
	
	public BaseSectionDetailsTab(SectionInfoItem item, String name, int iconRes) { 
		super(item, name, iconRes);
	}
	
	public abstract SectionInfoItem getSectionItem();
	
}
