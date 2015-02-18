package org.javenstudio.provider.account.dashboard;

import org.javenstudio.android.account.AccountUser;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.provider.ProviderBase;
import org.javenstudio.provider.ProviderCallback;

public abstract class DashboardProvider extends ProviderBase {
	//private static final Logger LOG = Logger.getLogger(DashboardProvider.class);

	private final DashboardBinder mBinder;
	private final DashboardDataSets mDataSets;
	
	private OnDashboardClickListener mItemClickListener = null;
	private OnDashboardClickListener mViewClickListener = null;
	private OnDashboardClickListener mUserClickListener = null;
	
	public DashboardProvider(String name, int iconRes, 
			DashboardFactory factory) { 
		super(name, iconRes);
		mBinder = factory.createDashboardBinder(this);
		mDataSets = factory.createDashboardDataSets(this);
	}

	public abstract AccountUser getAccountUser();
	public DashboardDataSets getDashboardDataSets() { return mDataSets; }
	
	public void setOnDashboardItemClickListener(OnDashboardClickListener l) { mItemClickListener = l; }
	public OnDashboardClickListener getOnDashboardItemClickListener() { return mItemClickListener; }
	
	public void setOnDashboardViewClickListener(OnDashboardClickListener l) { mViewClickListener = l; }
	public OnDashboardClickListener getOnDashboardViewClickListener() { return mViewClickListener; }
	
	public void setOnDashboardUserClickListener(OnDashboardClickListener l) { mUserClickListener = l; }
	public OnDashboardClickListener getOnDashboardUserClickListener() { return mUserClickListener; }
	
	@Override
	public DashboardBinder getBinder() {
		return mBinder;
	}

	@Override
	public void setContentBackground(IActivity activity) {
		getBinder().bindBackgroundView(activity);
	}
	
	@Override
	public boolean isContentProgressEnabled() { 
		return getDashboardDataSets().getCount() == 0; 
	}
	
	@Override
	public void reloadOnThread(ProviderCallback callback, 
			ReloadType type, long reloadId) { 
	}
	
	protected void postClearDataSets(final ProviderCallback callback) { 
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					getDashboardDataSets().clear();
					if (callback != null) {
						callback.getController().getModel()
							.callbackOnDataSetClear(getDashboardDataSets()); 
					}
				}
			});
	}
	
	protected void postAddDashboardItem(final ProviderCallback callback, 
			final DashboardItem... item) { 
		if (item == null) return;
		
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					getDashboardDataSets().addDashboardItem(item);
					if (callback != null) {
						callback.getController().getModel()
							.callbackOnDataSetUpdate(item); 
					}
				}
			});
	}
	
	protected void postNotifyChanged(final ProviderCallback callback) { 
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					getDashboardDataSets().notifyContentChanged(true);
					getDashboardDataSets().notifyDataSetChanged();
					if (callback != null) {
						callback.getController().getModel()
							.callbackOnDataSetChanged(getDashboardDataSets());
					}
				}
			});
	}
	
}
