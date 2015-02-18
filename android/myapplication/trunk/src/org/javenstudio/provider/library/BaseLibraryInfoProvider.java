package org.javenstudio.provider.library;

import org.javenstudio.android.account.AccountApp;
import org.javenstudio.android.account.AccountUser;
import org.javenstudio.provider.library.details.LibraryInfoProvider;

public class BaseLibraryInfoProvider extends LibraryInfoProvider {

	public BaseLibraryInfoProvider(AccountApp app, AccountUser account, 
			String name, int iconRes) {
		super(app, account, name, iconRes);
	}
	
}
