package org.javenstudio.android.data.image;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.worker.work.WorkQueue;

public class ImageTask {

	private static final WorkQueue sWorkQueue = 
			new WorkQueue(ImageTask.class.getSimpleName());
	
	public interface ExceptionListener { 
		public void onException(Throwable e);
	}
	
	public static void runTask(final String name, final Runnable command) { 
		runTask(name, command, (ExceptionListener)null);
	}
	
	public static void runTask(final String name, final Runnable command, 
			final ExceptionListener listener) { 
		if (command == null) throw new NullPointerException();
		
		sWorkQueue.put(new WorkQueue.Command(name) {
				@Override
				public void run() {
					command.run();
				}
				@Override
				public void onException(Throwable e) { 
					if (listener != null) 
						listener.onException(e);
				}
			});
	}
	
	public static void postHandler(final Runnable command) { 
		ResourceHelper.getHandler().post(command);
	}
	
}
