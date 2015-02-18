package org.javenstudio.falcon.util.job;

public class AdvancedQueueFactory implements TaskQueueFactory {

	private final int mMinPoolSize; 
	private final int mMaxPoolSize; 
	private final int mKeepAliveSeconds; 
	private AdvancedWorkerQueue sInstance = null; 
	
	public final synchronized AdvancedWorkerQueue getQueue() { 
		if (sInstance == null) { 
			sInstance = new AdvancedWorkerQueue(
					mMinPoolSize, mMaxPoolSize, mKeepAliveSeconds);
		}
		return sInstance;
	}
	
	public AdvancedQueueFactory() { 
		this(Constants.MIN_POOL_SIZE, Constants.MAX_POOL_SIZE, Constants.KEEPALIVE_SECONDS);
	}
	
	public AdvancedQueueFactory(int minPoolSize, int maxPoolSize, int keepAliveSeconds) { 
		mMinPoolSize = minPoolSize; 
		mMaxPoolSize = maxPoolSize; 
		mKeepAliveSeconds = keepAliveSeconds; 
	}
	
	public WorkerQueue<Boolean> createWorkerQueue() { 
		//return new SimpleWorkerQueue(Constants.MIN_POOL_SIZE, Constants.MAX_POOL_SIZE); 
		return getQueue(); 
	}
	
	public WorkerTask<Boolean> createWorkerTask(PriorityRunnable r) { 
		return new WorkerTaskImpl(r); 
	}
	
}
