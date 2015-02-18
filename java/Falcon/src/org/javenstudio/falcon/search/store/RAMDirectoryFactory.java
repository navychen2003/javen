package org.javenstudio.falcon.search.store;

import java.io.File;
import java.io.IOException;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.store.ram.RAMDirectory;

/**
 * Factory to instantiate {@link RAMDirectory}
 */
public class RAMDirectoryFactory extends StandardDirectoryFactory {

	@Override
	protected IDirectory create(String path) throws IOException {
		return new RAMDirectory();
	}
  
	@Override
	public boolean exists(String path) {
		String fullPath = new File(path).getAbsolutePath();
		
		synchronized (this) {
			CacheValue cacheValue = mByPathCache.get(fullPath);
			
			IDirectory directory = null;
			if (cacheValue != null) 
				directory = cacheValue.getDirectory();
			
			if (directory == null) 
				return false;
			else 
				return true;
		}
	}

}
