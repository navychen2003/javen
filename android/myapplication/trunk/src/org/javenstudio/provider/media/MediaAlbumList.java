package org.javenstudio.provider.media;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.javenstudio.android.data.ReloadCallback;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.android.data.media.MediaSet;

public class MediaAlbumList {
	private static final int BATCH_SIZE = 20;
	
	private final List<MediaSet> mMediaSetList = new ArrayList<MediaSet>();
	private final Set<MediaSet> mMediaSetSet = new TreeSet<MediaSet>();
	
	private final MediaSet[] mTopSets;
	private int mMediaSetIndex = 0;
	
	public MediaAlbumList(MediaSet set) { 
		this(new MediaSet[] { set });
	}
	
	public MediaAlbumList(MediaSet[] topSets) { 
		mTopSets = topSets;
	}
	
	public synchronized int getAlbumCount() { return mMediaSetList.size(); }
	
	public synchronized MediaSet getAlbumAt(int index) { 
		return index >= 0 && index < mMediaSetList.size() ? mMediaSetList.get(index) : null;
	}
	
	@SuppressWarnings("unused")
	private int countMediaSetImages(MediaSet mediaSet) { 
		if (mediaSet == null) return 0;
		
		int count = mediaSet.getItemCount();
		
		for (int i=0; i < mediaSet.getSubSetCount(); i++) { 
			MediaSet set = mediaSet.getSubSetAt(i);
			count += countMediaSetImages(set);
		}
		
		return count;
	}
	
	public synchronized boolean hasNext() { 
		return mMediaSetIndex >= 0 && mMediaSetIndex < mMediaSetList.size();
	}
	
	public synchronized MediaSet[] nextAlbums(ReloadCallback callback, 
			ReloadType type) { 
		List<MediaSet> sets = new ArrayList<MediaSet>();
		
		final int indexTo = mMediaSetIndex + BATCH_SIZE;
		if (indexTo >= mMediaSetList.size())
			loadMediaSets(callback, type, indexTo);
		
		if (mMediaSetIndex >= 0 && mMediaSetIndex < mMediaSetList.size()) { 
			for (; mMediaSetIndex < indexTo && mMediaSetIndex < mMediaSetList.size(); mMediaSetIndex++) { 
				MediaSet set = mMediaSetList.get(mMediaSetIndex);
				sets.add(set);
			}
		}
		
		return sets.toArray(new MediaSet[sets.size()]);
	}
	
	private void loadMediaSets(ReloadCallback callback, ReloadType type, int size) { 
		for (int i=0; mTopSets != null && i < mTopSets.length; i++) { 
			MediaSet set = mTopSets[i];
			if (set == null) continue;
			
			set.reloadData(callback, type);
			addMediaSet(set);
			addSubMediaSets(set, callback, type, size);
			
			if (mMediaSetList.size() > size) 
				break;
		}
	}
	
	private void addMediaSet(MediaSet mediaSet) { 
		if (mediaSet != null && !mMediaSetSet.contains(mediaSet)) {
			mMediaSetList.add(mediaSet);
			mMediaSetSet.add(mediaSet);
		}
	}
	
	private void addSubMediaSets(MediaSet mediaSet, ReloadCallback callback, 
			ReloadType type, int size) {	
		for (int i=0; i < mediaSet.getSubSetCount(); i++) { 
			MediaSet set = mediaSet.getSubSetAt(i);
			if (set == null) continue;
			
			set.reloadData(callback, type);
			addMediaSet(set);
			addSubMediaSets(set, callback, type, size);
			
			if (mMediaSetList.size() > size) 
				break;
		}
	}
	
}
