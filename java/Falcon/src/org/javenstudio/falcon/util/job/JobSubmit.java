package org.javenstudio.falcon.util.job;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.javenstudio.common.util.Logger;

public class JobSubmit {
	private static final Logger LOG = Logger.getLogger(JobSubmit.class);
	
    // Resource type
    public static final int MODE_NONE = 0;
    public static final int MODE_CPU = 1;
    public static final int MODE_NETWORK = 2;

    public static final JobContext JOB_CONTEXT_STUB = new JobContextStub();

    private static final ResourceCounter sCpuCounter = new ResourceCounter(2);
    private static final ResourceCounter sNetworkCounter = new ResourceCounter(2);
    //private static final AtomicInteger sJobCounter = new AtomicInteger(0);

    private static class JobContextStub implements JobContext {
        @Override
        public boolean isCancelled() {
            return false;
        }
        @Override
        public void setCancelListener(JobCancelListener listener) {
        }
        @Override
        public boolean setMode(int mode) {
            return true;
        }
    }

    private static class ResourceCounter {
        public int value;
        public ResourceCounter(int v) {
            value = v;
        }
    }

    private static final int CORE_POOL_SIZE = 2;
    private static final int MAX_POOL_SIZE = 100;
    private static final int KEEP_ALIVE_TIME = 10; // 10 seconds
    
    private static class PriorityThreadFactory implements ThreadFactory { 

        //private final int mPriority;
        private final AtomicInteger mNumber = new AtomicInteger();
        private final String mName;

        public PriorityThreadFactory(String name, int priority) {
            mName = name;
            //mPriority = priority;
        }

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, mName + '-' + mNumber.getAndIncrement()) {
                @Override
                public void run() {
                    //Process.setThreadPriority(mPriority);
                    super.run();
                }
            };
        }
    }
    
    private static class ThreadPool { 
        private final Executor mExecutor;

        public ThreadPool() {
            this(CORE_POOL_SIZE, MAX_POOL_SIZE);
        }

        public ThreadPool(int initPoolSize, int maxPoolSize) {
            mExecutor = new ThreadPoolExecutor(
                    initPoolSize, maxPoolSize, KEEP_ALIVE_TIME,
                    TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
                    new PriorityThreadFactory("jobthread",
                    0));
        }
    }
    
    private static ThreadPool sThreadPool = null;
    private static Object sThreadLock = new Object();
    
    public static void execute(Runnable r) { 
    	if (r == null) return;
    	
    	synchronized (sThreadLock) { 
    		if (sThreadPool == null) 
    			sThreadPool = new ThreadPool();
    	}
    	
    	sThreadPool.mExecutor.execute(r);
    }
    
    // Submit a job to the thread pool. The listener will be called when the
    // job is finished (or cancelled).
    public static <T> JobWork<T> submit(Job<T> job, FutureListener<T> listener) {
    	if (job == null) return null;
    	try {
	    	JobWork<T> w = new JobWork<T>(job, listener);
	        //ResourceHelper.getScheduler().post(w);
	    	execute(w);
	        return w;
    	} catch (Throwable e) { 
    		if (LOG.isErrorEnabled())
    			LOG.error(e.toString(), e);
    		return null;
    	}
    }
    
    public static <T> JobWork<T> submit(Job<T> job) {
        return submit(job, null);
    }

    public static JobContext newContext() { 
    	return new JobContextImpl();
    }
    
    private static class JobContextImpl implements JobContext {
    	//private JobCancelListener mCancelListener;
    	//private int mMode;
    	
		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public void setCancelListener(JobCancelListener listener) {
			//mCancelListener = listener;
		}

		@Override
		public boolean setMode(int mode) {
			return false;
		}
    }
    
    private static final List<JobWork<?>> sWorks = 
    		new ArrayList<JobWork<?>>();
    
    public static interface Filter { 
    	public boolean accept(JobWork<?> work);
    }
    
    public static Collection<JobWork<?>> getWorks() { 
    	return getWorks(null);
    }
    
    public static Collection<JobWork<?>> getWorks(Filter filter) { 
    	List<JobWork<?>> list = new ArrayList<JobWork<?>>();
    	
    	synchronized (sWorks) { 
    		for (int i=0; i < sWorks.size(); ) { 
    			JobWork<?> impl = sWorks.get(i);
    			if (impl == null || impl.isDone()) { 
    				sWorks.remove(i);
    				continue;
    			} else {
    				if (filter == null || filter.accept(impl))
    					list.add(impl);
    			}
    			i ++;
    		}
    	}
    	
    	return list;
    }
    
    private static void addWork(JobWork<?> work) { 
    	if (work == null) return;
    	
    	synchronized (sWorks) { 
    		boolean found = false;
    		
    		for (int i=0; i < sWorks.size(); ) { 
    			JobWork<?> impl = sWorks.get(i);
    			if (impl == null || impl.isDone()) { 
    				sWorks.remove(i);
    				continue;
    			} else if (impl == work) {
    				found = true;
    			}
    			i ++;
    		}
    		
    		if (!found) sWorks.add(work);
    	}
    }
    
    private static void removeWork(JobWork<?> work) { 
    	if (work == null) return;
    	
    	synchronized (sWorks) { 
    		for (int i=0; i < sWorks.size(); ) { 
    			JobWork<?> impl = sWorks.get(i);
    			if (impl == null || impl == work || impl.isDone()) { 
    				sWorks.remove(i);
    				continue;
    			}
    			i ++;
    		}
    	}
    }
    
    public static class JobWork<T> extends Work 
    		implements Runnable, Future<T>, JobContext {

        private final Job<T> mJob;
        private final FutureListener<T> mListener;
        private JobCancelListener mCancelListener;
        private ResourceCounter mWaitOnResource;
        private volatile boolean mIsCancelled;
        private boolean mIsDone;
        private T mResult;
        private int mMode;

        private JobWork(Job<T> job, FutureListener<T> listener) {
        	super(job.getName());
            mJob = job;
            mListener = listener;
            addWork(this);
        }

        public Job<T> getJob() { return mJob; }
        
        // This is called by a thread in the thread pool.
        @Override
        public void onRun() {
            T result = null;

            // A job is in CPU mode by default. setMode returns false
            // if the job is cancelled.
            if (mJob != null && setMode(MODE_CPU)) {
                try {
                	if (LOG.isDebugEnabled())
                		LOG.debug("running job: " + mJob);
                	
                    result = mJob.run(this);
                    
                    if (result == null && LOG.isDebugEnabled())
                    	LOG.debug("job: " + mJob + " return null");
                    
                } catch (Throwable ex) {
                	if (LOG.isWarnEnabled())
                    	LOG.warn("Exception in running a job", ex);
                }
            } else { 
            	if (LOG.isWarnEnabled())
            		LOG.warn("job: " + mJob + " not run, setMode=false");
            }

            synchronized(this) {
                setMode(MODE_NONE);
                mResult = result;
                mIsDone = true;
                notifyAll();
            }
            
            if (mListener != null) 
            	mListener.onFutureDone(this);
            
            removeWork(this);
        }

        // Below are the methods for Future.
        @Override
        public synchronized void cancel() {
            if (mIsCancelled) return;
            mIsCancelled = true;
            
            if (LOG.isDebugEnabled())
            	LOG.debug("cancel: work=" + this);
            
            if (mJob != null)
            	mJob.onCancel();
            
            if (mWaitOnResource != null) {
                synchronized (mWaitOnResource) {
                    mWaitOnResource.notifyAll();
                }
            }
            
            if (mCancelListener != null) 
                mCancelListener.onCancel();
        }

        @Override
        public boolean isCancelled() {
            return mIsCancelled;
        }

        @Override
        public synchronized boolean isDone() {
            return mIsDone;
        }

        @Override
        public synchronized T get() {
            while (!mIsDone) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    // ignore.
                }
            }
            return mResult;
        }

        @Override
        public void waitDone() {
        	if (LOG.isDebugEnabled())
        		LOG.debug("waitDone: work=" + this);
            get();
        }

        // Below are the methods for JobContext (only called from the
        // thread running the job)
        @Override
        public synchronized void setCancelListener(JobCancelListener listener) {
            mCancelListener = listener;
            if (mIsCancelled && mCancelListener != null) 
                mCancelListener.onCancel();
        }

        @Override
        public boolean setMode(int mode) {
            // Release old resource
            ResourceCounter rc = modeToCounter(mMode);
            if (rc != null) releaseResource(rc);
            mMode = MODE_NONE;

            // Acquire new resource
            rc = modeToCounter(mode);
            if (rc != null) {
                if (!acquireResource(rc)) 
                    return false;
                
                mMode = mode;
            }

            return true;
        }

        private ResourceCounter modeToCounter(int mode) {
            if (mode == MODE_CPU) 
                return sCpuCounter;
            else if (mode == MODE_NETWORK) 
                return sNetworkCounter;
            else 
                return null;
        }

        private boolean acquireResource(ResourceCounter counter) {
            while (true) {
                synchronized (this) {
                    if (mIsCancelled) {
                        mWaitOnResource = null;
                        return false;
                    }
                    mWaitOnResource = counter;
                }

                synchronized (counter) {
                    if (counter.value > 0) {
                        counter.value--;
                        break;
                    } else {
                        try {
                            counter.wait();
                        } catch (InterruptedException ex) {
                            // ignore.
                        }
                    }
                }
            }

            synchronized (this) {
                mWaitOnResource = null;
            }

            return true;
        }

        private void releaseResource(ResourceCounter counter) {
            synchronized (counter) {
                counter.value++;
                counter.notifyAll();
            }
        }
    }
    
}
