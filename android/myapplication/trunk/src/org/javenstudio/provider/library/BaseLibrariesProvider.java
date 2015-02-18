package org.javenstudio.provider.library;

import org.javenstudio.android.account.AccountApp;
import org.javenstudio.android.account.AccountUser;
import org.javenstudio.provider.library.list.LibrariesProvider;

public abstract class BaseLibrariesProvider extends LibrariesProvider {

	public BaseLibrariesProvider(String name, int iconRes) { 
		super(name, iconRes);
	}
	
	public abstract AccountApp getAccountApp();
	public abstract AccountUser getAccountUser();
	
}
