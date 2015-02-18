package org.javenstudio.provider.library.details;

import org.javenstudio.provider.ProviderActionTabBase;

public class SectionActionTab extends ProviderActionTabBase {
	//private static final Logger LOG = Logger.getLogger(SectionActionTab.class);

	public SectionActionTab(SectionInfoItem item, String name) { 
		this(item, name, 0);
	}
	
	public SectionActionTab(SectionInfoItem item, String name, int iconRes) { 
		super(item, name, iconRes);
	}
	
}
