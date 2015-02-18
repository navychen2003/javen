package org.javenstudio.provider.account;

import android.widget.ListAdapter;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.IMenuOperation;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.provider.Provider;
import org.javenstudio.provider.ProviderCallback;

public class AccountActionProvider extends AccountActionTab {

	private final Provider mProvider;
	
	public AccountActionProvider(AccountInfoItem item, 
			Provider provider, String name) { 
		this(item, provider, name, 0);
	}
	
	public AccountActionProvider(AccountInfoItem item, 
			Provider provider, String name, int iconRes) { 
		super(item, name, iconRes);
		mProvider = provider;
	}
	
	public Provider getProvider() { return mProvider; }
	
	@Override
	public IMenuOperation getMenuOperation(IActivity activity) { 
		Provider p = getProvider();
		if (p != null) { 
			IMenuOperation mo = p.getMenuOperation();
			if (mo != null) 
				return mo;
		}
		return super.getMenuOperation(activity);
	}
	
	@Override
	public ListAdapter getAdapter(IActivity activity) { 
		return null;
	}
	
	@Override
	public void reloadOnThread(ProviderCallback callback, 
			ReloadType type, long reloadId) {
		Provider p = getProvider();
		if (p != null) 
			p.reloadOnThread(callback, type, reloadId);
	}
	
}
