package org.javenstudio.provider.app.anybox.user;

import java.util.ArrayList;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.provider.ProviderCallback;
import org.javenstudio.provider.account.dashboard.DashboardFactory;
import org.javenstudio.provider.account.dashboard.DashboardItem;
import org.javenstudio.provider.account.dashboard.DashboardProvider;
import org.javenstudio.provider.account.dashboard.HistorySectionItem;
import org.javenstudio.provider.account.dashboard.IHistorySectionData;
import org.javenstudio.provider.app.anybox.AnyboxAccount;
import org.javenstudio.provider.app.anybox.AnyboxDashboard;
import org.javenstudio.provider.app.anybox.AnyboxHistory;

public class AnyboxDashboardProvider extends DashboardProvider {
	//private static final Logger LOG = Logger.getLogger(AnyboxDashboardProvider.class);
	
	private final AnyboxAccount mAccount;
	private final DashboardFactory mFactory;
	private boolean mReloaded = false;
	
	public AnyboxDashboardProvider(AnyboxAccount account, int nameRes, 
			int iconRes, int indicatorRes, DashboardFactory factory) { 
		super(ResourceHelper.getResources().getString(nameRes), 
				iconRes, factory);
		mFactory = factory;
		mAccount = account;
		//setOptionsMenu(new AnyboxAccountOptionsMenu(app, null));
		setHomeAsUpIndicator(indicatorRes);
	}
	
	public AnyboxAccount getAccountUser() { return mAccount; }
	
	@Override
	public CharSequence getLastUpdatedLabel(IActivity activity) { 
		AnyboxAccount account = getAccountUser();
		if (account != null) {
			AnyboxDashboard dashboard = account.getDashboard();
			if (dashboard != null) {
				long requestTime = dashboard.getRequestTime();
				return AppResources.getInstance().formatRefreshTime(requestTime);
			}
		}
		return null;
	}
	
	@Override
	public synchronized void reloadOnThread(final ProviderCallback callback, 
			ReloadType type, long reloadId) { 
		if (mReloaded == false || type == ReloadType.FORCE) {
	    	if (type == ReloadType.FORCE || getDashboardDataSets().getCount() <= 0) {
	    		AnyboxAccount account = getAccountUser();
	    		if (type == ReloadType.FORCE || (account != null && account.getSpaceInfo() == null)) 
	    			AnyboxDashboard.getDashboard(account, callback);
	    		
	    		postClearDataSets(callback);
	    		if (account != null) postAddDashboardItems(callback, account.getDashboard());
	    		//postNotifyChanged(callback);
	    	}
	    	mReloaded = true;
		}
	}
	
	private void postAddDashboardItems(final ProviderCallback callback, 
			AnyboxDashboard dashboard) {
		if (dashboard == null) return;
		
		ArrayList<DashboardItem> list = new ArrayList<DashboardItem>();
		
		AnyboxHistory.SectionData[] sections = dashboard.getSections();
		if (sections != null) {
			for (AnyboxHistory.SectionData section : sections) {
				if (section != null) {
					HistorySectionItem item = createSectionItem(section);
					if (item != null) list.add(item);
				}
			}
		}
		
		if (list.size() == 0)
			list.add(mFactory.createEmptyItem(this));
		
		postAddDashboardItem(callback, list.toArray(new DashboardItem[list.size()]));
	}
	
	protected HistorySectionItem createSectionItem(IHistorySectionData data) {
		if (data == null) return null;
		return new AnyboxDashboardItem(this, data);
	}
	
}
