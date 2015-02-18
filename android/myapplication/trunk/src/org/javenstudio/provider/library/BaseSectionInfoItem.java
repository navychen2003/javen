package org.javenstudio.provider.library;

import org.javenstudio.provider.library.details.SectionInfoItemBase;
import org.javenstudio.provider.library.details.SectionInfoProvider;

public abstract class BaseSectionInfoItem extends SectionInfoItemBase {

	public BaseSectionInfoItem(SectionInfoProvider p, 
			ISectionInfoData data) { 
		super(p, data);
	}
	
}
