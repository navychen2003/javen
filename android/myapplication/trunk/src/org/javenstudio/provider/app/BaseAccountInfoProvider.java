package org.javenstudio.provider.app;

import org.javenstudio.android.account.AccountApp;
import org.javenstudio.provider.account.AccountMenuBinder;
import org.javenstudio.provider.account.AccountInfoProvider;
import org.javenstudio.provider.account.AccountSecondaryMenuBinder;

public abstract class BaseAccountInfoProvider extends AccountInfoProvider {

	public BaseAccountInfoProvider(AccountApp app, String name, int iconRes) { 
		super(app, name, iconRes);
	}
	
	@Override
	public AccountMenuBinder getMenuBinder() {
		return null;
	}
	
	@Override
	public AccountSecondaryMenuBinder getSecondaryMenuBinder() {
		return null;
	}
	
}
