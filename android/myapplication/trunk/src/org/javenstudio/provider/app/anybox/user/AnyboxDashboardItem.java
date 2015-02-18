package org.javenstudio.provider.app.anybox.user;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.account.dashboard.HistorySectionItem;
import org.javenstudio.provider.account.dashboard.IHistorySectionData;
import org.javenstudio.provider.app.anybox.AnyboxAccount;
import org.javenstudio.provider.app.anybox.AnyboxDashboard;
import org.javenstudio.provider.app.anybox.AnyboxHistory;

public class AnyboxDashboardItem extends HistorySectionItem {
	private static final Logger LOG = Logger.getLogger(AnyboxDashboardItem.class);

	public AnyboxDashboardItem(AnyboxDashboardProvider provider, 
			IHistorySectionData data) {
		super(provider, data);
	}
	
	@Override
	public AnyboxDashboardProvider getProvider() {
		return (AnyboxDashboardProvider)super.getProvider();
	}
	
	@Override
	protected void onImageClick(IActivity activity) {
		if (activity == null || activity.isDestroyed()) return;
		if (LOG.isDebugEnabled()) LOG.debug("onImageClick: item=" + this);
	}
	
	@Override
	protected void onTitleClick(IActivity activity) {
		if (activity == null || activity.isDestroyed()) return;
		if (LOG.isDebugEnabled()) LOG.debug("onTitleClick: item=" + this);
		getProvider().getAccountUser().getApp().openSectionDetails(activity.getActivity(), 
				getProvider().getAccountUser(), getData());
	}
	
	protected AnyboxHistory.SectionData[] getDashboardSections() {
		AnyboxAccount account = (AnyboxAccount)getProvider().getAccountUser();
		AnyboxDashboard dashboard = account != null ? account.getDashboard() : null;
		return dashboard != null ? dashboard.getSections() : null;
	}
	
}
