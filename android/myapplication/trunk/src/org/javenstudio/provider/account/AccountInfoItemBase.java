package org.javenstudio.provider.account;

import android.graphics.drawable.Drawable;
import android.view.View;

import org.javenstudio.android.account.AccountUser;

public class AccountInfoItemBase extends AccountInfoItem {

	private final AccountInfoProvider mProvider;
	
	public AccountInfoItemBase(AccountInfoProvider p, AccountUser user) { 
		super(p.getAccountApp(), user);
		mProvider = p;
	}
	
	public AccountInfoProvider getProvider() { return mProvider; }
	
	@Override
	public Drawable getProviderIcon() { 
		return getProvider().getIcon();
	}
	
	@Override
	protected void onUpdateViewOnVisible(boolean restartSlide) { 
		AccountInfoBinder binder = getAccountBinder();
		if (binder != null) binder.onBindView(this);
	}
	
	private AccountInfoBinder getAccountBinder() { 
		final AccountInfoBinder binder = (AccountInfoBinder)getProvider().getBinder();
		final View view = getBindView();
		
		if (binder == null || view == null) 
			return null;
		
		return binder;
	}
	
}
