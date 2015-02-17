package org.javenstudio.cocoka.worker.work;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.os.Process;

import org.javenstudio.common.util.Logger;

public final class WorkQueue {
	private static Logger LOG = Logger.getLogger(WorkQueue.class);
	
	private final String mQueueName;
	private final BlockingQueue<Command> mCommands;
    private final Thread mQueueThread;
	
	public WorkQueue(String name) { 
		mQueueName = name;
		mCommands = new LinkedBlockingQueue<Command>();
		mQueueThread = new Thread(new QueueRunnable());
    	mQueueThread.start();
	}
	
	public static abstract class Command implements Runnable { 
		private final String mName;
		public Command(String name) { mName = name; }
		public final String getName() { return mName; }
		public void onException(Throwable e) {}
	}
	
	public static class StopCommand extends Command { 
		public StopCommand() { super("stop"); }
		public void run() {}
	}
	
	private class QueueRunnable implements Runnable {
	    @Override 
	    public final void run() {
	        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
	        // TODO: add an end test to this infinite loop
	        if (LOG.isInfoEnabled())
	        	LOG.info("WorkQueue: " + mQueueName + " thread started"); 
	        
	        boolean stop = false;
	        while (!stop) {
	            try {
	                final Command command = mCommands.take();
	                try { 
	                	if (command != null) {
	                		if (LOG.isDebugEnabled())
	                			LOG.debug("WorkQueue: execute " + command.getName());
	                		
	                		command.run();
	                	}
    				} catch (Exception ex) { 
    					if (command != null)
    						command.onException(ex);
    					
    				} finally { 
    					if (command != null && command instanceof StopCommand) 
    						stop = true;
    				}
	            } catch (InterruptedException e) {
	                continue; //re-test the condition on the eclosing while
	            }
	        }
	    }
	}
	
	public String getQueueName() { 
		return mQueueName;
	}
	
	public void put(Command command) { 
		if (command == null) throw new NullPointerException();
		mCommands.add(command);
	}
	
	public void stop() { 
		put(new StopCommand());
	}
	
}
