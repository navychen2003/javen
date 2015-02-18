package org.javenstudio.provider.app.anybox.user;

import android.app.Activity;

import org.javenstudio.provider.account.AccountOptionsMenu;
import org.javenstudio.provider.app.anybox.AnyboxAccount;
import org.javenstudio.provider.app.anybox.AnyboxApp;

public abstract class AnyboxAccountOptionsMenu extends AccountOptionsMenu {
	//private static final Logger LOG = Logger.getLogger(AnyboxAccountOptionsMenu.class);

	private final AnyboxApp mApp;
	
	public AnyboxAccountOptionsMenu(AnyboxApp app) {
		if (app == null) throw new NullPointerException();
		mApp = app;
	}
	
	public AnyboxApp getAccountApp() { return mApp; }
	public AnyboxAccount getAccountUser() { return getAccountApp().getAccount(); }
	
	@Override
	public boolean hasOptionsMenu(Activity activity) {
		return true;
	}
	
}
