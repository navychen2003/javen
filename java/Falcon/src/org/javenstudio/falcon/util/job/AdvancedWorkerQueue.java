package org.javenstudio.falcon.util.job;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.javenstudio.common.util.Logger;

public class AdvancedWorkerQueue implements WorkerQueue<Boolean> {
	private static Logger LOG = Logger.getLogger(AdvancedWorkerQueue.class); 
	
    private final BlockingQueue<Runnable> sWorkQueue;
    private final ThreadFactory sThreadFactory;
    private final ThreadPoolExecutor sExecutor;
	
    public AdvancedWorkerQueue(int coreSize, int maxSize, int keepAliveTime) { 
		sWorkQueue = new LinkedBlockingQueue<Runnable>(10); 
		sThreadFactory = new ThreadFactory() {
		        private final AtomicInteger mCount = new AtomicInteger(1);
		        @Override
		        public Thread newThread(Runnable r) {
		        	final String threadName = "AdvancedWorkerThread-" + mCount.getAndIncrement(); 
		        	
		        	if (LOG.isDebugEnabled())
		        		LOG.debug("AdvancedWorkerQueue.newThread: "+threadName); 
		        	
		            return new Thread(r, threadName);
		        }
		    };
	    sExecutor = new ThreadPoolExecutor(coreSize,
	    	maxSize, keepAliveTime, TimeUnit.SECONDS, sWorkQueue, sThreadFactory);
	}
	
    public final ThreadPoolExecutor getExecutor() { 
    	return sExecutor; 
    }
    
	@Override 
	public boolean execute(WorkerTask<Boolean> task) throws InterruptedException { 
		if (task == null) return false; 
		
		getExecutor().execute(new FutureTask<Boolean>(task)); 
		
		return true; 
	}
	
	@Override 
	public void stopWorkers() throws InterruptedException { 
		// do nothing
	}
	
}
