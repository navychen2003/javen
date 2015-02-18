package org.javenstudio.provider.library;

import org.javenstudio.android.account.AccountApp;
import org.javenstudio.android.account.AccountUser;
import org.javenstudio.provider.library.details.SectionInfoProvider;

public abstract class BaseSectionInfoProvider extends SectionInfoProvider {

	public BaseSectionInfoProvider(AccountApp app, AccountUser account, 
			String name, int iconRes) {
		super(app, account, name, iconRes);
	}

}
