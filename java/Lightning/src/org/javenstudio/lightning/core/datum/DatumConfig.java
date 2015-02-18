package org.javenstudio.lightning.core.datum;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.cache.MemCacheConfig;
import org.javenstudio.falcon.datum.cache.SimpleLRUCache;
import org.javenstudio.falcon.util.ContextLoader;
import org.javenstudio.lightning.core.CoreAdminConfig;
import org.javenstudio.lightning.core.CoreConfig;

public class DatumConfig extends CoreConfig {
	//private static final Logger LOG = Logger.getLogger(DatumConfig.class);
	
	private final MemCacheConfig mCacheConfig;
	
	public DatumConfig(CoreAdminConfig conf, ContextLoader loader, 
			String name, InputStream is) throws ErrorException {
		super(conf, loader, name, is);
		
		Map<String,String> args = new HashMap<String,String>();
    	args.put("name", "MemoryCache");
    	args.put("size", "2000");
    	args.put("initialSize", "10");
    	args.put("showItems", "-1");
    	
    	mCacheConfig = new MemCacheConfig(SimpleLRUCache.class, args);
	}

	public MemCacheConfig getCacheConfig() { return mCacheConfig; }
	
}
