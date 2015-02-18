package org.javenstudio.provider.app.picasa;

import org.javenstudio.provider.account.AccountInfoBinder;

final class PicasaAccountBinder extends AccountInfoBinder {

	private final PicasaAccountProvider mProvider;
	
	public PicasaAccountBinder(PicasaAccountProvider p) { 
		mProvider = p;
	}
	
	@Override
	public PicasaAccountProvider getProvider() { 
		return mProvider; 
	}
	
}
