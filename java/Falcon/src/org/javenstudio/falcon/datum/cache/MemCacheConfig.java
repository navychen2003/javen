package org.javenstudio.falcon.datum.cache;

import java.lang.reflect.Constructor;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;

public class MemCacheConfig {
	static final Logger LOG = Logger.getLogger(MemCacheConfig.class);
	  
	private Class<? extends MemCache> mClazz;
	private Map<String,String> mArgs;

	public MemCacheConfig() {}

	public MemCacheConfig(Class<? extends MemCache> clazz, 
			Map<String,String> args) {
		mClazz = clazz;
		mArgs = args;
		
		if (clazz == null || args == null) 
			throw new NullPointerException("SearchCache class or args is null");
	}

	public Map<String,String> getCacheArgs() { return mArgs; }
	public void setCacheArgs(Map<String,String> args) { mArgs = args; }
	
	public Class<? extends MemCache> getCacheClass() { return mClazz; }
	public void setCacheClass(Class<? extends MemCache> clazz) { mClazz = clazz; }
	
	public MemCache newInstance() throws ErrorException {
		try {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Creating cache: " + mClazz.getName() 
						+ " with args: " + mArgs);
			}
			
			Constructor<? extends MemCache> ctor = mClazz.getConstructor();
			MemCache cache = ctor.newInstance();
			cache.init(mArgs);
			return cache;
			
		} catch (Throwable e) {
			if (e instanceof ErrorException) 
				throw (ErrorException)e;
			else
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}

}
