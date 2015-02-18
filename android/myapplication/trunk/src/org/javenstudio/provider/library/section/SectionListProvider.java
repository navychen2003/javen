package org.javenstudio.provider.library.section;

import org.javenstudio.android.account.AccountApp;
import org.javenstudio.android.account.AccountUser;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.ViewType;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.ProviderBase;
import org.javenstudio.provider.ProviderCallback;
import org.javenstudio.provider.library.ISectionList;
import org.javenstudio.provider.library.IVisibleData;

public abstract class SectionListProvider extends ProviderBase {
	private static final Logger LOG = Logger.getLogger(SectionListProvider.class);

	public static final int ACTION_UPLOAD = 1;
	public static final int ACTION_CREATE = 2;
	public static final int ACTION_SCAN = 3;
	
	private final AccountApp mApp;
	private final AccountUser mAccount;
	//private final SectionListDataSets mDataSets;
	private final SectionListFactory mFactory;
	
	private OnSectionListClickListener mClickListener = null;
	private ISectionListBinder mListBinder = null;
	private ISectionListBinder mGridBinder = null;
	
	public SectionListProvider(AccountApp app, AccountUser account, 
			String name, int iconRes, SectionListFactory factory) { 
		super(name, iconRes);
		mApp = app;
		mAccount = account;
		mFactory = factory;
		//mDataSets = factory.createSectionListDataSets(this);
	}

	public AccountApp getAccountApp() { return mApp; }
	public AccountUser getAccountUser() { return mAccount; }
	public SectionListFactory getFactory() { return mFactory; }
	
	public void setOnItemClickListener(OnSectionListClickListener l) { mClickListener = l; }
	public OnSectionListClickListener getOnItemClickListener() { return mClickListener; }
	
	public int getSectionListTotalCount() { return 0; }
	
	public void setFirstVisibleItem(IVisibleData data) {}
	public IVisibleData getFirstVisibleItem() { return null; }
	
	public abstract ISectionList getSectionList();
	public void startSelectMode(IActivity activity, SectionListItem item) {}
	
	public void onActionButtonClick(IActivity activity, int action) {}
	
	@Override
	public synchronized ISectionListBinder getBinder() {
		if (getFactory().getViewType().getSelectType() == ViewType.Type.GRID) {
			if (mGridBinder == null)
				mGridBinder = getFactory().createSectionGridBinder(this);
			return mGridBinder;
		} else {
			if (mListBinder == null)
				mListBinder = getFactory().createSectionListBinder(this);
			return mListBinder;
		}
	}
	
	public SectionListDataSets getSectionListDataSets() { 
		return getBinder().getDataSets(); 
	}
	
	@Override
	public void setContentBackground(IActivity activity) {
		getBinder().bindBackgroundView(activity);
	}
	
	@Override
	public boolean isContentProgressEnabled() { 
		return true; //getSectionListDataSets().getCount() == 0; 
	}
	
	@Override
	public void reloadOnThread(ProviderCallback callback, 
			ReloadType type, long reloadId) { 
	}
	
	public void postSetVisibleSelection(final IVisibleData data) {
		if (data == null) return;
		if (LOG.isDebugEnabled()) 
			LOG.debug("postSetVisibleSelection: data=" + data);
		
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					getBinder().setVisibleSelection(data);
				}
			});
	}
	
	public void postClearDataSets(final ProviderCallback callback) { 
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					getSectionListDataSets().clear();
					if (callback != null) {
						callback.getController().getModel()
							.callbackOnDataSetClear(getSectionListDataSets()); 
					}
				}
			});
	}
	
	public void postAddSectionListItem(final ProviderCallback callback, 
			final SectionListItem... item) { 
		if (item == null) return;
		
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					getSectionListDataSets().addSectionListItem(item);
					if (callback != null) {
						callback.getController().getModel()
							.callbackOnDataSetUpdate(item); 
					}
				}
			});
	}
	
	public void postAddSectionEmptyItem(final ProviderCallback callback) { 
		SectionListItem item = getFactory().createEmptyItem(this);
		if (item != null) postAddSectionListItem(callback, item);
	}
	
	public void postNotifyChanged(final ProviderCallback callback) { 
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					getSectionListDataSets().notifyContentChanged(true);
					getSectionListDataSets().notifyDataSetChanged();
					if (callback != null) {
						callback.getController().getModel()
							.callbackOnDataSetChanged(getSectionListDataSets());
					}
				}
			});
	}
	
	public void postShowCenterView(final boolean show) {
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					getBinder().showAboveCenterView(show);
				}
			});
	}
	
}
