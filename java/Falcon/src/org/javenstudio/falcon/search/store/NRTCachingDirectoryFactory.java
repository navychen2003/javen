package org.javenstudio.falcon.search.store;

import java.io.File;
import java.io.IOException;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.hornet.store.local.LocalFS;
import org.javenstudio.hornet.store.local.NRTCachingDirectory;

/**
 * Factory to instantiate {@link NRTCachingDirectory}
 */
public class NRTCachingDirectoryFactory extends StandardDirectoryFactory {

	@Override
	protected IDirectory create(String path) throws IOException {
		return new NRTCachingDirectory(getContext(), LocalFS.open(new File(path)), 4, 48);
	}

}
