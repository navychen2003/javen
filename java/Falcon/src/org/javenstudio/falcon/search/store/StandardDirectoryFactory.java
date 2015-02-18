package org.javenstudio.falcon.search.store;

import java.io.File;
import java.io.IOException;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.hornet.store.local.LocalFS;

/**
 * Directory provider which mimics original 
 * {@link FSDirectory} based behavior.
 * 
 */
public class StandardDirectoryFactory extends CachingDirectoryFactory {

	@Override
	protected IDirectory create(String path) throws IOException {
		return LocalFS.open(new File(path));
	}
  
	@Override
	public String normalize(String path) throws IOException {
		return new File(path).getCanonicalPath();
	}
	
}
