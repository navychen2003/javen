package org.javenstudio.android.mail.controller;

import android.os.Process;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.javenstudio.android.mail.Preferences;
import org.javenstudio.cocoka.net.SocketHelper;
import org.javenstudio.cocoka.worker.WorkerTask;
import org.javenstudio.common.util.Logger;

public class MessagingQueue implements Runnable {
	private static Logger LOG = Logger.getLogger(MessagingQueue.class);

	public static final String COMMAND_SYNCHRONIZEFOLDERS = "SynchronizFolders"; 
	public static final String COMMAND_SYNCHRONIZEMAILBOX = "SynchronizeMailbox"; 
	public static final String COMMAND_SYNCHRONIZEACTIONS = "SynchronizActions"; 
	public static final String COMMAND_CHECKMAIL = "CheckMail"; 
	public static final String COMMAND_FETCHMESSAGEBODY = "FetchMessageBody"; 
	public static final String COMMAND_FETCHMESSAGEATTACHMENT = "FetchMessageAttachment"; 
	public static final String COMMAND_SENDMESSAGE = "SendMessage"; 
	
	static class Command {
        public WorkerTask<Boolean> task;
        public CallbackMessagingListener listener;
        public String description;

        @Override
        public String toString() {
            return description;
        }
    }
	
	private static MessagingQueue sInstance = null; 
	public synchronized static MessagingQueue getInstance() { 
		if (sInstance == null) 
			sInstance = new MessagingQueue(); 
		return sInstance; 
	}
	
	/**
     * All access to mListeners *must* be synchronized
     */
    private final GroupMessagingListener mListeners = new GroupMessagingListener();
    private MessagingEvent mLastEvent = null; 
    private boolean mIsBusy = false;
	
	private final BlockingQueue<Command> mCommands = new LinkedBlockingQueue<Command>();
    private final Thread mQueueThread;
	private final ReentrantLock mCommandLock;
	private final Condition mCommandDone; 
    
    private MessagingQueue() { 
    	mCommandLock = new ReentrantLock(false); 
    	mCommandDone = mCommandLock.newCondition(); 
    	mQueueThread = new Thread(this);
    	mQueueThread.start();
    }
	
    // TODO: seems that this reading of mBusy isn't thread-safe
    public boolean isBusy() {
        return mIsBusy;
    }

    private class CommandThread implements Runnable { 
    	private final Command mCommand;
    	private final Thread mThread;
    	private final long mStartTime;
    	private boolean mDone = false;
    	
    	public CommandThread(Command command) { 
    		mCommand = command;
    		mThread = new Thread(this);
    		mStartTime = System.currentTimeMillis();
    	}
    	
    	final Thread getThread() { return mThread; }
    	final void start() { mThread.start(); }
    	@SuppressWarnings("deprecation")
		final void stop() { mThread.stop(); notifyDone(); }
    	
    	final long getCommandTime() { 
    		long eventTime = mCommand.listener.getLastEventTime();
    		return eventTime > mStartTime ? eventTime : mStartTime;
    	}
    	
    	@Override 
        public final void run() {
            try { 
            	if (LOG.isDebugEnabled()) 
                	LOG.debug("MessagingQueue: starting command: "+mCommand.toString()); 
            	
            	mCommand.task.call(); 
            	
            	if (LOG.isDebugEnabled()) 
                	LOG.debug("MessagingQueue: finished command: "+mCommand.toString()); 
            } catch (Exception e) { 
            	LOG.error("MessagingQueue: run command: "+mCommand.toString()+" error", e); 
            }
            
            notifyDone();
    	}
    	
    	final void notifyDone() {
    		final ReentrantLock lock = mCommandLock;
    		lock.lock();
    		try { 
	            mDone = true;
	            mCommandDone.signal();
    		} finally { 
    			lock.unlock();
    		}
    	}
    	
    	final void waitForDone() { 
    		try { 
        		final ReentrantLock lock = mCommandLock;
        		lock.lockInterruptibly();
        		try {
        			try {
        				while (!mDone) { 
        					if (checkCommandTimeout(this)) { 
        						stop(); LOG.warn("MessagingQueue: stopped command: "+mCommand+" that timeout");
        						break;
        					}
        					mCommandDone.await(); 
        				}
        			} catch (InterruptedException ie) {
        				mCommandDone.signal(); // propagate to non-interrupted thread
        				throw ie; 
        			}
        		} finally {
        			lock.unlock();
        		}
        	} catch (InterruptedException ie) { 
        		LOG.warn("MessagingQueue: wait for command done error", ie);
        	}
    	}
    }
    
    private boolean checkCommandTimeout(CommandThread command) { 
    	final Preferences pref = Preferences.getPreferences();
    	
    	if (SocketHelper.hasConnectingSocketOverTime(command.getThread(), pref.getMessagingConnectTimeout()))
    		return true;
    	
    	final long current = System.currentTimeMillis();
    	if (current - command.getCommandTime() > pref.getMessagingCommandTimeout())
    		return true;
    	
    	return false;
    }
    
    private void checkCommandStatus() { 
    	final ReentrantLock lock = mCommandLock;
		lock.lock();
		try {
			if (LOG.isDebugEnabled()) 
	        	LOG.debug("MessagingQueue: check running command status"); 
			
			mCommandDone.signal();
		} finally { 
			lock.unlock();
		}
    }
    
    @Override 
    public final void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        // TODO: add an end test to this infinite loop
        LOG.info("MessagingQueue: thread started"); 
        while (true) {
            Command command;
            try {
                command = mCommands.take();
            } catch (InterruptedException e) {
                continue; //re-test the condition on the eclosing while
            }
            if (command.listener != null) {
            	mIsBusy = true;
            	try { 
	            	final CommandThread thread = new CommandThread(command);
	            	thread.start();
	            	thread.waitForDone();
            	} catch (Exception ignore) { 
            		// ignore
            	}
            	mIsBusy = false;
            }
        }
    }

    public synchronized void putCommand(final CallbackMessagingListener listener, final Runnable runnable) {
    	if (listener == null || runnable == null) 
    		return;
    	
    	checkCommandStatus();
    	
        try {
        	final WorkerTask.WorkerInfo workerInfo = new WorkerTask.WorkerInfo() { 
	        		public String getName() { 
	        			return MessagingQueue.class.getName() + "$" + listener.getCommand() + ":" + 
	        					listener.getAccount().getEmailAddress(); 
	        		}
	        		public Object getData() { 
	        			return listener; 
	        		}
	        	};
        	
	        Command[] commands = mCommands.toArray(new Command[0]);
	        for (int i=0; commands != null && i < commands.length; i++) { 
	        	Command cmd = commands[i]; 
	        	if (cmd != null && cmd.listener.getCommandKey().equals(listener.getCommandKey())) { 
	        		if (LOG.isDebugEnabled()) 
	        			LOG.debug("MessagingQueue: command already queued: " + listener.getCommandKey());
	        		return;
	        	}
	        }
	        
            Command command = new Command();
            command.listener = listener;
            command.description = listener.getCommand();
            command.task = new WorkerTask<Boolean>() { 
	            	public WorkerTask.WorkerInfo getWorkerInfo() { 
	            		return workerInfo; 
	            	}
	            	protected Boolean onCall() throws Exception { 
	            		if (runnable != null) runnable.run(); 
	            		return true; 
	            	}
	            };
            
	        listener.setWorkerTask(command.task); 
	        
            mCommands.add(command);
            
            if (LOG.isDebugEnabled()) 
            	LOG.debug("MessagingQueue: new command queued: " + workerInfo.getName()); 
            
        } catch (IllegalStateException ie) {
            throw new Error(ie);
        }
    }

    public void registerListener(MessagingListener listener) {
        mListeners.registerListener(listener);
    }

    public void unregisterListener(MessagingListener listener) {
        mListeners.unregisterListener(listener);
    }

    public synchronized void notifyEvent(MessagingEvent event) { 
    	mLastEvent = event; 
    	mListeners.notifyEvent(event); 
    }
    
    public synchronized MessagingEvent getLastEvent() { 
    	return mLastEvent; 
    }
    
}
