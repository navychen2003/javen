package org.javenstudio.provider.app.picasa;

import org.javenstudio.provider.people.user.UserBinder;

final class PicasaUserBinder extends UserBinder {

	private final PicasaUserInfoProvider mProvider;
	
	public PicasaUserBinder(PicasaUserInfoProvider p) { 
		mProvider = p;
	}
	
	@Override
	protected PicasaUserInfoProvider getProvider() { 
		return mProvider; 
	}
	
}
