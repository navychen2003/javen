package org.javenstudio.provider.app.anybox.user;

import org.javenstudio.provider.account.AccountMenuBinder;

public class AnyboxAccountMenuBinder extends AccountMenuBinder {
	//private static final Logger LOG = Logger.getLogger(AnyboxAccountMenuBinder.class);

	private final AnyboxAccountProvider mProvider;
	
	public AnyboxAccountMenuBinder(AnyboxAccountProvider p) { 
		mProvider = p;
	}
	
	@Override
	public AnyboxAccountProvider getProvider() { 
		return mProvider; 
	}

}
