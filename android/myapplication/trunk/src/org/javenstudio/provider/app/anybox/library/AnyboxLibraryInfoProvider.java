package org.javenstudio.provider.app.anybox.library;

import org.javenstudio.android.account.AccountApp;
import org.javenstudio.android.account.AccountUser;
import org.javenstudio.provider.library.BaseLibraryInfoProvider;

public class AnyboxLibraryInfoProvider extends BaseLibraryInfoProvider {

	public AnyboxLibraryInfoProvider(AccountApp app, AccountUser account, 
			String name, int iconRes) {
		super(app, account, name, iconRes);
	}
	
}
