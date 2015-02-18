package org.javenstudio.provider.app.anybox.user;

import org.javenstudio.provider.account.AccountInfoBinder;

public class AnyboxAccountBinder extends AccountInfoBinder {

	private final AnyboxAccountProvider mProvider;
	
	public AnyboxAccountBinder(AnyboxAccountProvider p) { 
		mProvider = p;
	}
	
	@Override
	public AnyboxAccountProvider getProvider() { 
		return mProvider; 
	}
	
}
