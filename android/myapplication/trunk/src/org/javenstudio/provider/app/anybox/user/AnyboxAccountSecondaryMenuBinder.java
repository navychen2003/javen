package org.javenstudio.provider.app.anybox.user;

import org.javenstudio.provider.account.AccountSecondaryMenuBinder;

public class AnyboxAccountSecondaryMenuBinder extends AccountSecondaryMenuBinder {
	//private static final Logger LOG = Logger.getLogger(AnyboxAccountSecondaryMenuBinder.class);

	private final AnyboxAccountProvider mProvider;
	
	public AnyboxAccountSecondaryMenuBinder(AnyboxAccountProvider p) { 
		mProvider = p;
	}
	
	@Override
	public AnyboxAccountProvider getProvider() { 
		return mProvider; 
	}
	
}
