package org.javenstudio.cocoka.view;

import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;

import org.javenstudio.cocoka.data.IMediaItem;
import org.javenstudio.cocoka.graphics.BitmapUtil;
import org.javenstudio.cocoka.opengl.BitmapScreenNail;
import org.javenstudio.cocoka.opengl.GLActivity;
import org.javenstudio.cocoka.opengl.ScreenNail;
import org.javenstudio.cocoka.opengl.SynchronizedHandler;
import org.javenstudio.cocoka.opengl.TileImageViewAdapter;
import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.cocoka.util.Utils;
import org.javenstudio.cocoka.worker.job.Future;
import org.javenstudio.cocoka.worker.job.FutureListener;
import org.javenstudio.cocoka.worker.job.JobSubmit;
import org.javenstudio.common.util.Logger;

public class SinglePhotoAdapter extends TileImageViewAdapter 
		implements PhotoPage.Model {
	private static final Logger LOG = Logger.getLogger(SinglePhotoAdapter.class);
	
    private static final int SIZE_BACKUP = 1024;
    private static final int MSG_UPDATE_IMAGE = 1;

    private IMediaItem mItem;
    private boolean mHasFullImage;
    private Future<?> mTask;
    private Handler mHandler;

    private final PhotoPage mPhotoPage;
    private PhotoView mPhotoView;
    private int mLoadingState = LOADING_INIT;
    private BitmapScreenNail mBitmapScreenNail;

    public SinglePhotoAdapter(GLActivity activity, PhotoPage page, IMediaItem item) {
    	super(page);
        mItem = Utils.checkNotNull(item);
        mPhotoPage = page;
        mHasFullImage = (item.getSupportedOperations() & IMediaItem.SUPPORT_FULL_IMAGE) != 0;
        mPhotoView = Utils.checkNotNull(page.mPhotoView);
        mHandler = new SynchronizedHandler(activity.getGLRoot()) {
            @Override
            @SuppressWarnings("unchecked")
            public void handleMessage(Message message) {
                Utils.assertTrue(message.what == MSG_UPDATE_IMAGE);
                if (mHasFullImage) {
                    onDecodeLargeComplete((ImageBundle) message.obj);
                } else {
                    onDecodeThumbComplete((Future<BitmapRef>) message.obj);
                }
            }
        };
    }

    public BitmapHolder getBitmapHolder() { return mPhotoPage; }
    
    private static class ImageBundle {
        public final BitmapRegionDecoder decoder;
        public final BitmapRef backupImage;

        public ImageBundle(BitmapRegionDecoder decoder, BitmapRef backupImage) {
            this.decoder = decoder;
            this.backupImage = backupImage;
        }
    }

    private FutureListener<BitmapRegionDecoder> mLargeListener =
            new FutureListener<BitmapRegionDecoder>() {
        @Override
        public void onFutureDone(Future<BitmapRegionDecoder> future) {
            BitmapRegionDecoder decoder = future.get();
            if (decoder == null) return;
            int width = decoder.getWidth();
            int height = decoder.getHeight();
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = BitmapUtil.computeSampleSize(
                    (float) SIZE_BACKUP / Math.max(width, height));
            BitmapRef bitmap = BitmapRef.decodeRegion(getBitmapHolder(), decoder, 
            		new Rect(0, 0, width, height), options);
            mHandler.sendMessage(mHandler.obtainMessage(
                    MSG_UPDATE_IMAGE, new ImageBundle(decoder, bitmap)));
        }
    };

    private FutureListener<BitmapRef> mThumbListener =
            new FutureListener<BitmapRef>() {
        @Override
        public void onFutureDone(Future<BitmapRef> future) {
            mHandler.sendMessage(
                    mHandler.obtainMessage(MSG_UPDATE_IMAGE, future));
        }
    };

    @Override
    public boolean isEmpty() {
        return false;
    }

    private void setScreenNail(BitmapRef bitmap, int width, int height) {
        mBitmapScreenNail = new BitmapScreenNail(getBitmapHolder(), bitmap);
        setScreenNail(mBitmapScreenNail, width, height);
    }

    private void onDecodeLargeComplete(ImageBundle bundle) {
        try {
        	if (bundle != null) {
	            setScreenNail(bundle.backupImage,
	                    bundle.decoder.getWidth(), bundle.decoder.getHeight());
	            setRegionDecoder(bundle.decoder);
	            mPhotoView.notifyImageChange(0);
        	}
        } catch (Throwable t) {
        	if (LOG.isDebugEnabled())
        		LOG.debug("fail to decode large", t);
        }
    }

    private void onDecodeThumbComplete(Future<BitmapRef> future) {
        try {
        	if (future != null) {
	            BitmapRef backup = future.get();
	            if (backup == null) {
	                mLoadingState = LOADING_SCREENNAIL_FAIL;
	                return;
	            } else {
	                mLoadingState = LOADING_COMPLETE;
	            }
	            setScreenNail(backup, backup.getWidth(), backup.getHeight());
	            mPhotoView.notifyImageChange(0);
        	}
        } catch (Throwable t) {
        	if (LOG.isDebugEnabled())
            	LOG.debug("fail to decode thumb", t);
        }
    }

    @Override
    public void resume() {
        if (mTask == null) {
            if (mHasFullImage) {
            	mTask = JobSubmit.submit(
            			mItem.requestLargeImage(getBitmapHolder(), null), 
            			mLargeListener);
            } else {
        		mTask = JobSubmit.submit(
        				mItem.requestThumbnail(getBitmapHolder(), null), 
        				mThumbListener);
            }
        }
    }

    @Override
    public void pause() {
        Future<?> task = mTask;
        task.cancel();
        task.waitDone();
        if (task.get() == null) {
            mTask = null;
        }
        if (mBitmapScreenNail != null) {
            mBitmapScreenNail.recycle();
            mBitmapScreenNail = null;
        }
    }

    @Override
    public void moveTo(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void getImageSize(int offset, PhotoView.Size size) {
        if (offset == 0) {
            size.width = mItem.getWidth();
            size.height = mItem.getHeight();
        } else {
            size.width = 0;
            size.height = 0;
        }
    }

    @Override
    public int getImageRotation(int offset) {
        return (offset == 0) ? mItem.getRotation() : 0;
    }

    @Override
    public ScreenNail getScreenNail(int offset) {
        return (offset == 0) ? getScreenNail() : null;
    }

    @Override
    public void setNeedFullImage(boolean enabled) {
        // currently not necessary.
    }

    @Override
    public boolean isCamera(int offset) {
        return false;
    }

    @Override
    public boolean isStaticCamera(int offset) {
        return false;
    }

    @Override
    public boolean isVideo(int offset) {
        return mItem.getMediaType() == IMediaItem.MEDIA_TYPE_VIDEO;
    }

    @Override
    public boolean isDeletable(int offset) {
        return (mItem.getSupportedOperations() & IMediaItem.SUPPORT_DELETE) != 0;
    }

    @Override
    public IMediaItem getMediaItem(int offset) {
        return offset == 0 ? mItem : null;
    }

    @Override
    public int getCurrentIndex() {
        return 0;
    }

    @Override
    public void setCurrentPhoto(IMediaItem item, int indexHint) {
        // ignore
    }

    @Override
    public void setFocusHintDirection(int direction) {
        // ignore
    }

    @Override
    public void setFocusHintItem(IMediaItem item) {
        // ignore
    }

    @Override
    public int getLoadingState(int offset) {
        return mLoadingState;
    }
    
}
