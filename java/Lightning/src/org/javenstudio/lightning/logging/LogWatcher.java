package org.javenstudio.lightning.logging;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.javenstudio.falcon.util.ResultItem;
import org.javenstudio.falcon.util.ResultList;

/**
 * A Class to monitor Logging events and hold N events in memory
 * 
 * This is abstract so we can support both JUL and Log4j (and other logging platforms)
 */
public abstract class LogWatcher<E> {
  
	protected CircularList<E> mHistory;
	protected long mLast = -1;
  
	/** @return The implementation name */
	public abstract String getName();
  
	/** @return The valid level names for this framework */
	public abstract List<String> getAllLevels();
  
	/** Sets the log level within this framework */
	public abstract void setLogLevel(String category, String level);
  
	/** @return all registered loggers */
	public abstract Collection<LoggerInfo> getAllLoggers();
  
	public abstract void setThreshold(String level);
	public abstract String getThreshold();

	public void add(E event, long timstamp) {
		mHistory.add(event);
		mLast = timstamp;
	}
  
	public long getLastEvent() {
		return mLast;
	}
  
	public int getHistorySize() {
		return (mHistory == null) ? -1 : mHistory.getBufferSize();
	}
  
	public abstract long getTimestamp(E event);
	public abstract ResultItem toResult(E event);
	public abstract void registerListener(ListenerConfig cfg);

	public void reset() {
		mHistory.clear();
		mLast = -1;
	}
	
	public ResultList getHistory(long since, AtomicBoolean found) {
		if (mHistory == null) 
			return null;
    
		ResultList docs = new ResultList();
		Iterator<E> iter = mHistory.iterator();
		
		while (iter.hasNext()) {
			E e = iter.next();
			long ts = getTimestamp(e);
			
			if (ts == since) {
				if (found != null) 
					found.set(true);
			}
			
			if (ts > since) 
				docs.add(toResult(e));
		}
		
		docs.setNumFound(docs.size()); // make it not look too funny
		return docs;
	}
	
}
