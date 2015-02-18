package org.javenstudio.provider.publish.discuss;

import org.javenstudio.android.data.ReloadType;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.provider.ProviderBase;
import org.javenstudio.provider.ProviderBinder;
import org.javenstudio.provider.ProviderCallback;

public class ReplyProvider extends ProviderBase {

	private final ReplyBinder mBinder;
	private final ReplyDataSets mDataSets;
	
	private OnReplyClickListener mItemClickListener = null;
	private OnReplyClickListener mViewClickListener = null;
	private OnReplyClickListener mUserClickListener = null;
	
	public ReplyProvider(String name, int iconRes) { 
		this(name, iconRes, new ReplyFactory());
	}
	
	public ReplyProvider(String name, int iconRes, 
			ReplyFactory factory) { 
		super(name, iconRes);
		mBinder = factory.createReplyBinder(this);
		mDataSets = factory.createReplyDataSets(this);
	}

	public ReplyDataSets getReplyDataSets() { return mDataSets; }
	
	public void setOnItemClickListener(OnReplyClickListener l) { mItemClickListener = l; }
	public OnReplyClickListener getOnItemClickListener() { return mItemClickListener; }
	
	public void setOnViewClickListener(OnReplyClickListener l) { mViewClickListener = l; }
	public OnReplyClickListener getOnViewClickListener() { return mViewClickListener; }
	
	public void setOnUserClickListener(OnReplyClickListener l) { mUserClickListener = l; }
	public OnReplyClickListener getOnUserClickListener() { return mUserClickListener; }
	
	@Override
	public ProviderBinder getBinder() {
		return mBinder;
	}

	@Override
	public void reloadOnThread(ProviderCallback callback, 
			ReloadType type, long reloadId) { 
		// do nothing
	}
	
	protected void postClearDataSets() { 
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					getReplyDataSets().clear();
				}
			});
	}
	
	protected void postAddReplyItem(final ProviderCallback callback, 
			final ReplyItem item) { 
		if (callback == null || item == null) 
			return;
		
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					getReplyDataSets().addReplyItem(item, false);
					callback.getController().getModel().callbackOnDataSetUpdate(item); 
				}
			});
	}
	
	protected void postNotifyChanged() { 
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					getReplyDataSets().notifyContentChanged(true);
					getReplyDataSets().notifyDataSetChanged();
				}
			});
	}
	
}
