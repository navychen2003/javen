package org.javenstudio.cocoka.util;

import java.util.HashMap;

import android.os.Handler;

import org.javenstudio.common.util.Logger;

public abstract class TimerRunner implements Runnable {
	private static Logger LOG = Logger.getLogger(TimerRunner.class); 

	private static HashMap<String, TimerRunner> mRunners = 
			new HashMap<String, TimerRunner>(); 
	
	private final Handler mHandler; 
	private final Runnable mRunnable; 
	private final long mDelayMillis; 
	private boolean mStarted; 
	private volatile boolean mInterrupted; 
	
	public TimerRunner(Handler handler, long delayMillis) { 
		mHandler = handler; 
		mDelayMillis = delayMillis; 
		mStarted = false; 
		mInterrupted = false; 
		mRunnable = new Runnable() { 
				public void run() { 
					TimerRunner.this.run(); 
					TimerRunner.this.postRun(); 
				}
			}; 
	}
	
	public String getName() { 
		return getClass().getName(); 
	}
	
	public void interrupt() { 
		mInterrupted = true; 
		if (LOG.isDebugEnabled()) { 
			LOG.debug("TimmerRunnable["+getName()+"] interrupted"); 
		}
	}
	
	public final void restart() { 
		synchronized (this) { 
			if (mStarted && mInterrupted) { 
				mStarted = false; 
				mInterrupted = false; 
			} else if (mStarted) 
				return; 
		}
		start(); 
	}
	
	public final void start() { 
		synchronized (this) { 
			if (mStarted || mInterrupted) 
				return; 
			mStarted = true; 
			
			synchronized (mRunners) { 
				String name = getName(); 
				
				if (mRunners.containsKey(name)) { 
					TimerRunner runner = mRunners.get(name); 
					if (runner != null) 
						runner.interrupt(); 
				}
				
				mRunners.put(name, this); 
			}
		}
		postRun(); 
	}
	
	private void postRun() { 
		if (!mInterrupted && mDelayMillis >= 0) { 
			if (LOG.isDebugEnabled()) { 
				LOG.debug("TimmerRunnable["+getName()+"] run after "+mDelayMillis+" ms"); 
			}
			mHandler.postDelayed(mRunnable, mDelayMillis); 
		} 
	}
	
}
