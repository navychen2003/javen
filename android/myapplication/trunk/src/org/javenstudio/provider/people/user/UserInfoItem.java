package org.javenstudio.provider.people.user;

import android.graphics.drawable.Drawable;
import android.view.View;

public class UserInfoItem extends UserItem {

	private final UserInfoProvider mProvider;
	
	public UserInfoItem(UserInfoProvider p, String userId) { 
		this(p, userId, null);
	}
	
	public UserInfoItem(UserInfoProvider p, String userId, String userName) { 
		super(userId, userName);
		mProvider = p;
	}
	
	public UserInfoProvider getProvider() { return mProvider; }
	
	@Override
	public Drawable getProviderIcon() { 
		return getProvider().getIcon();
	}
	
	@Override
	protected void onUpdateViews() { 
		UserBinder binder = getUserBinder();
		if (binder != null) binder.onUpdateViews(this);
	}
	
	private UserBinder getUserBinder() { 
		final UserBinder binder = (UserBinder)getProvider().getBinder();
		final View view = getBindView();
		
		if (binder == null || view == null) 
			return null;
		
		return binder;
	}
	
}
