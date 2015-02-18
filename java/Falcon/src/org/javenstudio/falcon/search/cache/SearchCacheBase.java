package org.javenstudio.falcon.search.cache;

import java.io.Serializable;
import java.util.Map;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.Searcher;
import org.javenstudio.falcon.util.InfoMBean;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;

/**
 * Common base class of reusable functionality for SearchCaches
 */
public abstract class SearchCacheBase<K,V> implements InfoMBean {
	
	private final Searcher mSearcher;
	
	protected CacheRegenerator<K,V> mRegenerator;
	protected SearchCache.State mState;
	protected AutoWarmCountRef mAutowarm;
	protected String mName;

	protected SearchCacheBase(Searcher searcher) { 
		mSearcher = searcher; 
		
		if (searcher == null) 
			throw new NullPointerException("Searcher is null");
	}
	
	public Searcher getSearcher() { 
		return mSearcher;
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
		
		lst.add("searcher", mSearcher.toString());
		
		return lst;
	}
	
	public void init(Map<String, String> args, 
			CacheRegenerator<K,V> regenerator) throws ErrorException {
		mRegenerator = regenerator;
		mState = SearchCache.State.CREATED;
		mName = (String) args.get("name");
		mAutowarm = new AutoWarmCountRef((String)args.get("autowarmCount"));
		
		if (mName == null) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Param name is empty");
		}
	}
  
	protected String getAutowarmDescription() {
		return "autowarmCount=" + mAutowarm + ", regenerator=" + mRegenerator;
	}
  
	protected boolean isAutowarmingOn() {
		return mAutowarm.isAutoWarmingOn();
	}
  
	public void setState(SearchCache.State state) {
		mState = state;
	}

	public SearchCache.State getState() {
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
