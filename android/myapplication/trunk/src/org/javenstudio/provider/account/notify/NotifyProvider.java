package org.javenstudio.provider.account.notify;

import java.util.ArrayList;

import org.javenstudio.android.account.AccountUser;
import org.javenstudio.android.app.IFragment;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.ProviderBase;
import org.javenstudio.provider.ProviderCallback;

public abstract class NotifyProvider extends ProviderBase 
		implements AccountUser.OnDataChangeListener {
	private static final Logger LOG = Logger.getLogger(NotifyProvider.class);

	private final NotifyBinder mBinder;
	private final NotifyDataSets mDataSets;
	
	private OnNotifyClickListener mItemClickListener = null;
	private OnNotifyClickListener mViewClickListener = null;
	private OnNotifyClickListener mUserClickListener = null;
	
	public NotifyProvider(String name, int iconRes, 
			NotifyFactory factory) { 
		super(name, iconRes);
		mBinder = factory.createNotifyBinder(this);
		mDataSets = factory.createNotifyDataSets(this);
	}

	public abstract AccountUser getAccountUser();
	public NotifyDataSets getNotifyDataSets() { return mDataSets; }
	
	public void setOnNotifyItemClickListener(OnNotifyClickListener l) { mItemClickListener = l; }
	public OnNotifyClickListener getOnNotifyItemClickListener() { return mItemClickListener; }
	
	public void setOnNotifyViewClickListener(OnNotifyClickListener l) { mViewClickListener = l; }
	public OnNotifyClickListener getOnNotifyViewClickListener() { return mViewClickListener; }
	
	public void setOnNotifyUserClickListener(OnNotifyClickListener l) { mUserClickListener = l; }
	public OnNotifyClickListener getOnNotifyUserClickListener() { return mUserClickListener; }
	
	@Override
	public NotifyBinder getBinder() {
		return mBinder;
	}

	@Override
	public boolean isContentProgressEnabled() { 
		return getNotifyDataSets().getCount() == 0; 
	}
	
	@Override
	public void reloadOnThread(ProviderCallback callback, 
			ReloadType type, long reloadId) { 
	}
	
	protected void postClearDataSets(final ProviderCallback callback) { 
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					getNotifyDataSets().clear();
					if (callback != null) {
						callback.getController().getModel()
							.callbackOnDataSetClear(getNotifyDataSets()); 
					}
				}
			});
	}
	
	protected void postAddNotifyItem(final ProviderCallback callback, 
			final NotifyItem... item) { 
		if (item == null) return;
		
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					getNotifyDataSets().addNotifyItem(item);
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
					getNotifyDataSets().notifyContentChanged(true);
					getNotifyDataSets().notifyDataSetChanged();
					if (callback != null) {
						callback.getController().getModel()
							.callbackOnDataSetChanged(getNotifyDataSets());
					}
				}
			});
	}
	
	public void updateDataSets(INotifySource source, ProviderCallback callback) {
		if (source == null) return;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("updateDataSets: source=" + source 
					+ " callback=" + callback);
		}
		
		ArrayList<NotifyItem> list = new ArrayList<NotifyItem>();
		int count = 0;
		
		ISystemNotifyData systemData = source.getSystemNotifyData();
		if (systemData != null) {
			list.add(new SystemNotifyItem(this, systemData));
		}
		
		IInviteNotifyData inviteData = source.getInviteNotifyData();
		if (inviteData != null) {
			list.add(new InviteNotifyItem(this, inviteData));
			count ++;
		}
		
		IMessageNotifyData messageData = source.getMessageNotifyData();
		if (messageData != null) {
			list.add(new MessageNotifyItem(this, messageData));
			count ++;
		}
		
		IAnnouncementNotifyData announcementData = source.getAnnouncementNotifyData();
		if (announcementData != null) {
			list.add(new AnnouncementNotifyItem(this, announcementData));
			count ++;
		}
		
		postClearDataSets(callback);
		postAddNotifyItem(callback, list.toArray(new NotifyItem[list.size()]));
		//postNotifyChanged(callback);
		
		AccountUser user = source.getAccountUser();
		if (user != null) {
			OnNotifyChangeListener listener = user.getOnNotifyChangeListener();
			if (listener != null) listener.onNotifyChanged(user, count);
		}
	}
	
	@Override
	public void onDataChanged(AccountUser user, AccountUser.DataType type, 
			AccountUser.DataState state) {
	}
	
	public void onMenuOpened(IFragment fragment) {}
	
}
