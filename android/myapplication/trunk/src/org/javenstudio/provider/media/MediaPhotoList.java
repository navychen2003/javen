package org.javenstudio.provider.media;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.javenstudio.android.data.ReloadCallback;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.android.data.media.MediaItem;
import org.javenstudio.android.data.media.MediaSet;

public class MediaPhotoList {
	private static final int BATCH_SIZE = 20;
	
	private final List<MediaItem> mMediaItemList = new ArrayList<MediaItem>();
	private final Set<MediaItem> mMediaItemSet = new TreeSet<MediaItem>();
	
	private final MediaSet[] mTopSets;
	private int mMediaItemIndex = 0;
	
	public MediaPhotoList(MediaSet mediaSet) { 
		this(new MediaSet[] { mediaSet });
	}
	
	public MediaPhotoList(MediaSet[] mediaSets) { 
		mTopSets = mediaSets;
	}
	
	public synchronized int getPhotoCount() { return mMediaItemList.size(); }
	
	public synchronized MediaItem getPhotoAt(int index) { 
		return index >= 0 && index < mMediaItemList.size() ? mMediaItemList.get(index) : null;
	}
	
	public synchronized boolean hasNext() { 
		return mMediaItemIndex >= 0 && mMediaItemIndex < mMediaItemList.size();
	}
	
	public synchronized MediaItem[] nextPhotos(ReloadCallback callback, 
			ReloadType type) { 
		List<MediaItem> sets = new ArrayList<MediaItem>();
		
		final int indexTo = mMediaItemIndex + BATCH_SIZE;
		if (indexTo >= mMediaItemList.size())
			loadMediaItems(callback, type, indexTo);
		
		if (mMediaItemIndex >= 0 && mMediaItemIndex < mMediaItemList.size()) { 
			for (; mMediaItemIndex < indexTo && mMediaItemIndex < mMediaItemList.size(); mMediaItemIndex++) { 
				MediaItem set = mMediaItemList.get(mMediaItemIndex);
				sets.add(set);
			}
		}
		
		return sets.toArray(new MediaItem[sets.size()]);
	}
	
	private void loadMediaItems(ReloadCallback callback, ReloadType type, int size) { 
		for (int i=0; mTopSets != null && i < mTopSets.length; i++) { 
			MediaSet set = mTopSets[i];
			if (set == null) continue;
			
			set.reloadData(callback, type);
			addMediaItem(set);
			addSubMediaItems(set, callback, type, size);
			
			if (mMediaItemList.size() > size) 
				break;
		}
	}
	
	private void addMediaItem(MediaSet mediaSet) { 
		if (mediaSet == null) return;
		List<MediaItem> items = mediaSet.getItemList(0, mediaSet.getItemCount());
		if (items != null) { 
			for (MediaItem item : items) { 
				addMediaItem(item);
			}
		}
	}
	
	private void addMediaItem(MediaItem mediaItem) { 
		if (mediaItem != null && !mMediaItemSet.contains(mediaItem)) {
			mMediaItemList.add(mediaItem);
			mMediaItemSet.add(mediaItem);
		}
	}
	
	private void addSubMediaItems(MediaSet mediaSet, ReloadCallback callback, 
			ReloadType type, int size) {	
		for (int i=0; i < mediaSet.getSubSetCount(); i++) { 
			MediaSet set = mediaSet.getSubSetAt(i);
			if (set == null) continue;
			
			set.reloadData(callback, type);
			addMediaItem(set);
			addSubMediaItems(set, callback, type, size);
			
			if (mMediaItemList.size() > size) 
				break;
		}
	}
	
}
