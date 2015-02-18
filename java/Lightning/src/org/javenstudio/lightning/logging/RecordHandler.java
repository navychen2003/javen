package org.javenstudio.lightning.logging;

import java.util.logging.LogRecord;

public final class RecordHandler extends java.util.logging.Handler {
	
	private final LogWatcher<LogRecord> mFramework;
  
	public RecordHandler(LogWatcher<LogRecord> framework) {
		mFramework = framework;
	}
  
	@Override
	public void close() throws SecurityException {
		//history.reset();
	}
  
	@Override
	public void flush() {
		// nothing
	}
  
	@Override
	public void publish(LogRecord r) {
		if (isLoggable(r)) 
			mFramework.add(r, r.getMillis());
	}
	
}
