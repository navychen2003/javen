package org.javenstudio.lightning.handler.system;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.falcon.util.ResultList;
import org.javenstudio.lightning.logging.LogWatcher;
import org.javenstudio.lightning.logging.LoggerInfo;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

public class LoggingHandler extends AdminHandlerBase {

	private final LogWatcher<?> mWatcher;
	
	public LoggingHandler(LogWatcher<?> watcher) { 
		//if (watcher == null) 
		//	throw new NullPointerException();
		
		mWatcher = watcher;
	}
	
	@Override
	public void handleRequestBody(Request req, Response rsp)
			throws ErrorException {
		checkAuth(req, IUserClient.Op.ACCESS);
		
		final Params params = req.getParams();
		
		// Don't do anything if the framework is unknown
		if (mWatcher == null) {
			rsp.add("error", "Logging Not Initalized");
			return;
		}
		
	    rsp.add("watcher", mWatcher.getName());
	    
	    String threshold = params.get("threshold");
	    if (threshold != null) 
	    	mWatcher.setThreshold(threshold);
	    
	    String[] set = params.getParams("set");
	    if (set != null) {
	    	for (String pair : set) {
	    		String[] split = pair.split(":");
	    		if (split.length != 2) {
	    			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR,
	    					"Invalid format, expected level:value, got " + pair);
	    		}
	    		
	    		String category = split[0];
	    		String level = split[1];

	    		mWatcher.setLogLevel(category, level);
	    	}
	    }
	    
	    String since = req.getParams().get("since");
	    if (since != null) {
	    	long time = -1;
	    	try {
	    		time = Long.parseLong(since);
	    	} catch (Exception ex) {
	    		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
	    				"invalid timestamp: " + since);
	    	}
	    	
	    	AtomicBoolean found = new AtomicBoolean(false);
	    	
	    	ResultList docs = mWatcher.getHistory(time, found);
	    	if (docs == null) {
	    		rsp.add("error", "History not enabled");
	    		return;
	    		
	    	} else {
	    		NamedMap<Object> info = new NamedMap<Object>();
	    		if (time > 0) {
	    			info.add("since", time);
	    			info.add("found", found);
	    		} else {
	    			// show for the first request
	    			info.add("levels", mWatcher.getAllLevels()); 
	    		}
	    		
	    		info.add("last", mWatcher.getLastEvent());
	    		info.add("buffer", mWatcher.getHistorySize());
	    		info.add("threshold", mWatcher.getThreshold());
	        
	    		rsp.add("info", info);
	    		rsp.add("history", docs);
	    	}
	    	
	    } else {
	    	rsp.add("levels", mWatcher.getAllLevels());
	  
	    	List<LoggerInfo> loggers = new ArrayList<LoggerInfo>(mWatcher.getAllLoggers());
	    	Collections.sort(loggers);
	  
	    	List<NamedMap<?>> info = new ArrayList<NamedMap<?>>();
	    	for (LoggerInfo wrap : loggers) {
	    		info.add(wrap.getInfo());
	    	}
	    	
	    	rsp.add("loggers", info);
	    }
	    
	    //rsp.setHttpCaching(false);
	}

}
