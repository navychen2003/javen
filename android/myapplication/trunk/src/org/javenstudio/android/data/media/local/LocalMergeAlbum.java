package org.javenstudio.android.data.media.local;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import android.net.Uri;
import android.provider.MediaStore;

import org.javenstudio.android.data.DataApp;
import org.javenstudio.android.data.DataException;
import org.javenstudio.android.data.DataPath;
import org.javenstudio.android.data.ReloadCallback;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.android.data.media.MediaItem;
import org.javenstudio.android.data.media.MediaSet;
import org.javenstudio.cocoka.util.ApiHelper;
import org.javenstudio.cocoka.util.ContentListener;

final class LocalMergeAlbum extends LocalMediaSet 
		implements ContentListener {

    private static final int PAGE_SIZE = 64;

    // mIndex maps global position to the position of each underlying media sets.
    private TreeMap<Integer, int[]> mIndex = new TreeMap<Integer, int[]>();
    
    private final Comparator<MediaItem> mComparator;
    private final MediaSet[] mSources;

    private final LocalMediaSource mSource;
    private String mName;
    private FetchCache[] mFetcher;
    private int mSupportedOperation;
    private int mBucketId;
	
	public LocalMergeAlbum(LocalMediaSource source, DataPath path, 
			Comparator<MediaItem> comparator, MediaSet[] sources, int bucketId) { 
		super(path, INVALID_DATA_VERSION);
		mSource = source;
		mComparator = comparator;
        mSources = sources;
        mName = sources.length == 0 ? "" : sources[0].getName();
        mBucketId = bucketId;
        
        //for (MediaSet set : mSources) {
        //    set.addContentListener(this);
        //}
        //reloadData(ReloadType.DEFAULT);
	}

	@Override
	public DataApp getDataApp() { 
		return mSource.getDataApp();
	}
	
    //public boolean isCameraRoll() {
    //    if (mSources.length == 0) return false;
    //    for (MediaSet set : mSources) {
    //        if (!set.isCameraRoll()) return false;
    //    }
    //    return true;
    //}

    private void updateData() {
        //ArrayList<MediaSet> matches = new ArrayList<MediaSet>();
        int supported = mSources.length == 0 ? 0 : MediaItem.SUPPORT_ALL;
        mFetcher = new FetchCache[mSources.length];
        for (int i = 0, n = mSources.length; i < n; ++i) {
            mFetcher[i] = new FetchCache(mSources[i]);
            supported &= mSources[i].getSupportedOperations();
        }
        mSupportedOperation = supported;
        mIndex.clear();
        mIndex.put(0, new int[mSources.length]);
        mName = mSources.length == 0 ? "" : mSources[0].getName();
    }

    private void invalidateCache() {
        for (int i = 0, n = mSources.length; i < n; i++) {
            mFetcher[i].invalidate();
        }
        mIndex.clear();
        mIndex.put(0, new int[mSources.length]);
    }

    public Uri getContentUri() {
        String bucketId = String.valueOf(mBucketId);
        if (ApiHelper.HAS_MEDIA_PROVIDER_FILES_TABLE) {
            return MediaStore.Files.getContentUri("external").buildUpon()
                    .appendQueryParameter(LocalMediaSource.KEY_BUCKET_ID, bucketId)
                    .build();
        } else {
            // We don't have a single URL for a merged image before ICS
            // So we used the image's URL as a substitute.
            return MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon()
                    .appendQueryParameter(LocalMediaSource.KEY_BUCKET_ID, bucketId)
                    .build();
        }
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public int getItemCount() {
        return getTotalItemCount();
    }

    @Override
    public List<MediaItem> getItemList(int start, int count) {
        // First find the nearest mark position <= start.
        SortedMap<Integer, int[]> head = mIndex.headMap(start + 1);
        int markPos = head.lastKey();
        int[] subPos = head.get(markPos).clone();
        MediaItem[] slot = new MediaItem[mSources.length];

        int size = mSources.length;

        // fill all slots
        for (int i = 0; i < size; i++) {
            slot[i] = mFetcher[i].getItem(subPos[i]);
        }

        ArrayList<MediaItem> result = new ArrayList<MediaItem>();

        for (int i = markPos; i < start + count; i++) {
            int k = -1;  // k points to the best slot up to now.
            for (int j = 0; j < size; j++) {
                if (slot[j] != null) {
                    if (k == -1 || mComparator.compare(slot[j], slot[k]) < 0) 
                        k = j;
                }
            }

            // If we don't have anything, all streams are exhausted.
            if (k == -1) break;

            // Pick the best slot and refill it.
            subPos[k]++;
            if (i >= start) 
                result.add(slot[k]);
            
            slot[k] = mFetcher[k].getItem(subPos[k]);

            // Periodically leave a mark in the index, so we can come back later.
            if ((i + 1) % PAGE_SIZE == 0) {
                mIndex.put(i + 1, subPos.clone());
            }
        }

        return result;
    }

    @Override
    public int getTotalItemCount() {
        int count = 0;
        for (MediaSet set : mSources) {
            count += set.getTotalItemCount();
        }
        return count;
    }

    @Override
    public long reloadData(ReloadCallback callback, ReloadType type) {
        boolean changed = false;
        for (int i = 0, n = mSources.length; i < n; ++i) {
            if (mSources[i].reloadData(callback, type) > mDataVersion) 
            	changed = true;
        }
        if (changed) {
            mDataVersion = nextVersionNumber();
            updateData();
            invalidateCache();
        }
        return mDataVersion;
    }

    @Override
    public void onContentDirty() {
    //    notifyContentChanged();
    }

    @Override
    public int getSupportedOperations() {
        return mSupportedOperation;
    }

    @Override
    public boolean delete() throws DataException {
    	int count = 0;
        for (MediaSet set : mSources) {
            if (set.delete()) count ++;
        }
        return count > 0;
    }

    @Override
    public void rotate(int degrees) {
        for (MediaSet set : mSources) {
            set.rotate(degrees);
        }
    }

    private static class FetchCache {
        private MediaSet mBaseSet;
        private SoftReference<List<MediaItem>> mCacheRef;
        private int mStartPos;

        public FetchCache(MediaSet baseSet) {
            mBaseSet = baseSet;
        }

        public void invalidate() {
            mCacheRef = null;
        }

        public MediaItem getItem(int index) {
            boolean needLoading = false;
            List<MediaItem> cache = null;
            if (mCacheRef == null
                    || index < mStartPos || index >= mStartPos + PAGE_SIZE) {
                needLoading = true;
            } else {
                cache = mCacheRef.get();
                if (cache == null) {
                    needLoading = true;
                }
            }

            if (needLoading) {
                cache = mBaseSet.getItemList(index, PAGE_SIZE);
                mCacheRef = new SoftReference<List<MediaItem>>(cache);
                mStartPos = index;
            }

            if (index < mStartPos || index >= mStartPos + cache.size()) {
                return null;
            }

            return cache.get(index - mStartPos);
        }
    }

    public boolean isLeafAlbum() {
        return true;
    }
	
}
