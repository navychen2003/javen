package org.javenstudio.lightning.logging;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.javenstudio.falcon.util.ResultItem;
import org.javenstudio.falcon.util.Throwables;

final class DefaultLogWatcher extends LogWatcher<LogRecord> {

	private final String mName;
	private RecordHandler mHandler = null;
  
	public DefaultLogWatcher() { 
		this(java.util.logging.Logger.class.getName());
	}
	
	public DefaultLogWatcher(String name) {
		mName = name;
	}
  
	@Override
	public String getName() {
		return "DefaultLogger (" + mName + ")";
	}

	@Override
	public List<String> getAllLevels() {
		return Arrays.asList(
				Level.FINEST.getName(),
				Level.FINER.getName(),
				Level.FINE.getName(),
				Level.CONFIG.getName(),
				Level.INFO.getName(),
				Level.WARNING.getName(),
				Level.SEVERE.getName(),
				Level.OFF.getName());
	}

	@Override
	public void setLogLevel(String category, String level) {
		if (LoggerInfo.ROOT_NAME.equals(category)) 
			category = "";
    
		Logger log = LogManager.getLogManager().getLogger(category);
		
		if (level == null || "unset".equalsIgnoreCase(level) || 
			"null".equalsIgnoreCase(level)) {
			if (log != null) 
				log.setLevel(null);
			
		} else {
			if (log == null) 
				log = Logger.getLogger(category); // create it
      
			log.setLevel(Level.parse(level));
		}
	}

	@Override
	public Collection<LoggerInfo> getAllLoggers() {
		LogManager manager = LogManager.getLogManager();

		Logger root = manager.getLogger("");
		
		Map<String,LoggerInfo> map = new HashMap<String,LoggerInfo>();
		Enumeration<String> names = manager.getLoggerNames();
		
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			Logger logger = Logger.getLogger(name);
			
			if (logger == root) 
				continue;
      
			map.put(name, new DefaultLoggerInfo(name, logger));

			while (true) {
				int dot = name.lastIndexOf(".");
				if (dot < 0)
					break;
				
				name = name.substring(0, dot);
				if (!map.containsKey(name)) 
					map.put(name, new DefaultLoggerInfo(name, null));
			}
		}
		
		map.put(LoggerInfo.ROOT_NAME, 
				new DefaultLoggerInfo(LoggerInfo.ROOT_NAME, root));
		
		return map.values();
	}

	@Override
	public void setThreshold(String level) {
		if (mHandler == null) 
			throw new IllegalStateException("Must have an handler");
    
		mHandler.setLevel(Level.parse(level));
	}

	@Override
	public String getThreshold() {
		if (mHandler == null) 
			throw new IllegalStateException("Must have an handler");
		
		return mHandler.getLevel().toString();
	}

	@Override
	public void registerListener(ListenerConfig cfg) {
		if (mHistory != null) 
			throw new IllegalStateException("History already registered");
    
		mHistory = new CircularList<LogRecord>(cfg.getSize());
		mHandler = new RecordHandler(this);
		
		if (cfg.getThreshold() != null) 
			mHandler.setLevel(Level.parse(cfg.getThreshold()));
		else 
			mHandler.setLevel(Level.WARNING);
    
		Logger log = LogManager.getLogManager().getLogger("");
		log.addHandler(mHandler);
	}

	@Override
	public long getTimestamp(LogRecord event) {
		return event.getMillis();
	}
	
	@Override
	public ResultItem toResult(LogRecord event) {
		ResultItem doc = new ResultItem();
		doc.setField("time", new Date(event.getMillis()));
		doc.setField("level", event.getLevel().toString());
		doc.setField("logger", event.getLoggerName());
		doc.setField("message", event.getMessage().toString());
		
		Throwable t = event.getThrown();
		if (t != null) 
			doc.setField("trace", Throwables.getStackTraceAsString(t));
		
		return doc;
	}
	
}
