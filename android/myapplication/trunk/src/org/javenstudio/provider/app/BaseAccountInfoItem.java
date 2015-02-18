package org.javenstudio.provider.app;

import org.javenstudio.android.account.AccountUser;
import org.javenstudio.provider.account.AccountInfoItemBase;

public abstract class BaseAccountInfoItem extends AccountInfoItemBase {

	public BaseAccountInfoItem(BaseAccountInfoProvider p, AccountUser user) { 
		super(p, user);
	}
	
}
