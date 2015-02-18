package org.javenstudio.cocoka.view;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import android.graphics.BitmapRegionDecoder;
import android.os.Handler;
import android.os.Message;

import org.javenstudio.cocoka.data.IMediaItem;
import org.javenstudio.cocoka.data.IMediaObject;
import org.javenstudio.cocoka.data.IMediaSet;
import org.javenstudio.cocoka.graphics.BitmapUtil;
import org.javenstudio.cocoka.opengl.GLActivity;
import org.javenstudio.cocoka.opengl.ScreenNail;
import org.javenstudio.cocoka.opengl.SynchronizedHandler;
import org.javenstudio.cocoka.opengl.TileImageViewAdapter;
import org.javenstudio.cocoka.opengl.TiledScreenNail;
import org.javenstudio.cocoka.opengl.TiledTexture;
import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapPool;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.cocoka.util.ContentListener;
import org.javenstudio.cocoka.util.Utils;
import org.javenstudio.cocoka.worker.job.Future;
import org.javenstudio.cocoka.worker.job.FutureListener;
import org.javenstudio.cocoka.worker.job.Job;
import org.javenstudio.cocoka.worker.job.JobContext;
import org.javenstudio.cocoka.worker.job.JobSubmit;
import org.javenstudio.common.util.Logger;

public class PhotoAdapter implements PhotoPage.Model {
	private static final Logger LOG = Logger.getLogger(PhotoAdapter.class);

    private static final int MSG_LOAD_START = 1;
    private static final int MSG_LOAD_FINISH = 2;
    private static final int MSG_RUN_OBJECT = 3;
    private static final int MSG_UPDATE_IMAGE_REQUESTS = 4;

    private static final int MIN_LOAD_COUNT = 16;
    private static final int DATA_CACHE_SIZE = 256;
    private static final int SCREEN_NAIL_MAX = PhotoView.SCREEN_NAIL_MAX;
    private static final int IMAGE_CACHE_SIZE = 2 * SCREEN_NAIL_MAX + 1;

    private static final int BIT_SCREEN_NAIL = 1;
    private static final int BIT_FULL_IMAGE = 2;

    // sImageFetchSeq is the fetching sequence for images.
    // We want to fetch the current screennail first (offset = 0), the next
    // screennail (offset = +1), then the previous screennail (offset = -1) etc.
    // After all the screennail are fetched, we fetch the full images (only some
    // of them because of we don't want to use too much memory).
    private static ImageFetch[] sImageFetchSeq;

    private static class ImageFetch {
        int indexOffset;
        int imageBit;
        public ImageFetch(int offset, int bit) {
            indexOffset = offset;
            imageBit = bit;
        }
    }

    static {
        int k = 0;
        sImageFetchSeq = new ImageFetch[1 + (IMAGE_CACHE_SIZE - 1) * 2 + 3];
        sImageFetchSeq[k++] = new ImageFetch(0, BIT_SCREEN_NAIL);

        for (int i = 1; i < IMAGE_CACHE_SIZE; ++i) {
            sImageFetchSeq[k++] = new ImageFetch(i, BIT_SCREEN_NAIL);
            sImageFetchSeq[k++] = new ImageFetch(-i, BIT_SCREEN_NAIL);
        }

        sImageFetchSeq[k++] = new ImageFetch(0, BIT_FULL_IMAGE);
        sImageFetchSeq[k++] = new ImageFetch(1, BIT_FULL_IMAGE);
        sImageFetchSeq[k++] = new ImageFetch(-1, BIT_FULL_IMAGE);
    }

    private final TileImageViewAdapter mTileProvider;

    // PhotoDataAdapter caches MediaItems (data) and ImageEntries (image).
    //
    // The MediaItems are stored in the mData array, which has DATA_CACHE_SIZE
    // entries. The valid index range are [mContentStart, mContentEnd). We keep
    // mContentEnd - mContentStart <= DATA_CACHE_SIZE, so we can use
    // (i % DATA_CACHE_SIZE) as index to the array.
    //
    // The valid MediaItem window size (mContentEnd - mContentStart) may be
    // smaller than DATA_CACHE_SIZE because we only update the window and reload
    // the MediaItems when there are significant changes to the window position
    // (>= MIN_LOAD_COUNT).
    private final IMediaItem mDataCache[] = new IMediaItem[DATA_CACHE_SIZE];
    private int mContentStart = 0;
    private int mContentEnd = 0;

    // The ImageCache is a Path-to-ImageEntry map. It only holds the
    // ImageEntries in the range of [mActiveStart, mActiveEnd).  We also keep
    // mActiveEnd - mActiveStart <= IMAGE_CACHE_SIZE.  Besides, the
    // [mActiveStart, mActiveEnd) range must be contained within
    // the [mContentStart, mContentEnd) range.
    private HashMap<IMediaItem, ImageEntry> mImageCache =
            new HashMap<IMediaItem, ImageEntry>();
    private int mActiveStart = 0;
    private int mActiveEnd = 0;

    // mCurrentIndex is the "center" image the user is viewing. The change of
    // mCurrentIndex triggers the data loading and image loading.
    private int mCurrentIndex;

    // mChanges keeps the version number (of MediaItem) about the images. If any
    // of the version number changes, we notify the view. This is used after a
    // database reload or mCurrentIndex changes.
    private final long mChanges[] = new long[IMAGE_CACHE_SIZE];
    // mPaths keeps the corresponding Path (of MediaItem) for the images. This
    // is used to determine the item movement.
    private final IMediaItem mCacheItems[] = new IMediaItem[IMAGE_CACHE_SIZE];

    private final Handler mMainHandler;

    private final PhotoPage mPhotoPage;
    private final PhotoView mPhotoView;
    private final IMediaSet mSource;
    private ReloadTask mReloadTask;

    private long mSourceVersion = IMediaObject.INVALID_DATA_VERSION;
    private int mSize = 0;
    private IMediaItem mMediaItem;
    private int mCameraIndex;
    private boolean mIsStaticCamera;
    private boolean mIsActive;
    private boolean mNeedFullImage;
    private int mFocusHintDirection = FOCUS_HINT_NEXT;
    private IMediaItem mFocusHintItem = null;

    public interface DataListener extends LoadingListener {
        public void onPhotoChanged(int index, IMediaItem item);
    }

    private DataListener mDataListener;
    private volatile int mDataRequestCount = 0;

    private final SourceListener mSourceListener = new SourceListener();
    private final TiledTexture.Uploader mUploader;

    // The path of the current viewing item will be stored in mMediaItem.
    // If mMediaItem is not null, mCurrentIndex is only a hint for where we
    // can find the item. If mMediaItem is null, then we use the mCurrentIndex to
    // find the image being viewed. cameraIndex is the index of the camera
    // preview. If cameraIndex < 0, there is no camera preview.
    public PhotoAdapter(GLActivity activity, PhotoPage page, PhotoView view,
            IMediaSet mediaSet, IMediaItem item, int indexHint, int cameraIndex,
            boolean isStaticCamera) {
        mSource = Utils.checkNotNull(mediaSet);
        mPhotoPage = Utils.checkNotNull(page);
        mPhotoView = Utils.checkNotNull(view);
        mTileProvider = new TileImageViewAdapter(page);
        mMediaItem = Utils.checkNotNull(item);
        mCurrentIndex = indexHint;
        mCameraIndex = cameraIndex;
        mIsStaticCamera = isStaticCamera;
        mNeedFullImage = true;

        Arrays.fill(mChanges, IMediaObject.INVALID_DATA_VERSION);
        mUploader = new TiledTexture.Uploader(activity.getGLRoot());

        mMainHandler = new SynchronizedHandler(activity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_RUN_OBJECT:
                        ((Runnable) message.obj).run();
                        return;
                    case MSG_LOAD_START: {
                        if (mDataListener != null) 
                            mDataListener.onLoadingStarted();
                        return;
                    }
                    case MSG_LOAD_FINISH: {
                        if (mDataListener != null) 
                            mDataListener.onLoadingFinished(false);
                        return;
                    }
                    case MSG_UPDATE_IMAGE_REQUESTS: {
                        updateImageRequests();
                        return;
                    }
                    default: throw new AssertionError();
                }
            }
        };

        updateSlidingWindow();
    }

    private BitmapHolder getBitmapHolder() { 
    	return mPhotoPage;
    }
    
    private IMediaItem getItemInternal(int index) {
        if (index < 0 || index >= mSize) return null;
        if (index >= mContentStart && index < mContentEnd) 
            return mDataCache[index % DATA_CACHE_SIZE];
        
        return null;
    }

    private long getVersionAt(int index) {
        IMediaItem item = getItemInternal(index);
        if (item == null) return IMediaObject.INVALID_DATA_VERSION;
        return item.getVersion();
    }

    private IMediaItem getItemAt(int index) { 
    	return getItemInternal(index);
    }
    
    private void fireDataChange() {
        // First check if data actually changed.
        boolean changed = false;
        for (int i = -SCREEN_NAIL_MAX; i <= SCREEN_NAIL_MAX; ++i) {
            long newVersion = getVersionAt(mCurrentIndex + i);
            if (mChanges[i + SCREEN_NAIL_MAX] != newVersion) {
                mChanges[i + SCREEN_NAIL_MAX] = newVersion;
                changed = true;
            }
        }

        if (!changed) return;

        // Now calculate the fromIndex array. fromIndex represents the item
        // movement. It records the index where the picture come from. The
        // special value Integer.MAX_VALUE means it's a new picture.
        final int N = IMAGE_CACHE_SIZE;
        int fromIndex[] = new int[N];

        // Remember the old path array.
        IMediaItem oldItems[] = new IMediaItem[N];
        System.arraycopy(mCacheItems, 0, oldItems, 0, N);

        // Update the mPaths array.
        for (int i = 0; i < N; ++i) {
            mCacheItems[i] = getItemAt(mCurrentIndex + i - SCREEN_NAIL_MAX);
        }

        // Calculate the fromIndex array.
        for (int i = 0; i < N; i++) {
            IMediaItem p = mCacheItems[i];
            if (p == null) {
                fromIndex[i] = Integer.MAX_VALUE;
                continue;
            }

            // Try to find the same path in the old array
            int j;
            for (j = 0; j < N; j++) {
                if (oldItems[j] == p) {
                    break;
                }
            }
            fromIndex[i] = (j < N) ? j - SCREEN_NAIL_MAX : Integer.MAX_VALUE;
        }

        mPhotoView.notifyDataChange(fromIndex, -mCurrentIndex,
                mSize - 1 - mCurrentIndex);
    }

    public void setDataListener(DataListener listener) {
        mDataListener = listener;
    }

    private void updateScreenNail(IMediaItem item, Future<ScreenNail> future) {
        ImageEntry entry = mImageCache.get(item);
        ScreenNail screenNail = future.get();

        if (entry == null || entry.screenNailTask != future) {
            if (screenNail != null) screenNail.recycle();
            return;
        }

        entry.screenNailTask = null;

        // Combine the ScreenNails if we already have a BitmapScreenNail
        if (entry.screenNail instanceof TiledScreenNail) {
            TiledScreenNail original = (TiledScreenNail) entry.screenNail;
            screenNail = original.combine(screenNail);
        }

        if (screenNail == null) {
            entry.failToLoadScreenNail = true;
        } else {
            entry.failToLoadScreenNail = false;
            entry.screenNail = screenNail;
        }

        if (LOG.isDebugEnabled()) {
        	LOG.debug("updateScreenNail: item=" + item 
        			+ " screenNail=" + entry.screenNail 
        			+ " failedLoad=" + entry.failToLoadScreenNail);
        }
        
        for (int i = -SCREEN_NAIL_MAX; i <= SCREEN_NAIL_MAX; ++i) {
            if (item == getItemAt(mCurrentIndex + i)) {
            	if (LOG.isDebugEnabled()) {
            		LOG.debug("updateScreenNail: notifyImageChange: " + i 
            				+ " current=" + mCurrentIndex);
            	}
            	
                if (i == 0) updateTileProvider(entry);
                mPhotoView.notifyImageChange(i);
                mPhotoPage.refreshBottomControlsWhenReady();
                break;
            }
        }
        
        updateImageRequests();
        updateScreenNailUploadQueue();
    }

    private void updateFullImage(IMediaItem item, Future<BitmapRegionDecoder> future) {
        ImageEntry entry = mImageCache.get(item);
        if (entry == null || entry.fullImageTask != future) {
            BitmapRegionDecoder fullImage = future.get();
            if (fullImage != null) fullImage.recycle();
            return;
        }

        entry.fullImageTask = null;
        entry.fullImage = future.get();
        
        if (entry.fullImage == null) {
        	entry.failToLoadFullImage = true;
        } else {
        	entry.failToLoadFullImage = false;
        }
        
        if (LOG.isDebugEnabled()) {
        	LOG.debug("updateFullImage: item=" + item 
        			+ " fullImage=" + entry.fullImage);
        }
        
        if (entry.fullImage != null) {
            if (item == getItemAt(mCurrentIndex)) {
            	if (LOG.isDebugEnabled()) {
            		LOG.debug("updateFullImage: notifyImageChange: " + 0 
            				+ " current=" + mCurrentIndex);
            	}
            	
                updateTileProvider(entry);
                mPhotoView.notifyImageChange(0);
                mPhotoPage.refreshBottomControlsWhenReady();
            }
        }
        
        updateImageRequests();
    }

    @Override
    public void resume() {
        mIsActive = true;
        TiledTexture.prepareResources(getBitmapHolder());

        mSource.addContentListener(mSourceListener);
        updateImageCache();
        updateImageRequests();

        mReloadTask = new ReloadTask();
        mReloadTask.start();

        fireDataChange();
    }

    @Override
    public void pause() {
        mIsActive = false;

        mReloadTask.terminate();
        mReloadTask = null;

        mSource.removeContentListener(mSourceListener);

        for (ImageEntry entry : mImageCache.values()) {
            if (entry.fullImageTask != null) entry.fullImageTask.cancel();
            if (entry.screenNailTask != null) entry.screenNailTask.cancel();
            if (entry.screenNail != null) entry.screenNail.recycle();
        }
        mImageCache.clear();
        mTileProvider.clear();

        mUploader.clear();
        TiledTexture.freeResources();
    }

    private IMediaItem getItem(int index) {
        if (index < 0 || index >= mSize || !mIsActive) return null;
        Utils.assertTrue(index >= mActiveStart && index < mActiveEnd);

        if (index >= mContentStart && index < mContentEnd) 
            return mDataCache[index % DATA_CACHE_SIZE];
        
        return null;
    }

    private void updateCurrentIndex(int index) {
        if (mCurrentIndex == index) return;
        mCurrentIndex = index;
        mSource.setIndexHint(index);
        updateSlidingWindow();

        mMediaItem = mDataCache[index % DATA_CACHE_SIZE];

        if (LOG.isDebugEnabled())
        	LOG.debug("updateCurrentIndex: " + index + " item=" + mMediaItem);
        
        updateImageCache();
        updateImageRequests();
        updateTileProvider();

        if (mDataListener != null) 
            mDataListener.onPhotoChanged(index, mMediaItem);

        fireDataChange();
    }

    private void uploadScreenNail(int offset) {
        int index = mCurrentIndex + offset;
        if (index < mActiveStart || index >= mActiveEnd) return;

        IMediaItem item = getItem(index);
        if (item == null) return;

        ImageEntry e = mImageCache.get(item); 
        if (e == null) return;

        ScreenNail s = e.screenNail;
        if (s instanceof TiledScreenNail) {
            TiledTexture t = ((TiledScreenNail) s).getTexture();
            if (t != null && !t.isReady()) mUploader.addTexture(t);
        }
    }

    private void updateScreenNailUploadQueue() {
        mUploader.clear();
        uploadScreenNail(0);
        for (int i = 1; i < IMAGE_CACHE_SIZE; ++i) {
            uploadScreenNail(i);
            uploadScreenNail(-i);
        }
    }

    @Override
    public void moveTo(int index) {
        updateCurrentIndex(index);
    }

    @Override
    public ScreenNail getScreenNail(int offset) {
        int index = mCurrentIndex + offset;
        if (index < 0 || index >= mSize || !mIsActive) return null;
        Utils.assertTrue(index >= mActiveStart && index < mActiveEnd);

        IMediaItem item = getItem(index);
        if (item == null) return null;

        ImageEntry entry = mImageCache.get(item); 
        if (entry == null) return null;

        // Create a default ScreenNail if the real one is not available yet,
        // except for camera that a black screen is better than a gray tile.
        if (entry.screenNail == null && !isCamera(offset)) {
            entry.screenNail = newPlaceholderScreenNail(item);
            if (offset == 0) updateTileProvider(entry);
        }

        return entry.screenNail;
    }

    @Override
    public void getImageSize(int offset, PhotoView.Size size) {
        IMediaItem item = getItem(mCurrentIndex + offset);
        if (item == null) {
            size.width = 0;
            size.height = 0;
        } else {
            size.width = item.getWidth();
            size.height = item.getHeight();
        }
    }

    @Override
    public int getImageRotation(int offset) {
        IMediaItem item = getItem(mCurrentIndex + offset);
        return (item == null) ? 0 : item.getRotation();
    }

    @Override
    public void setNeedFullImage(boolean enabled) {
        mNeedFullImage = enabled;
        mMainHandler.sendEmptyMessage(MSG_UPDATE_IMAGE_REQUESTS);
    }

    @Override
    public boolean isCamera(int offset) {
        return mCurrentIndex + offset == mCameraIndex;
    }

    @Override
    public boolean isStaticCamera(int offset) {
        return isCamera(offset) && mIsStaticCamera;
    }

    @Override
    public boolean isVideo(int offset) {
        IMediaItem item = getItem(mCurrentIndex + offset);
        return (item == null) ? false : 
        	item.getMediaType() == IMediaItem.MEDIA_TYPE_VIDEO;
    }

    @Override
    public boolean isDeletable(int offset) {
        IMediaItem item = getItem(mCurrentIndex + offset);
        return (item == null) ? false : 
        	(item.getSupportedOperations() & IMediaItem.SUPPORT_DELETE) != 0;
    }

    @Override
    public int getLoadingState(int offset) {
        ImageEntry entry = mImageCache.get(getItemAt(mCurrentIndex + offset));
        if (entry == null) return LOADING_INIT;
        if (entry.failToLoadScreenNail) return LOADING_SCREENNAIL_FAIL;
        if (entry.failToLoadFullImage) return LOADING_FULLIMAGE_FAIL;
        if (entry.screenNail != null) return LOADING_COMPLETE;
        if (entry.fullImage != null) return LOADING_COMPLETE;
        return LOADING_INIT;
    }

    @Override
    public ScreenNail getScreenNail() {
        return getScreenNail(0);
    }

    @Override
    public int getImageHeight() {
        return mTileProvider.getImageHeight();
    }

    @Override
    public int getImageWidth() {
        return mTileProvider.getImageWidth();
    }

    @Override
    public int getLevelCount() {
        return mTileProvider.getLevelCount();
    }

    @Override
    public BitmapRef getTile(int level, int x, int y, int tileSize,
            int borderSize, BitmapPool pool) {
        return mTileProvider.getTile(level, x, y, tileSize, borderSize, pool);
    }

    @Override
    public boolean isEmpty() {
        return mSize == 0;
    }

    @Override
    public int getCurrentIndex() {
        return mCurrentIndex;
    }

    @Override
    public IMediaItem getMediaItem(int offset) {
        int index = mCurrentIndex + offset;
        if (index >= mContentStart && index < mContentEnd) 
            return mDataCache[index % DATA_CACHE_SIZE];
        
        return null;
    }

    @Override
    public void setCurrentPhoto(IMediaItem item, int indexHint) {
        if (mMediaItem == item) return;
        mMediaItem = item;
        mCurrentIndex = indexHint;
        
        updateSlidingWindow();
        updateImageCache();
        fireDataChange();

        // We need to reload content if the path doesn't match.
        IMediaItem firstItem = getMediaItem(0);
        if (firstItem != null && firstItem != item) {
            if (mReloadTask != null) mReloadTask.notifyDirty();
        }
    }

    @Override
    public void setFocusHintDirection(int direction) {
        mFocusHintDirection = direction;
    }

    @Override
    public void setFocusHintItem(IMediaItem path) {
        mFocusHintItem = path;
    }

    private void updateTileProvider() {
        ImageEntry entry = mImageCache.get(getItemAt(mCurrentIndex));
        if (entry == null) { // in loading
            mTileProvider.clear();
        } else {
            updateTileProvider(entry);
        }
    }

    private void updateTileProvider(ImageEntry entry) {
        ScreenNail screenNail = entry.screenNail;
        BitmapRegionDecoder fullImage = entry.fullImage;
        if (screenNail != null) {
            if (fullImage != null) {
                mTileProvider.setScreenNail(screenNail,
                        fullImage.getWidth(), fullImage.getHeight());
                mTileProvider.setRegionDecoder(fullImage);
            } else {
                int width = screenNail.getWidth();
                int height = screenNail.getHeight();
                mTileProvider.setScreenNail(screenNail, width, height);
            }
        } else {
            mTileProvider.clear();
        }
    }

    private void updateSlidingWindow() {
        // 1. Update the image window
        int start = Utils.clamp(mCurrentIndex - IMAGE_CACHE_SIZE / 2,
                0, Math.max(0, mSize - IMAGE_CACHE_SIZE));
        int end = Math.min(mSize, start + IMAGE_CACHE_SIZE);

        if (mActiveStart == start && mActiveEnd == end) 
        	return;

        mActiveStart = start;
        mActiveEnd = end;

        // 2. Update the data window
        start = Utils.clamp(mCurrentIndex - DATA_CACHE_SIZE / 2,
                0, Math.max(0, mSize - DATA_CACHE_SIZE));
        end = Math.min(mSize, start + DATA_CACHE_SIZE);
        
        if (mContentStart > mActiveStart || mContentEnd < mActiveEnd || 
        		Math.abs(start - mContentStart) > MIN_LOAD_COUNT) {
        	
            for (int i = mContentStart; i < mContentEnd; ++i) {
                if (i < start || i >= end) 
                    mDataCache[i % DATA_CACHE_SIZE] = null;
            }
            
            mContentStart = start;
            mContentEnd = end;
            if (mReloadTask != null) mReloadTask.notifyDirty();
        }
        
        if (LOG.isDebugEnabled()) {
        	LOG.debug("updateSlidingWindow: active=[" + mActiveStart + ", " + mActiveEnd 
        			+ "] content=[" + mContentStart + ", " + mContentEnd 
        			+ "] current=" + mCurrentIndex + " size=" + mSize);
        }
    }

    private void updateImageRequests() {
    	updateImageRequests((IMediaItem)null, 0);
    }
    
    private void updateImageRequests(IMediaItem updateItem, int updateWhich) {
        if (!mIsActive) return;

        int currentIndex = mCurrentIndex;
        IMediaItem currentItem = mDataCache[currentIndex % DATA_CACHE_SIZE];
        if (currentItem == null || currentItem != mMediaItem) {
            // current item mismatch - don't request image
            return;
        }

        // 1. Find the most wanted request and start it (if not already started).
        Future<?> task = null;
        if (updateItem != null) {
        	if (LOG.isDebugEnabled()) {
        		LOG.debug("updateImageRequests: updateItem=" + updateItem 
        				+ " currentItem=" + currentItem + " which=" + updateWhich);
        	}
        	if (updateItem == currentItem) {
        		int bit = updateWhich; //BIT_FULL_IMAGE;
        		task = startTaskIfNeeded(currentIndex, bit);
        	}
        	if (task == null) return;
        	
        } else {
	        for (int i = 0; i < sImageFetchSeq.length; i++) {
	            int offset = sImageFetchSeq[i].indexOffset;
	            int bit = sImageFetchSeq[i].imageBit;
	            if (bit == BIT_FULL_IMAGE && !mNeedFullImage) continue;
	            task = startTaskIfNeeded(currentIndex + offset, bit);
	            if (task != null) break;
	        }
        }

        // 2. Cancel everything else.
        for (ImageEntry entry : mImageCache.values()) {
            if (entry.screenNailTask != null && entry.screenNailTask != task) {
                entry.screenNailTask.cancel();
                entry.screenNailTask = null;
                entry.requestedScreenNail = IMediaObject.INVALID_DATA_VERSION;
            }
            if (entry.fullImageTask != null && entry.fullImageTask != task) {
                entry.fullImageTask.cancel();
                entry.fullImageTask = null;
                entry.requestedFullImage = IMediaObject.INVALID_DATA_VERSION;
            }
        }
    }

    private void increaseDataRequestCount() {
    	if (mDataRequestCount < 0) mDataRequestCount = 0;
    	mDataRequestCount ++;
    	
    	GLActivity activity = mPhotoPage.getActivity();
    	if (activity != null) activity.postShowProgress(false);
    }
    
    private void decreaseDataRequestCount() {
    	mDataRequestCount --;
    	if (mDataRequestCount < 0) mDataRequestCount = 0;
    	
    	GLActivity activity = mPhotoPage.getActivity();
    	if (activity != null) activity.postHideProgress(false);
    }
    
    public int getDataRequestCount() {
    	return mDataRequestCount;
    }
    
    private class ScreenNailJob implements Job<ScreenNail>, 
    		IMediaItem.RequestListener {
        private IMediaItem mItem;

        public ScreenNailJob(IMediaItem item) {
            mItem = item;
        }

        @Override
        public ScreenNail run(JobContext jc) {
            // We try to get a ScreenNail first, if it fails, we fallback to get
            // a Bitmap and then wrap it in a BitmapScreenNail instead.
            ScreenNail s = mItem.getScreenNail();
            if (s != null) return s;

            // If this is a temporary item, don't try to get its bitmap because
            // it won't be available. We will get its bitmap after a data reload.
            if (isTemporaryItem(mItem)) 
                return newPlaceholderScreenNail(mItem);

            Job<BitmapRef> job = mItem.requestThumbnail(getBitmapHolder(), this);
            BitmapRef bitmap = job != null ? job.run(jc) : null;
            if (jc.isCancelled()) return null;
            if (bitmap != null) {
                bitmap = BitmapUtil.rotateBitmap(getBitmapHolder(), bitmap,
                    mItem.getRotation() - mItem.getRotation(), true);
            }
            
            return bitmap == null ? null : 
            	new TiledScreenNail(getBitmapHolder(), bitmap);
        }
        
        @Override
        public void onMediaItemPrepare(IMediaItem item) {
        	if (item != null) increaseDataRequestCount();
        	if (item != mItem) return;
        	if (LOG.isDebugEnabled()) {
        		LOG.debug("onMediaItemPrepare: screen nail prepare, item=" + item
        				+ " requests=" + getDataRequestCount());
        	}
        }
        
        @Override
		public void onMediaItemDone(IMediaItem item, boolean ready) {
        	if (item != null) decreaseDataRequestCount();
        	if (item != mItem) return;
        	if (LOG.isDebugEnabled()) {
        		LOG.debug("onMediaItemDone: screen nail done, item=" + item 
        				+ " ready=" + ready + " requests=" + getDataRequestCount());
        	}
        	
        	if (ready) updateImageRequests(item, BIT_SCREEN_NAIL);
		}
        
        @Override
        public String toString() {
        	return getClass().getSimpleName() + "{item=" + mItem + "}";
        }
    }

    private class FullImageJob implements Job<BitmapRegionDecoder>, 
    		IMediaItem.RequestListener {
        private IMediaItem mItem;

        public FullImageJob(IMediaItem item) {
            mItem = item;
        }

        @Override
        public BitmapRegionDecoder run(JobContext jc) {
            if (isTemporaryItem(mItem)) 
                return null;
            
            Job<BitmapRegionDecoder> job = mItem.requestLargeImage(getBitmapHolder(), this); 
            if (job != null) return job.run(jc);
            return null;
        }
        
        @Override
        public void onMediaItemPrepare(IMediaItem item) {
        	if (item != null) increaseDataRequestCount();
        	if (item != mItem) return;
        	if (LOG.isDebugEnabled()) {
        		LOG.debug("onMediaItemPrepare: full image prepare, item=" + item
        				+ " requests=" + getDataRequestCount());
        	}
        }
        
        @Override
		public void onMediaItemDone(IMediaItem item, boolean ready) {
        	if (item != null) decreaseDataRequestCount();
        	if (item != mItem) return;
        	if (LOG.isDebugEnabled()) {
        		LOG.debug("onMediaItemDone: full image done, item=" + item 
        				+ " ready=" + ready + " requests=" + getDataRequestCount());
        	}
        	
        	if (ready) updateImageRequests(item, BIT_FULL_IMAGE);
		}
        
        @Override
        public String toString() {
        	return getClass().getSimpleName() + "{item=" + mItem + "}";
        }
    }

    // Returns true if we think this is a temporary item created by Camera. A
    // temporary item is an image or a video whose data is still being
    // processed, but an incomplete entry is created first in MediaProvider, so
    // we can display them (in grey tile) even if they are not saved to disk
    // yet. When the image or video data is actually saved, we will get
    // notification from MediaProvider, reload data, and show the actual image
    // or video data.
    private boolean isTemporaryItem(IMediaItem mediaItem) {
        // Must have camera to create a temporary item.
        if (mCameraIndex < 0) return false;
        // Must be an item in camera roll.
        if (!mediaItem.isLocalItem()) return false;
        //if (!(mediaItem instanceof LocalMediaItem)) return false;
        //LocalMediaItem item = (LocalMediaItem) mediaItem;
        IMediaItem item = mediaItem;
        //if (item.getBucketId() != MediaUtils.CAMERA_BUCKET_ID) return false;
        // Must have no size, but must have width and height information
        if (item.getSize() != 0) return false;
        if (item.getWidth() == 0) return false;
        if (item.getHeight() == 0) return false;
        // Must be created in the last 10 seconds.
        if (item.getDateInMs() - System.currentTimeMillis() > 10000) return false;
        return true;
    }

    // Create a default ScreenNail when a ScreenNail is needed, but we don't yet
    // have one available (because the image data is still being saved, or the
    // Bitmap is still being loaded.
    private ScreenNail newPlaceholderScreenNail(IMediaItem item) {
        int width = item.getWidth();
        int height = item.getHeight();
        return new TiledScreenNail(width, height);
    }

    // Returns the task if we started the task or the task is already started.
    private Future<?> startTaskIfNeeded(int index, int which) {
        if (index < mActiveStart || index >= mActiveEnd) 
        	return null;

        ImageEntry entry = mImageCache.get(getItemAt(index));
        if (entry == null) return null;
        IMediaItem item = mDataCache[index % DATA_CACHE_SIZE];
        Utils.assertTrue(item != null);
        long version = item.getVersion();

        if (which == BIT_SCREEN_NAIL && entry.screenNailTask != null && 
        		entry.requestedScreenNail == version) {
        	if (LOG.isDebugEnabled()) {
            	LOG.debug("startTaskIfNeeded: return screenNailTask, index=" + index 
            			+ " which=" + which + " item=" + item + " entry=" + entry);
            }
        	
            return entry.screenNailTask;
            
        } else if (which == BIT_FULL_IMAGE && entry.fullImageTask != null && 
        		entry.requestedFullImage == version) {
        	if (LOG.isDebugEnabled()) {
            	LOG.debug("startTaskIfNeeded: return fullImageTask, index=" + index 
            			+ " which=" + which + " item=" + item + " entry=" + entry);
            }
        	
            return entry.fullImageTask;
        }

        if (which == BIT_SCREEN_NAIL && entry.requestedScreenNail != version) {
        	if (LOG.isDebugEnabled()) {
            	LOG.debug("startTaskIfNeeded: start screenNailTask, index=" + index 
            			+ " which=" + which + " item=" + item + " entry=" + entry);
            }
        	
        	entry.failToLoadScreenNail = false;
            entry.requestedScreenNail = version;
            entry.screenNailTask = JobSubmit.submit(
                    new ScreenNailJob(item),
                    new ScreenNailListener(item));
            // request screen nail
            return entry.screenNailTask;
        }
        
        if (which == BIT_FULL_IMAGE && entry.requestedFullImage != version && 
        		(item.getSupportedOperations() & IMediaItem.SUPPORT_FULL_IMAGE) != 0) {
        	
        	if (LOG.isDebugEnabled()) {
            	LOG.debug("startTaskIfNeeded: start fullImageTask, index=" + index 
            			+ " which=" + which + " item=" + item + " entry=" + entry);
            }
        	
        	entry.failToLoadFullImage = false;
            entry.requestedFullImage = version;
            entry.fullImageTask = JobSubmit.submit(
                    new FullImageJob(item),
                    new FullImageListener(item));
            // request full image
            return entry.fullImageTask;
        }
        
        return null;
    }

    private void updateImageCache() {
        HashSet<IMediaItem> toBeRemoved = new HashSet<IMediaItem>(mImageCache.keySet());
        for (int i = mActiveStart; i < mActiveEnd; ++i) {
            IMediaItem item = mDataCache[i % DATA_CACHE_SIZE];
            if (item == null) continue;
            
            ImageEntry entry = mImageCache.get(item);
            toBeRemoved.remove(item);
            
            if (entry != null) {
                if (Math.abs(i - mCurrentIndex) > 1) {
                    if (entry.fullImageTask != null) {
                        entry.fullImageTask.cancel();
                        entry.fullImageTask = null;
                    }
                    entry.fullImage = null;
                    entry.requestedFullImage = IMediaObject.INVALID_DATA_VERSION;
                }
                
                if (entry.requestedScreenNail != item.getVersion()) {
                    // This ScreenNail is outdated, we want to update it if it's
                    // still a placeholder.
                    if (entry.screenNail instanceof TiledScreenNail) {
                        TiledScreenNail s = (TiledScreenNail) entry.screenNail;
                        s.updatePlaceholderSize(item.getWidth(), item.getHeight());
                    }
                }
            } else {
                entry = new ImageEntry();
                mImageCache.put(item, entry);
            }
        }

        // Clear the data and requests for ImageEntries outside the new window.
        for (IMediaItem path : toBeRemoved) {
            ImageEntry entry = mImageCache.remove(path);
            if (entry.fullImageTask != null) entry.fullImageTask.cancel();
            if (entry.screenNailTask != null) entry.screenNailTask.cancel();
            if (entry.screenNail != null) entry.screenNail.recycle();
        }

        updateScreenNailUploadQueue();
    }

    private class FullImageListener implements Runnable, 
    		FutureListener<BitmapRegionDecoder> {
        private final IMediaItem mItem;
        private Future<BitmapRegionDecoder> mFuture;

        public FullImageListener(IMediaItem item) {
            mItem = item; 
        }

        @Override
        public void onFutureDone(Future<BitmapRegionDecoder> future) {
            mFuture = future;
            mMainHandler.sendMessage(
                    mMainHandler.obtainMessage(MSG_RUN_OBJECT, this));
        }

        @Override
        public void run() {
            updateFullImage(mItem, mFuture);
        }
    }

    private class ScreenNailListener implements Runnable, 
    		FutureListener<ScreenNail> {
        private final IMediaItem mItem;
        private Future<ScreenNail> mFuture;

        public ScreenNailListener(IMediaItem item) {
            mItem = item; 
        }

        @Override
        public void onFutureDone(Future<ScreenNail> future) {
            mFuture = future;
            mMainHandler.sendMessage(
                    mMainHandler.obtainMessage(MSG_RUN_OBJECT, this));
        }

        @Override
        public void run() {
            updateScreenNail(mItem, mFuture);
        }
    }

    private static class ImageEntry {
        public BitmapRegionDecoder fullImage;
        public ScreenNail screenNail;
        public Future<ScreenNail> screenNailTask;
        public Future<BitmapRegionDecoder> fullImageTask;
        public long requestedScreenNail = IMediaObject.INVALID_DATA_VERSION;
        public long requestedFullImage = IMediaObject.INVALID_DATA_VERSION;
        public boolean failToLoadScreenNail = false;
        public boolean failToLoadFullImage = false;
        
        @Override
        public String toString() {
        	return getClass().getSimpleName() + "{screenNailTask=" + screenNailTask 
        			+ ",fullImageTask=" + fullImageTask 
        			+ ",requestedScreenNail=" + requestedScreenNail 
        			+ ",requestedFullImage=" + requestedFullImage 
        			+ ",failToLoadScreenNail=" + failToLoadScreenNail 
        			+ ",failToLoadFullImage=" + failToLoadFullImage + "}";
        }
    }

    private class SourceListener implements ContentListener {
        @Override
        public void onContentDirty() {
            if (mReloadTask != null) mReloadTask.notifyDirty();
        }
    }

    private <T> T executeAndWait(Callable<T> callable) {
        FutureTask<T> task = new FutureTask<T>(callable);
        mMainHandler.sendMessage(
                mMainHandler.obtainMessage(MSG_RUN_OBJECT, task));
        try {
            return task.get();
        } catch (InterruptedException e) {
            return null;
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private static class UpdateInfo {
        public long version;
        public boolean reloadContent;
        public IMediaItem target;
        public int indexHint;
        public int contentStart;
        public int contentEnd;

        public int size;
        public List<IMediaItem> items;
        
        @Override
        public String toString() { 
        	StringBuilder sbuf = new StringBuilder();
        	sbuf.append("UpdateInfo{");
        	sbuf.append("version=").append(version);
        	sbuf.append(",reloadContent=").append(reloadContent);
        	sbuf.append(",target=").append(target);
        	sbuf.append(",indexHint=").append(indexHint);
        	sbuf.append(",contentStart=").append(contentStart);
        	sbuf.append(",contentEnd=").append(contentEnd);
        	sbuf.append(",size=").append(size);
        	//sbuf.append(",items{");
        	//if (items != null) { 
        	//	for (int i=0; i < items.size(); i++) { 
        	//		IMediaItem item = items.get(i);
        	//		if (i > 0) sbuf.append(',');
        	//		sbuf.append("[" + i + "]=").append(item);
        	//	}
        	//}
        	sbuf.append("}");
        	return sbuf.toString();
        }
        
        public void printItems() {
        	List<IMediaItem> items = this.items;
        	if (items != null && LOG.isDebugEnabled()) { 
        		for (int i=0; i < items.size(); i++) { 
        			IMediaItem item = items.get(i);
        			//if (i > 0) sbuf.append(',');
        			//sbuf.append("[" + i + "]=").append(item);
        			if (item != null)
        				LOG.debug("UpdateInfo.items[" + i + "]: " + item);
        		}
        	}
        }
    }

    private class GetUpdateInfo implements Callable<UpdateInfo> {

        private boolean needContentReload() {
            for (int i = mContentStart, n = mContentEnd; i < n; ++i) {
                if (mDataCache[i % DATA_CACHE_SIZE] == null) return true;
            }
            IMediaItem current = mDataCache[mCurrentIndex % DATA_CACHE_SIZE];
            return current == null || current != mMediaItem;
        }

        @Override
        public UpdateInfo call() throws Exception {
            // TODO: Try to load some data in first update
            UpdateInfo info = new UpdateInfo();
            info.version = mSourceVersion;
            info.reloadContent = needContentReload();
            info.target = mMediaItem;
            info.indexHint = mCurrentIndex;
            info.contentStart = mContentStart;
            info.contentEnd = mContentEnd;
            info.size = mSize;
            return info;
        }
    }

    private class UpdateContent implements Callable<Void> {
        private UpdateInfo mUpdateInfo;

        public UpdateContent(UpdateInfo updateInfo) {
            mUpdateInfo = updateInfo;
        }

        @Override
        public Void call() throws Exception {
            UpdateInfo info = mUpdateInfo;
            mSourceVersion = info.version;

            if (info.size != mSize) {
                mSize = info.size;
                if (mContentEnd > mSize) mContentEnd = mSize;
                if (mActiveEnd > mSize) mActiveEnd = mSize;
            }

            mCurrentIndex = info.indexHint;
            updateSlidingWindow();

            if (info.items != null) {
                int start = Math.max(info.contentStart, mContentStart);
                int end = Math.min(info.contentStart + info.items.size(), mContentEnd);
                int dataIndex = start % DATA_CACHE_SIZE;
                for (int i = start; i < end; ++i) {
                    mDataCache[dataIndex] = info.items.get(i - info.contentStart);
                    if (++dataIndex == DATA_CACHE_SIZE) dataIndex = 0;
                }
            }

            // update mMediaItem
            mMediaItem = mDataCache[mCurrentIndex % DATA_CACHE_SIZE];

            updateImageCache();
            updateTileProvider();
            updateImageRequests();

            if (mDataListener != null)
                mDataListener.onPhotoChanged(mCurrentIndex, mMediaItem);

            fireDataChange();
            return null;
        }
    }

    private class ReloadTask extends Thread {
        private volatile boolean mActive = true;
        private volatile boolean mDirty = true;

        private boolean mIsLoading = false;

        private void updateLoading(boolean loading) {
            if (mIsLoading == loading) return;
            mIsLoading = loading;
            mMainHandler.sendEmptyMessage(loading ? MSG_LOAD_START : MSG_LOAD_FINISH);
        }

        @Override
        public void run() {
            while (mActive) {
                synchronized (this) {
                    if (!mDirty && mActive) {
                        updateLoading(false);
                        Utils.waitWithoutInterrupt(this);
                        continue;
                    }
                }
                
                mDirty = false;
                UpdateInfo info = executeAndWait(new GetUpdateInfo());
                updateLoading(true);
                
                long version = mSource.reloadData();
                if (info.version != version) {
                    info.reloadContent = true;
                    info.size = mSource.getItemCount();
                }
                if (!info.reloadContent) continue;
                info.items = mSource.getItemList(info.contentStart, info.contentEnd);

                int index = IMediaSet.INDEX_NOT_FOUND;

                // First try to focus on the given hint path if there is one.
                if (mFocusHintItem != null) {
                    index = findIndexOfPathInCache(info, mFocusHintItem);
                    mFocusHintItem = null;
                }

                // Otherwise try to see if the currently focused item can be found.
                if (index == IMediaSet.INDEX_NOT_FOUND) {
                    IMediaItem item = findCurrentMediaItem(info);
                    if (item != null && item == info.target) {
                        index = info.indexHint;
                    } else {
                        index = findIndexOfTarget(info);
                    }
                }

                // The image has been deleted. Focus on the next image (keep
                // mCurrentIndex unchanged) or the previous image (decrease
                // mCurrentIndex by 1). In page mode we want to see the next
                // image, so we focus on the next one. In film mode we want the
                // later images to shift left to fill the empty space, so we
                // focus on the previous image (so it will not move). In any
                // case the index needs to be limited to [0, mSize).
                if (index == IMediaSet.INDEX_NOT_FOUND) {
                    index = info.indexHint;
                    int focusHintDirection = mFocusHintDirection;
                    if (index == (mCameraIndex + 1)) {
                        focusHintDirection = FOCUS_HINT_NEXT;
                    }
                    if (focusHintDirection == FOCUS_HINT_PREVIOUS && index > 0) {
                        index--;
                    }
                }

                // Don't change index if mSize == 0
                if (mSize > 0) {
                    if (index >= mSize) index = mSize - 1;
                }

                info.indexHint = index;

                if (LOG.isDebugEnabled()) {
                	LOG.debug("update: " + info);
                	info.printItems();
                }
                
                executeAndWait(new UpdateContent(info));
            }
        }

        public synchronized void notifyDirty() {
        	if (LOG.isDebugEnabled())
        		LOG.debug("ReloadTask.notifyDirty");
        	
            mDirty = true;
            notifyAll();
        }

        public synchronized void terminate() {
            mActive = false;
            notifyAll();
        }

        private IMediaItem findCurrentMediaItem(UpdateInfo info) {
            List<IMediaItem> items = info.items;
            int index = info.indexHint - info.contentStart;
            return index < 0 || index >= items.size() ? null : items.get(index);
        }

        private int findIndexOfTarget(UpdateInfo info) {
            if (info.target == null) return info.indexHint;
            List<IMediaItem> items = info.items;

            // First, try to find the item in the data just loaded
            if (items != null) {
                int i = findIndexOfPathInCache(info, info.target);
                if (i != IMediaSet.INDEX_NOT_FOUND) return i;
            }

            // Not found, find it in mSource.
            return mSource.getIndexOf(info.target, info.indexHint);
        }

        private int findIndexOfPathInCache(UpdateInfo info, IMediaItem mediaItem) {
            List<IMediaItem> items = info.items;
            for (int i = 0, n = items.size(); i < n; ++i) {
                IMediaItem item = items.get(i);
                if (item != null && item == mediaItem) 
                    return i + info.contentStart;
            }
            return IMediaSet.INDEX_NOT_FOUND;
        }
    }
    
}
