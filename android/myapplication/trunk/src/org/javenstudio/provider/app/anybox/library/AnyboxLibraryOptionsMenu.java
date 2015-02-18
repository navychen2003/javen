package org.javenstudio.provider.app.anybox.library;

import android.app.Activity;

import org.javenstudio.provider.app.anybox.AnyboxAccount;
import org.javenstudio.provider.app.anybox.AnyboxApp;
import org.javenstudio.provider.library.LibraryOptionsMenu;

public class AnyboxLibraryOptionsMenu extends LibraryOptionsMenu {
	//private static final Logger LOG = Logger.getLogger(AnyboxLibraryOptionsMenu.class);

	private final AnyboxApp mApp;
	
	public AnyboxLibraryOptionsMenu(AnyboxApp app) {
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
