package org.javenstudio.falcon.search.store;

import java.io.File;
import java.io.IOException;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.hornet.store.local.NIOFSDirectory;

/**
 * Factory to instantiate {@link NIOFSDirectory}
 *
 **/
public class NIOFSDirectoryFactory extends CachingDirectoryFactory {

	@Override
	protected IDirectory create(String path) throws IOException {
		return new NIOFSDirectory(new File(path));
	}
	
}
