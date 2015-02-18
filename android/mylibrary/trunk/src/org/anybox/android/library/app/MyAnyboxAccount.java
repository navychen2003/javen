package org.anybox.android.library.app;

import org.javenstudio.android.data.DataApp;
import org.javenstudio.android.entitydb.content.AccountData;
import org.javenstudio.android.entitydb.content.HostData;
import org.javenstudio.provider.app.anybox.AnyboxAccount;
import org.javenstudio.provider.app.anybox.AnyboxApp;
import org.javenstudio.provider.library.select.SelectOperation;

public class MyAnyboxAccount extends AnyboxAccount {

	public MyAnyboxAccount(AnyboxApp app, AccountData account, 
			HostData host, long authTime) {
		super(app, account, host, authTime);
	}
	
	@Override
	protected SelectOperation createSelectOperation() {
		return new MySelectOperation() { 
				@Override
				public DataApp getDataApp() { 
					return getApp().getDataApp();
				}
			};
	}
	
}
