package org.javenstudio.provider.app.anybox.library;

import android.app.Activity;

import org.javenstudio.provider.app.anybox.AnyboxAccount;
import org.javenstudio.provider.app.anybox.AnyboxApp;
import org.javenstudio.provider.library.SectionOptionsMenu;

public class AnyboxSectionOptionsMenu extends SectionOptionsMenu {
	//private static final Logger LOG = Logger.getLogger(AnyboxSectionOptionsMenu.class);

	private final AnyboxApp mApp;
	
	public AnyboxSectionOptionsMenu(AnyboxApp app) {
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
