package org.javenstudio.provider.publish.discuss;

import org.javenstudio.android.data.ReloadType;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.provider.ProviderBase;
import org.javenstudio.provider.ProviderBinder;
import org.javenstudio.provider.ProviderCallback;

public class TopicProvider extends ProviderBase {

	private final TopicBinder mBinder;
	private final TopicDataSets mDataSets;
	
	private OnTopicClickListener mItemClickListener = null;
	private OnTopicClickListener mViewClickListener = null;
	private OnTopicClickListener mUserClickListener = null;
	
	public TopicProvider(String name, int iconRes) { 
		this(name, iconRes, new TopicFactory());
	}
	
	public TopicProvider(String name, int iconRes, 
			TopicFactory factory) { 
		super(name, iconRes);
		mBinder = factory.createTopicBinder(this);
		mDataSets = factory.createTopicDataSets(this);
	}

	public TopicDataSets getTopicDataSets() { return mDataSets; }
	
	public void setOnTopicItemClickListener(OnTopicClickListener l) { mItemClickListener = l; }
	public OnTopicClickListener getOnTopicItemClickListener() { return mItemClickListener; }
	
	public void setOnTopicViewClickListener(OnTopicClickListener l) { mViewClickListener = l; }
	public OnTopicClickListener getOnTopicViewClickListener() { return mViewClickListener; }
	
	public void setOnTopicUserClickListener(OnTopicClickListener l) { mUserClickListener = l; }
	public OnTopicClickListener getOnTopicUserClickListener() { return mUserClickListener; }
	
	@Override
	public ProviderBinder getBinder() {
		return mBinder;
	}

	@Override
	public void reloadOnThread(ProviderCallback callback, 
			ReloadType type, long reloadId) {}
	
	protected void postClearDataSets() { 
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					getTopicDataSets().clear();
				}
			});
	}
	
	protected void postAddTopicItem(final ProviderCallback callback, 
			final TopicItem item) { 
		if (callback == null || item == null) 
			return;
		
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					getTopicDataSets().addTopicItem(item, false);
					callback.getController().getModel().callbackOnDataSetUpdate(item); 
				}
			});
	}
	
	protected void postNotifyChanged() { 
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					getTopicDataSets().notifyContentChanged(true);
					getTopicDataSets().notifyDataSetChanged();
				}
			});
	}
	
}
