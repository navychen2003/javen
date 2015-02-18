package org.javenstudio.provider.app.anybox.user;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.provider.account.space.SpacesProvider;
import org.javenstudio.provider.app.anybox.AnyboxAccountSpace;
import org.javenstudio.provider.app.anybox.AnyboxApp;
import org.javenstudio.provider.app.anybox.AnyboxAccount;

public class AnyboxSpacesProvider extends SpacesProvider {

	private final AnyboxApp mApp;
	private final AnyboxAccount mAccount;
	
	public AnyboxSpacesProvider(AnyboxApp app, AnyboxAccount account, 
			int titleRes, int iconRes, int indicatorRes, AnyboxUserClickListener listener) { 
		super(app.getContext().getResources().getString(titleRes), iconRes);
		mApp = app;
		mAccount = account;
		//setOptionsMenu(new AnyboxAccountOptionsMenu(app, account));
		setHomeAsUpIndicator(indicatorRes);
	}
	
	public AnyboxApp getAccountApp() { return mApp; }
	public AnyboxAccount getAccountUser() { return mAccount; }
	
	@Override
	public CharSequence getLastUpdatedLabel(IActivity activity) { 
		AnyboxAccount account = getAccountUser();
		if (account != null) {
			AnyboxAccountSpace spaceInfo = account.getSpaceInfo();
			if (spaceInfo != null) {
				long requestTime = spaceInfo.getRequestTime();
				return AppResources.getInstance().formatRefreshTime(requestTime);
			}
		}
		return null;
	}
	
}
