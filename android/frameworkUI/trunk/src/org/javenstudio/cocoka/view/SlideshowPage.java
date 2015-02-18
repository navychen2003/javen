package org.javenstudio.cocoka.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;

import org.javenstudio.cocoka.app.R;
import org.javenstudio.cocoka.data.IMediaItem;
import org.javenstudio.cocoka.data.IMediaObject;
import org.javenstudio.cocoka.data.IMediaSet;
import org.javenstudio.cocoka.opengl.GLActivityState;
import org.javenstudio.cocoka.opengl.GLCanvas;
import org.javenstudio.cocoka.opengl.GLView;
import org.javenstudio.cocoka.opengl.SynchronizedHandler;
import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.cocoka.util.ContentListener;
import org.javenstudio.cocoka.util.Utils;
import org.javenstudio.cocoka.worker.job.Future;
import org.javenstudio.cocoka.worker.job.FutureListener;
import org.javenstudio.common.util.Logger;

public class SlideshowPage extends GLActivityState {
	private static final Logger LOG = Logger.getLogger(SlideshowPage.class);

    public static final String KEY_SET_PATH = "media-set-path";
    public static final String KEY_ITEM_PATH = "media-item-path";
    public static final String KEY_PHOTO_INDEX = "photo-index";
    public static final String KEY_RANDOM_ORDER = "random-order";
    public static final String KEY_REPEAT = "repeat";
    public static final String KEY_DREAM = "dream";

    private static final long SLIDESHOW_DELAY = 3000; // 3 seconds

    private static final int MSG_LOAD_NEXT_BITMAP = 1;
    private static final int MSG_SHOW_PENDING_BITMAP = 2;

    public static interface Model {
        public void pause();
        public void resume();
        
        public Future<Slide> nextSlide(FutureListener<Slide> listener);
    }

    public static class Slide {
        private final BitmapRef mBitmap;
        private final IMediaItem mItem;
        private final int mIndex;

        public Slide(IMediaItem item, int index, BitmapRef bitmap) {
            mBitmap = bitmap;
            mItem = item;
            mIndex = index;
        }
        
        public IMediaItem getItem() { return mItem; }
        public BitmapRef getBitmap() { return mBitmap; }
        
        public int getIndex() { return mIndex; }
        public int getRotation() { return 0; }
        
        public String getPath() { return null; }
        
        @Override
        public String toString() { 
        	StringBuilder sbuf = new StringBuilder();
        	sbuf.append("Slide{bitmap=" + mBitmap);
        	sbuf.append(",item=" + mItem);
        	sbuf.append(",index=" + mIndex);
        	sbuf.append("}");
        	return sbuf.toString();
        }
    }

    private Handler mHandler;
    protected Model mModel;
    private SlideshowView mSlideshowView;

    private Slide mPendingSlide = null;
    private boolean mIsActive = false;
    private final Intent mResultIntent = new Intent();

    protected Intent getResultIntent() { return mResultIntent; }
    
    @Override
    protected int getBackgroundColorId() {
        return R.color.slideshow_background;
    }

    private final GLView mRootPane = new GLView() {
        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            mSlideshowView.layout(0, 0, right - left, bottom - top);
        }

        @Override
        protected boolean onTouch(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) { 
            	if (LOG.isDebugEnabled())
            		LOG.debug("onBackPressed when motion action up");
            	
                onBackPressed();
            }
            
            return true;
        }

        @Override
        protected void renderBackground(GLCanvas canvas) {
            canvas.clearBuffer(getBackgroundColor());
        }
    };

    @Override
    public void onCreate(Bundle data, Bundle restoreState) {
        super.onCreate(data, restoreState);
        
        mFlags |= (FLAG_HIDE_ACTION_BAR | FLAG_HIDE_STATUS_BAR | 
        		   FLAG_ALLOW_LOCK_WHILE_SCREEN_ON | FLAG_SHOW_WHEN_LOCKED);
        
        if (data.getBoolean(KEY_DREAM)) {
            // Dream screensaver only keeps screen on for plugged devices.
            mFlags |= FLAG_SCREEN_ON_WHEN_PLUGGED;
        } else {
            // User-initiated slideshow would always keep screen on.
            mFlags |= FLAG_SCREEN_ON_ALWAYS;
        }

        mHandler = new SynchronizedHandler(mActivity.getGLRoot()) {
	            @Override
	            public void handleMessage(Message message) {
	                switch (message.what) {
	                    case MSG_SHOW_PENDING_BITMAP:
	                        showPendingBitmap();
	                        break;
	                    case MSG_LOAD_NEXT_BITMAP:
	                        loadNextBitmap();
	                        break;
	                    default: throw new AssertionError();
	                }
	            }
	        };
        
        initializeViews();
        initializeData(data);
    }

    protected IMediaSet getMediaObject() { return null; }
    
    protected void initializeData(Bundle data) { 
    	boolean random = data.getBoolean(KEY_RANDOM_ORDER, false);
		boolean repeat = data.getBoolean(KEY_REPEAT);
		
        // We only want to show slideshow for images only, not videos.
        //String mediaPath = data.getString(KEY_SET_PATH);
		IMediaSet mediaSet = (IMediaSet)getMediaObject();

        mModel = new SlideshowAdapter(new ShuffleSource(
        		this, mediaSet, random, repeat), 0);
        
        setStateResult(Activity.RESULT_OK, 
        		getResultIntent().putExtra(KEY_PHOTO_INDEX, 0));
    }
    
	protected long reloadData(IMediaSet mediaSet) {
		return mediaSet.reloadData();
	}
    
    private void loadNextBitmap() {
        mModel.nextSlide(new FutureListener<Slide>() {
	            @Override
	            public void onFutureDone(Future<Slide> future) {
	                mPendingSlide = future.get();
	                if (mPendingSlide == null && LOG.isDebugEnabled()) 
	                	LOG.debug("get pending Slide return null");
	                
	                mHandler.sendEmptyMessage(MSG_SHOW_PENDING_BITMAP);
	            }
	        });
    }

    private void showPendingBitmap() {
        // mPendingBitmap could be null, if
        // 1.) there is no more items
        // 2.) mModel is paused
        Slide slide = mPendingSlide;
        if (slide == null) {
            if (LOG.isDebugEnabled())
            	LOG.debug("showPendingBitmap: pending Slide is null");
            
            if (mIsActive) 
                mActivity.getGLStateManager().finishState(SlideshowPage.this);
            
            return;
        }

        if (LOG.isDebugEnabled())
        	LOG.debug("showPendingBitmap: pending Slide: " + slide);
        
        mSlideshowView.next(slide.getBitmap(), slide.getRotation());

        setStateResult(Activity.RESULT_OK, mResultIntent
                .putExtra(KEY_ITEM_PATH, slide.getPath())
                .putExtra(KEY_PHOTO_INDEX, slide.getIndex()));
        
        mHandler.sendEmptyMessageDelayed(MSG_LOAD_NEXT_BITMAP, SLIDESHOW_DELAY);
    }

    @Override
    public void onPause() {
        super.onPause();
        mIsActive = false;
        mModel.pause();
        mSlideshowView.release();

        mHandler.removeMessages(MSG_LOAD_NEXT_BITMAP);
        mHandler.removeMessages(MSG_SHOW_PENDING_BITMAP);
    }

    @Override
    public void onResume() {
        super.onResume();
        mIsActive = true;
        mModel.resume();

        if (mPendingSlide != null) 
            showPendingBitmap();
        else 
            loadNextBitmap();
    }

    private void initializeViews() {
        mSlideshowView = new SlideshowView(this);
        mRootPane.addComponent(mSlideshowView);
        setContentPane(mRootPane);
    }

    protected class ShuffleSource implements SlideshowAdapter.SlideshowSource {
		private static final int RETRY_COUNT = 5;
		
		private final BitmapHolder mHolder;
		private final IMediaSet mMediaSet;
		private final Random mRandom = new Random();
		private int mOrder[] = new int[0];
		private final boolean mIsRepeat, mIsRandom;
		private long mSourceVersion = IMediaSet.INVALID_DATA_VERSION;
		private int mLastIndex = -1;
		
		public ShuffleSource(BitmapHolder holder, IMediaSet mediaSet, 
				boolean random, boolean repeat) {
			mHolder = holder;
		    mMediaSet = Utils.checkNotNull(mediaSet);
		    mIsRandom = random;
		    mIsRepeat = repeat;
		}
		
		public BitmapHolder getBitmapHolder() { return mHolder; }
		public IMediaSet getItemSet() { return mMediaSet; }
		
		@Override
		public IMediaItem getDataItem(int index) {
		    if (!mIsRepeat && index >= mOrder.length) return null;
		    if (mOrder.length == 0) return null;
		    
		    mLastIndex = mOrder[index % mOrder.length];
		    IMediaItem item = mMediaSet.findItem(mLastIndex);
		    
		    for (int i = 0; i < RETRY_COUNT && item == null; ++i) {
		    	if (LOG.isWarnEnabled())
		        	LOG.warn("fail to find image: " + mLastIndex);
		    	
		        mLastIndex = mRandom.nextInt(mOrder.length);
		        item = mMediaSet.findItem(mLastIndex);
		    }
		    
		    return item;
		}
		
		@Override
		public long reloadSource() {
		    long version = reloadData(mMediaSet);
		    if (version != mSourceVersion) {
		        mSourceVersion = version;
		        int count = mMediaSet.getItemCount();
		        if (count != mOrder.length) 
		        	generateOrderArray(count);
		    }
		    return version;
		}
		
		private void generateOrderArray(int totalCount) {
		    if (mOrder.length != totalCount) {
		        mOrder = new int[totalCount];
		        
		        for (int i = 0; i < totalCount; ++i) {
		            mOrder[i] = i;
		        }
		    }
		    
		    if (mIsRandom) {
			    for (int i = totalCount - 1; i > 0; --i) {
			        Utils.swap(mOrder, i, mRandom.nextInt(i + 1));
			    }
			    
			    if (mOrder[0] == mLastIndex && totalCount > 1) 
			        Utils.swap(mOrder, 0, mRandom.nextInt(totalCount - 1) + 1);
		    }
		}
		
		@Override
		public void addContentListener(ContentListener listener) {
		    mMediaSet.addContentListener(listener);
		}
		
		@Override
		public void removeContentListener(ContentListener listener) {
		    mMediaSet.removeContentListener(listener);
		}
    }
    
    protected class SequentialSource implements SlideshowAdapter.SlideshowSource {
    	protected static final int DATA_SIZE = 32;

    	private final BitmapHolder mHolder;
        private List<IMediaItem> mData = new ArrayList<IMediaItem>();
        private int mDataStart = 0;
        private long mDataVersion = IMediaObject.INVALID_DATA_VERSION;
        private final IMediaSet mMediaSet;
        private final boolean mIsRepeat;

        public SequentialSource(BitmapHolder holder, IMediaSet mediaSet, 
        		boolean repeat) {
        	mHolder = holder;
            mMediaSet = mediaSet;
            mIsRepeat = repeat;
        }

        public BitmapHolder getBitmapHolder() { return mHolder; }
        public IMediaSet getItemSet() { return mMediaSet; }
        
        @Override
        public IMediaItem getDataItem(int index) {
            int dataEnd = mDataStart + mData.size();

            if (mIsRepeat) {
                int count = mMediaSet.getItemCount();
                if (count == 0) return null;
                index = index % count;
            }
            
            if (index < mDataStart || index >= dataEnd) {
                mData = mMediaSet.getItemList(index, DATA_SIZE); 
                mDataStart = index;
                dataEnd = index + mData.size();
            }

            return (index < mDataStart || index >= dataEnd) ? null : 
            		mData.get(index - mDataStart);
        }

        @Override
        public long reloadSource() {
            long version = reloadData(mMediaSet);
            if (version != mDataVersion) {
                mDataVersion = version;
                mData.clear();
            }
            return mDataVersion;
        }

        @Override
        public void addContentListener(ContentListener listener) {
            mMediaSet.addContentListener(listener);
        }

        @Override
        public void removeContentListener(ContentListener listener) {
            mMediaSet.removeContentListener(listener);
        }
    }
    
}
