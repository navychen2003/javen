package org.javenstudio.provider.account.list;

import org.javenstudio.android.account.AccountApp;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.provider.ProviderBase;
import org.javenstudio.provider.ProviderCallback;

public class AccountListProvider extends ProviderBase {

	private final AccountApp mApp;
	private final AccountListBinder mBinder;
	private final AccountListDataSets mDataSets;
	
	private OnAccountListClickListener mClickListener = null;
	
	public AccountListProvider(AccountApp app, String name, int iconRes, 
			AccountListFactory factory) { 
		super(name, iconRes);
		mApp = app;
		mBinder = factory.createAccountListBinder(this);
		mDataSets = factory.createAccountListDataSets(this);
	}

	public final AccountApp getAccountApp() { return mApp; }
	
	@Override
	public AccountListBinder getBinder() {
		return mBinder;
	}
	
	public void setOnItemClickListener(OnAccountListClickListener l) { mClickListener = l; }
	public OnAccountListClickListener getOnItemClickListener() { return mClickListener; }
	
	public synchronized AccountListDataSets getAccountListDataSets() { 
		if (mDataSets.getCount() == 0) { 
			//SystemUser[] accounts = PicasaHelper.getAccounts(activity.getActivity());
			//if (accounts != null) { 
			//	for (SystemUser info : accounts) { 
			//		final AccountListItem item = new AccountListItem(this, info);
			//		mDataSets.addAccountItem(item, false);
			//	}
			//}
		}
		
		return mDataSets;
	}
	
	@Override
	public synchronized void reloadOnThread(ProviderCallback callback, 
			ReloadType type, long reloadId) { 
	}
	
}
