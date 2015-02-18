package org.javenstudio.falcon.datum.cache;

import java.util.Map;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.InfoMBean;

public interface MemCache extends InfoMBean {

	public enum State { 
		CREATED, 
		STATICWARMING, 
		AUTOWARMING, 
		LIVE 
	}
	
	public Object init(Map<String, String> args) throws ErrorException;
	
	public String getName();
	
	public Object put(String key, Object value);
	public Object get(String key);
	
	public int size();
	public void clear();
	
	public void setState(State state);
	public State getState();
	
	public void close();
	
}
