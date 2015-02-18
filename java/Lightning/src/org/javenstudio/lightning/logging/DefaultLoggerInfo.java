package org.javenstudio.lightning.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

final class DefaultLoggerInfo extends LoggerInfo {

	private final Logger mLogger;

	public DefaultLoggerInfo(String name, Logger logger) {
		super(name);
		mLogger = logger;
	}

	@Override
	public String getLevel() {
		if (mLogger == null) 
			return null;
    
		Level level = mLogger.getLevel();
		if (level != null) 
			return level.getName();
    
		for (Level l : DefaultLogger.LEVELS) {
			if (l == null) {
				// avoid NPE
				continue;
			}
			
			if (mLogger.isLoggable(l)) {
				// return first level loggable
				return l.getName();
			}
		}
		
		return Level.OFF.getName();
	}
  
	@Override
	public boolean isSet() {
		return (mLogger != null && mLogger.getLevel() != null);
	}
	
}
