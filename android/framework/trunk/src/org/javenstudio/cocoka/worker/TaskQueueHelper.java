package org.javenstudio.cocoka.worker;

import org.javenstudio.cocoka.worker.queue.AdvancedQueueFactory;
import org.javenstudio.cocoka.worker.queue.SimpleWorkerQueue;
import org.javenstudio.cocoka.worker.queue.WorkerTaskImpl;

public class TaskQueueHelper {

	@SuppressWarnings("unused")
	private static final TaskQueueFactory sSimpleFactory = new TaskQueueFactory() { 
			public WorkerQueue<Boolean> createWorkerQueue() { 
				return new SimpleWorkerQueue(Constants.MIN_POOL_SIZE, Constants.MAX_POOL_SIZE); 
			}
			
			public WorkerTask<Boolean> createWorkerTask(PriorityRunnable r) { 
				return new WorkerTaskImpl(r); 
			}
		};
	
	private static final AdvancedQueueFactory sFactory = new AdvancedQueueFactory();
	
	public static AdvancedQueueFactory getDefaultAdvancedQueueFactory() { 
		return sFactory;
	}
	
	public static AdvancedQueueFactory getDefaultFactory() {
		return getDefaultAdvancedQueueFactory();
	}

}
