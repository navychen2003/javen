package org.javenstudio.provider.library.details;

import org.javenstudio.android.account.AccountApp;
import org.javenstudio.android.account.AccountUser;
import org.javenstudio.provider.ProviderBase;
import org.javenstudio.provider.ProviderBinder;

public class LibraryInfoProvider extends ProviderBase {

	private final AccountApp mApp;
	private final AccountUser mAccount;
	private final LibraryInfoBinder mBinder;
	
	public LibraryInfoProvider(AccountApp app, AccountUser account, 
			String name, int iconRes) {
		super(name, iconRes);
		mApp = app;
		mAccount = account;
		mBinder = new LibraryInfoBinder();
	}
	
	public final AccountApp getAccountApp() { return mApp; }
	public final AccountUser getAccountUser() { return mAccount; }

	@Override
	public ProviderBinder getBinder() {
		return mBinder;
	}
	
}
