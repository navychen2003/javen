package org.javenstudio.provider.library.list;

import org.javenstudio.provider.ProviderList;
import org.javenstudio.provider.ProviderListBinder;

public class LibrariesBinder extends ProviderListBinder {

	private final LibrariesProvider mProvider;
	
	public LibrariesBinder(LibrariesProvider provider) {
		if (provider == null) throw new NullPointerException();
		mProvider = provider;
	}
	
	@Override
	public ProviderList getProvider() {
		return mProvider;
	}

}
