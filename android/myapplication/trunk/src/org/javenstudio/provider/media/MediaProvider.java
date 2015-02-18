package org.javenstudio.provider.media;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;

import org.javenstudio.android.app.AddAlbumOperation;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.cocoka.app.ActionItem;
import org.javenstudio.provider.ProviderBase;
import org.javenstudio.provider.ProviderBinder;
import org.javenstudio.provider.ProviderCallback;

public class MediaProvider extends ProviderBase 
		implements AddAlbumOperation.ClickListener {

	protected final List<IMediaSource> mSources;
	protected IMediaSource mSelectItem = null;
	
	public MediaProvider(String name, int iconRes) { 
		super(name, iconRes);
		mSources = new ArrayList<IMediaSource>();
	}
	
	public final void addSource(IMediaSource source) { 
		if (source != null) {
			synchronized (mSources) {
				for (IMediaSource item : mSources) { 
					if (item == source) return;
				}
				
				mSources.add(source);
			}
		}
	}
	
	public final IMediaSource[] getSources() { 
		synchronized (mSources) {
			return mSources.toArray(new IMediaSource[mSources.size()]);
		}
	}
	
	@Override
	public CharSequence getTitle() { 
		return null;
	}
	
	public CharSequence getDefaultTitle() { 
		return super.getTitle();
	}
	
	@Override
	public CharSequence getSubTitle() { 
		return null;
	}
	
	public CharSequence getDefaultSubTitle() { 
		return super.getSubTitle();
	}
	
	@Override
	public void onAddAlbumClick(Activity activity) { 
		IMediaSource item = mSelectItem;
		if (item != null) 
			item.onCreateAlbum(activity);
	}
	
	@Override
	public final ProviderBinder getBinder() { 
		IMediaSource item = mSelectItem;
		if (item != null)
			return item.getBinder(); 
		
		synchronized (mSources) {
			if (mSources.size() == 1) 
				return mSources.get(0).getBinder();
		}
		
		return null;
	}
	
	@Override
	public MediaActionItem[] getActionItems(IActivity activity) { 
		synchronized (mSources) {
			return createActionItems(activity);
		}
	}
	
	protected MediaActionItem[] createActionItems(IActivity activity) {
		final MediaActionItem[] items = new MediaActionItem[mSources.size()];
		IMediaSource defaultItem = null;
		
		for (int i=0; i < items.length; i++) { 
			final IMediaSource item = mSources.get(i);
			final int iconRes = item.getIconRes();
			
			final MediaActionItem actionItem = new MediaActionItem(
					item.getName(), iconRes, null, null, item);
			
			actionItem.setTitle(item.getTitle(activity));
			actionItem.setSubTitle(item.getSubTitle(activity));
			
			items[i] = actionItem;
			
			if (item.isDefault()) 
				defaultItem = item;
		}
		
		if (mSelectItem == null && defaultItem != null) 
			mSelectItem = defaultItem;
		
		return items; 
	}
	
	@Override
	public boolean onActionItemSelected(IActivity activity, int position, long id) { 
		ActionItem item = activity.getActionHelper().getActionItem(position);
		
		if (item != null && item instanceof MediaActionItem) { 
			MediaActionItem actionItem = (MediaActionItem)item;
			IMediaSource sourceItem = actionItem.getSourceItem();
			
			boolean changed = mSelectItem != sourceItem;
			mSelectItem = sourceItem;
			
			if (changed) activity.setContentFragment(); 
		}
		
		return true; 
	}
	
	@Override
	public synchronized void reloadOnThread(ProviderCallback callback, 
			ReloadType type, long reloadId) { 
		final IMediaSource selectItem = mSelectItem;
		if (selectItem != null) 
			selectItem.reloadData(callback, type, reloadId);
	}
	
}
