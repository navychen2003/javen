package org.javenstudio.provider.app.anybox.library;

import org.javenstudio.provider.library.BaseSectionInfoBinder;

public class AnyboxSectionInfoBinder extends BaseSectionInfoBinder {

	private final AnyboxSectionInfoProvider mProvider;
	
	public AnyboxSectionInfoBinder(AnyboxSectionInfoProvider provider) {
		if (provider == null) throw new NullPointerException();
		mProvider = provider;
	}

	@Override
	public AnyboxSectionInfoProvider getProvider() {
		return mProvider;
	}
	
}
