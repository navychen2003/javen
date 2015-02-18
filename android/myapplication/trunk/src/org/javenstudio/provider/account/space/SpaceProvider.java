package org.javenstudio.provider.account.space;

import org.javenstudio.android.data.ReloadType;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.provider.ProviderBase;
import org.javenstudio.provider.ProviderCallback;

public abstract class SpaceProvider extends ProviderBase {

	private final SpaceBinder mBinder;
	private final SpaceDataSets mDataSets;
	
	private OnSpaceClickListener mItemClickListener = null;
	private OnSpaceClickListener mViewClickListener = null;
	private OnSpaceClickListener mUserClickListener = null;
	
	public SpaceProvider(String name, int iconRes, 
			SpaceFactory factory) { 
		super(name, iconRes);
		mBinder = factory.createSpaceBinder(this);
		mDataSets = factory.createSpaceDataSets(this);
	}

	public SpaceDataSets getSpaceDataSets() { return mDataSets; }
	
	public void setOnSpaceItemClickListener(OnSpaceClickListener l) { mItemClickListener = l; }
	public OnSpaceClickListener getOnSpaceItemClickListener() { return mItemClickListener; }
	
	public void setOnSpaceViewClickListener(OnSpaceClickListener l) { mViewClickListener = l; }
	public OnSpaceClickListener getOnSpaceViewClickListener() { return mViewClickListener; }
	
	public void setOnSpaceUserClickListener(OnSpaceClickListener l) { mUserClickListener = l; }
	public OnSpaceClickListener getOnSpaceUserClickListener() { return mUserClickListener; }
	
	@Override
	public SpaceBinder getBinder() {
		return mBinder;
	}

	@Override
	public boolean isContentProgressEnabled() { 
		return getSpaceDataSets().getCount() == 0; 
	}
	
	@Override
	public void reloadOnThread(ProviderCallback callback, 
			ReloadType type, long reloadId) { 
	}
	
	protected void postClearDataSets(final ProviderCallback callback) { 
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					getSpaceDataSets().clear();
					callback.getController().getModel().callbackOnDataSetClear(getSpaceDataSets()); 
				}
			});
	}
	
	protected void postAddSpaceItem(final ProviderCallback callback, 
			final SpaceItem... item) { 
		if (callback == null || item == null) 
			return;
		
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					getSpaceDataSets().addSpaceItem(item);
					callback.getController().getModel().callbackOnDataSetUpdate(item); 
				}
			});
	}
	
	protected void postNotifyChanged(final ProviderCallback callback) { 
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					getSpaceDataSets().notifyContentChanged(true);
					getSpaceDataSets().notifyDataSetChanged();
					callback.getController().getModel().callbackOnDataSetChanged(getSpaceDataSets());
				}
			});
	}
	
}
