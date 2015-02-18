package org.javenstudio.falcon.datum.cache;

import java.io.Serializable;
import java.util.Map;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.InfoMBean;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;

public abstract class MemCacheBase implements InfoMBean {
	
	protected MemCache.State mState;
	protected String mName;

	protected MemCacheBase() {
	}
	
	protected String getDefaultName() { 
		return getClass().getName() + "@" + hashCode();
	}
	
	public String getName() { 
		return mName == null ? getDefaultName() : mName;
	}
	
	@Override
	public String getMBeanKey() {
		return getName(); 
	}
	
	@Override
	public String getMBeanName() { 
		return getClass().getName();
	}
	
	@Override
	public String getMBeanDescription() { 
		return getClass().getName();
	}
	
	@Override
	public String getMBeanVersion() {
		return "1.0";
	}

	@Override
	public String getMBeanCategory() {
		return InfoMBean.CATEGORY_CACHE;
	}

	@Override
	public NamedList<?> getMBeanStatistics() {
		NamedList<Serializable> lst = new NamedMap<Serializable>();
		
		//lst.add("searcher", mSearcher.toString());
		
		return lst;
	}
	
	public Object init(Map<String, String> args) throws ErrorException {
		mState = MemCache.State.CREATED;
		mName = (String) args.get("name");
		
		if (mName == null) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Param name is empty");
		}
		
		return null;
	}
  
	public void setState(MemCache.State state) {
		mState = state;
	}

	public MemCache.State getState() {
		return mState;
	}
  
	/**
	 * Returns a "Hit Ratio" (ie: max of 1.00, not a percentage) suitable for 
	 * display purposes.
	 */
	protected static String calcHitRatio(long lookups, long hits) {
		if (lookups == 0) return "0.00";
		if (lookups == hits) return "1.00";
		int hundredths = (int)(hits*100/lookups); // rounded down
		if (hundredths < 10) return "0.0" + hundredths;
		return "0." + hundredths;

		/*** code to produce a percent, if we want it...
    	int ones = (int)(hits*100 / lookups);
    	int tenths = (int)(hits*1000 / lookups) - ones*10;
    	return Integer.toString(ones) + '.' + tenths;
		 ***/
	}
	
}
