package org.javenstudio.cocoka.view;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.javenstudio.cocoka.data.IMediaItem;
import org.javenstudio.cocoka.data.IMediaObject;
import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.cocoka.util.ContentListener;
import org.javenstudio.cocoka.worker.job.Future;
import org.javenstudio.cocoka.worker.job.FutureListener;
import org.javenstudio.cocoka.worker.job.Job;
import org.javenstudio.cocoka.worker.job.JobContext;
import org.javenstudio.cocoka.worker.job.JobSubmit;
import org.javenstudio.common.util.Logger;

public class SlideshowAdapter implements SlideshowPage.Model {
	private static final Logger LOG = Logger.getLogger(SlideshowAdapter.class);
	
    private static final int IMAGE_QUEUE_CAPACITY = 3;

    public interface SlideshowSource {
        public void addContentListener(ContentListener listener);
        public void removeContentListener(ContentListener listener);
        
        public IMediaItem getDataItem(int index);
        public BitmapHolder getBitmapHolder();
        public long reloadSource();
    }

    private final SlideshowSource mSource;

    private int mLoadIndex = 0;
    private int mNextOutput = 0;
    private boolean mIsActive = false;
    private boolean mNeedReset;
    private boolean mDataReady;

    private final LinkedList<SlideshowPage.Slide> mImageQueue = 
    		new LinkedList<SlideshowPage.Slide>();

	private Future<Void> mReloadTask;

    private long mDataVersion = IMediaObject.INVALID_DATA_VERSION;
    private final AtomicBoolean mNeedReload = new AtomicBoolean(false);
    private final SourceListener mSourceListener = new SourceListener();

    // The index is just a hint if initialPath is set
    public SlideshowAdapter(SlideshowSource source, int index) {
        mSource = source;
        mLoadIndex = index;
        mNextOutput = index;
    }

    private IMediaItem loadDataItem() {
        if (mNeedReload.compareAndSet(true, false)) {
            long v = mSource.reloadSource();
            if (v != mDataVersion) {
            	if (LOG.isDebugEnabled()) {
            		LOG.debug("data version changed from " + mDataVersion 
            				+ " to " + v + ", need reset");
            	}
            	
                mDataVersion = v;
                mNeedReset = true;
                return null;
            }
        }
        
        int index = mLoadIndex;
        return mSource.getDataItem(index);
    }

	private class ReloadTask implements Job<Void> {
        @Override
        public Void run(JobContext jc) {
            while (true) {
                synchronized (SlideshowAdapter.this) {
                    while (mIsActive && (!mDataReady || mImageQueue.size() >= IMAGE_QUEUE_CAPACITY)) {
                        try {
                        	if (LOG.isDebugEnabled()) {
                    			LOG.debug("ReloadTask: waiting, isActive=" + mIsActive 
                    					+ " dataReady=" + mDataReady + " imageQueue=" 
                    					+ mImageQueue.size());
                    		}
                        	
                            SlideshowAdapter.this.wait();
                        } catch (InterruptedException ex) {
                            // ignored.
                        }
                        continue;
                    }
                }
                if (!mIsActive) return null;
                mNeedReset = false;

                IMediaItem item = loadDataItem();

                if (mNeedReset) {
                    synchronized (SlideshowAdapter.this) {
                        mImageQueue.clear();
                        mLoadIndex = mNextOutput;
                    }
                    continue;
                }

                if (item == null) {
                	if (LOG.isDebugEnabled())
                		LOG.debug("load MediaItem return null, data not ready");
                	
                    synchronized (SlideshowAdapter.this) {
                        if (!mNeedReload.get()) mDataReady = false;
                        SlideshowAdapter.this.notifyAll();
                    }
                    continue;
                }

                BitmapRef bitmap = item.requestThumbnail(mSource.getBitmapHolder(), null)
                		.run(jc);

                synchronized (SlideshowAdapter.this) {
                    mImageQueue.addLast(new SlideshowPage.Slide(item, mLoadIndex, bitmap));
                    if (mImageQueue.size() == 1) 
                        SlideshowAdapter.this.notifyAll();
                }
                
                mLoadIndex ++;
                
                if (LOG.isDebugEnabled())
                	LOG.debug("MediaItem: " + item + " loaded bitmap, loadIndex=" + mLoadIndex);
            }
        }
    }

    private class SourceListener implements ContentListener {
        @Override
        public void onContentDirty() {
            synchronized (SlideshowAdapter.this) {
                mNeedReload.set(true);
                mDataReady = true;
                SlideshowAdapter.this.notifyAll();
            }
        }
    }

	private synchronized SlideshowPage.Slide innerNextBitmap() {
        while (mIsActive && mDataReady && mImageQueue.isEmpty()) {
            try {
        		if (LOG.isDebugEnabled()) {
        			LOG.debug("innerNextBitmap: waiting, isActive=" + mIsActive 
        					+ " dataReady=" + mDataReady + " imageQueue=" 
        					+ mImageQueue.size());
        		}
        		
                wait();
            } catch (InterruptedException t) {
                throw new AssertionError();
            }
        }
        
        if (mImageQueue.isEmpty()) { 
        	if (LOG.isWarnEnabled())
        		LOG.warn("image queue is empty");
        	
        	return null; 
        }
        
        mNextOutput++;
        this.notifyAll();
        
		if (LOG.isDebugEnabled()) {
			LOG.debug("innerNextBitmap: return first, imageQueue=" + mImageQueue.size() 
					+ " nextOutput=" + mNextOutput);
		}
        
        return mImageQueue.removeFirst();
    }

    @Override
    public Future<SlideshowPage.Slide> nextSlide(
    		FutureListener<SlideshowPage.Slide> listener) {
        return JobSubmit.submit(new Job<SlideshowPage.Slide>() {
	            @Override
	            public SlideshowPage.Slide run(JobContext jc) {
	                jc.setMode(JobSubmit.MODE_NONE);
	                return innerNextBitmap();
	            }
	        }, listener);
    }

    @Override
    public void pause() {
        synchronized (this) {
            mIsActive = false;
            notifyAll();
        }
        mSource.removeContentListener(mSourceListener);
        mReloadTask.cancel();
        mReloadTask.waitDone();
        mReloadTask = null;
    }

    @Override
    public synchronized void resume() {
        mIsActive = true;
        mSource.addContentListener(mSourceListener);
        mNeedReload.set(true);
        mDataReady = true;
        mReloadTask = JobSubmit.submit(new ReloadTask());
    }
    
}
