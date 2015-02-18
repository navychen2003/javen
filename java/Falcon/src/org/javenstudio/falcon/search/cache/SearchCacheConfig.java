package org.javenstudio.falcon.search.cache;

import java.lang.reflect.Constructor;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.Searcher;

/**
 * Contains the knowledge of how cache config is
 * stored in the config.xml file, and implements a
 * factory to create caches.
 *
 */
@SuppressWarnings("rawtypes")
public class SearchCacheConfig {
	static final Logger LOG = Logger.getLogger(SearchCacheConfig.class);
  
	private Class<? extends SearchCache> mClazz;
	private Map<String,String> mArgs;
	
	private Object[] mPersistence = new Object[1];
	private CacheRegenerator mRegenerator;
	
	private String mNodeName;
	private String mCacheImpl;
	private String mRegenImpl;

	public SearchCacheConfig() {}

	public SearchCacheConfig(Class<? extends SearchCache> clazz, 
			Map<String,String> args, CacheRegenerator<?,?> regenerator) {
		mClazz = clazz;
		mArgs = args;
		mRegenerator = regenerator;
		
		if (clazz == null || args == null) 
			throw new NullPointerException("SearchCache class or args is null");
	}

	public Map<String,String> getCacheArgs() { return mArgs; }
	public void setCacheArgs(Map<String,String> args) { mArgs = args; }
	
	public String getNodeName() { return mNodeName; }
	public void setNodeName(String name) { mNodeName = name; }
	
	public String getCacheImpl() { return mCacheImpl; }
	public void setCacheImpl(String impl) { mCacheImpl = impl; }
	
	public String getRegenImpl() { return mRegenImpl; }
	public void setRegenImpl(String impl) { mRegenImpl = impl; }
	
	public Class<? extends SearchCache> getCacheClass() { return mClazz; }
	public void setCacheClass(Class<? extends SearchCache> clazz) { mClazz = clazz; }
	
	public CacheRegenerator getRegenerator() {
		return mRegenerator;
	}

	public void setRegenerator(CacheRegenerator regenerator) {
		mRegenerator = regenerator;
	}

	@SuppressWarnings("unchecked")
	public SearchCache newInstance(Searcher searcher) throws ErrorException {
		try {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Creating cache: " + mClazz.getName() 
						+ " of searcher: " + searcher + " with args: " + mArgs);
			}
			
			Constructor<? extends SearchCache> ctor = mClazz.getConstructor(Searcher.class);
			SearchCache cache = ctor.newInstance(searcher);
			mPersistence[0] = cache.init(mArgs, mPersistence[0], mRegenerator);
			return cache;
			
		} catch (Throwable e) {
			//if (LOG.isErrorEnabled())
			//	LOG.error("Error instantiating cache", e);
			
			// we can carry on without a cache... but should we?
			// in some cases (like an OOM) we probably should try to continue.
			//return null;
			
			if (e instanceof ErrorException) 
				throw (ErrorException)e;
			else
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}

}
