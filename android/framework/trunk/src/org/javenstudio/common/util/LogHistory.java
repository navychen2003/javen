package org.javenstudio.common.util;

import java.util.ArrayList;
import java.util.List;

public class LogHistory {

	private static final LogHistory sInstance = new LogHistory(); 
	public static LogHistory getInstance() { 
		return sInstance; 
	}
	
	public static class LogRecord { 
		public final long mTime = System.currentTimeMillis(); 
		public final int mLevel; 
		public final String mTag; 
		public final String mMessage; 
		public final Throwable mException; 
		
		public LogRecord(int level, String tag, String msg, Throwable tr) { 
			mLevel = level; 
			mTag = tag; 
			mMessage = msg; 
			mException = tr; 
		}
		
		@Override 
		public String toString() { 
			StringBuilder sbuf = new StringBuilder(); 
			switch (mLevel) { 
			case Log.LOG_LEVEL_D: 
				sbuf.append('D'); 
				break; 
			case Log.LOG_LEVEL_E: 
				sbuf.append('E'); 
				break; 
			case Log.LOG_LEVEL_I: 
				sbuf.append('I'); 
				break; 
			case Log.LOG_LEVEL_V: 
				sbuf.append('V'); 
				break; 
			case Log.LOG_LEVEL_W: 
				sbuf.append('W'); 
				break; 
			default: 
				sbuf.append('I'); 
				break; 
			}
			sbuf.append('/'); 
			if (mTag != null) 
				sbuf.append(mTag); 
			sbuf.append(": "); 
			sbuf.append(mMessage); 
			if (mException != null) { 
				sbuf.append(" catched exception: "); 
				sbuf.append(mException.toString()); 
			}
			return sbuf.toString(); 
		}
	}
	
	private final static int MAX_LINES = 10; 
	private final List<LogRecord> mHistorys = new ArrayList<LogRecord>(); 
	private final ListenerObservable mChangeObservable = new ListenerObservable(); 
	
	private LogHistory() {}
	
	public static void registerListener(ListenerObservable.OnChangeListener l) { 
		getInstance().mChangeObservable.registerListener(l); 
	}
	
	public static void unregisterListener(ListenerObservable.OnChangeListener l) { 
		getInstance().mChangeObservable.unregisterListener(l); 
	}
	
	public void log(int level, String tag, String msg) { 
		log(level, tag, msg, null); 
	}
	
	public void log(int level, String tag, String msg, Throwable tr) { 
		synchronized (this) { 
			while (mHistorys.size() >= MAX_LINES) { 
				mHistorys.remove(0); 
			}
			mHistorys.add(new LogRecord(level, tag, msg, tr)); 
		}
		
		mChangeObservable.notifyChange(0, null); 
	}
	
	public synchronized int size() { 
		return mHistorys.size(); 
	}
	
	public synchronized LogRecord get(int position) { 
		return position >= 0 && position < mHistorys.size() ? mHistorys.get(position) : null; 
	}
	
}
