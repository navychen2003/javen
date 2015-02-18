package org.javenstudio.provider.app.anybox.library;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.ProviderActionItem;
import org.javenstudio.provider.ProviderCallback;
import org.javenstudio.provider.app.anybox.AnyboxAccount;
import org.javenstudio.provider.app.anybox.AnyboxApp;
import org.javenstudio.provider.app.anybox.AnyboxLibrary;
import org.javenstudio.provider.library.BaseLibrariesProvider;
import org.javenstudio.provider.library.ISectionData;
import org.javenstudio.provider.library.ISectionList;
import org.javenstudio.provider.library.list.LibraryActionItem;
import org.javenstudio.provider.library.list.LibraryProvider;

public class AnyboxLibrariesProvider extends BaseLibrariesProvider {
	private static final Logger LOG = Logger.getLogger(AnyboxLibrariesProvider.class);

	private final AnyboxAccount mAccount;
	private long mRequestTime = 0;
	
	public AnyboxLibrariesProvider(AnyboxAccount account, 
			int nameRes, int iconRes, int indicatorRes) { 
		super(ResourceHelper.getResources().getString(nameRes), 
				iconRes);
		mAccount = account;
		//setOptionsMenu(new AnyboxAccountOptionsMenu(app, null));
		setHomeAsUpIndicator(indicatorRes);
	}
	
	public AnyboxApp getAccountApp() { return mAccount.getApp(); }
	public AnyboxAccount getAccountUser() { return mAccount; }
	
	@Override
	public ProviderActionItem[] getActionItems(IActivity activity) { 
		synchronized (mProviders) {
			AnyboxLibrary[] libraries = getAccountUser().getLibraryList();
			long requestTime = getAccountUser().getLibraryRequestTime();
			if (libraries != null && requestTime > mRequestTime) {
				mRequestTime = requestTime;
				clearProviders();
				
				for (AnyboxLibrary library : libraries) {
					if (library == null) continue;
					LibraryProvider p = getAccountApp().createLibraryProvider(
							getAccountUser(), library);
					if (p != null) addProvider(p);
				}
			}
			
			return super.getActionItems(activity);
		}
	}
	
	@Override
	public synchronized void reloadOnThread(ProviderCallback callback, 
			ReloadType type, long reloadId) { 
		super.reloadOnThread(callback, type, reloadId);
	}
	
	@Override
	protected LibraryActionItem createLibraryActionItem(final IActivity activity, 
			final LibraryProvider item, final ISectionList folder) {
		return new LibraryActionItem(this, item, folder, activity) { 
				@Override
				public void onItemInfoClick() {
					if (LOG.isDebugEnabled()) LOG.debug("onItemInfoClick: item=" + this);
					AnyboxLibraryProvider p = (AnyboxLibraryProvider)getProvider();
					ISectionList list = getSectionList();
					if (list != null && list instanceof ISectionData) {
						ISectionData data = (ISectionData)list;
						p.getAccountApp().openSectionDetails(activity.getActivity(), 
								p.getAccountUser(), data);
					}
				}
			};
	}
	
}
