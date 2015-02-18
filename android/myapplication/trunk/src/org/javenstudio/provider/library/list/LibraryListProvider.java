package org.javenstudio.provider.library.list;

import org.javenstudio.android.account.AccountApp;
import org.javenstudio.android.account.AccountUser;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.provider.ProviderBase;
import org.javenstudio.provider.ProviderCallback;

public class LibraryListProvider extends ProviderBase {

	private final AccountApp mApp;
	private final AccountUser mAccount;
	private final LibraryListBinder mBinder;
	private final LibraryListDataSets mDataSets;
	
	private OnLibraryListClickListener mClickListener = null;
	
	public LibraryListProvider(AccountApp app, AccountUser account, 
			String name, int iconRes, LibraryListFactory factory) { 
		super(name, iconRes);
		mApp = app;
		mAccount = account;
		mBinder = factory.createLibraryListBinder(this);
		mDataSets = factory.createLibraryListDataSets(this);
	}

	public final AccountApp getAccountApp() { return mApp; }
	public final AccountUser getAccountUser() { return mAccount; }
	
	@Override
	public LibraryListBinder getBinder() {
		return mBinder;
	}
	
	public void setOnItemClickListener(OnLibraryListClickListener l) { mClickListener = l; }
	public OnLibraryListClickListener getOnItemClickListener() { return mClickListener; }
	
	public synchronized LibraryListDataSets getLibraryListDataSets() { 
		if (mDataSets.getCount() == 0) { 
			//SystemUser[] accounts = PicasaHelper.getAccounts(activity.getActivity());
			//if (accounts != null) { 
			//	for (SystemUser info : accounts) { 
			//		final LibraryItem item = new LibraryItem(this, info);
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
