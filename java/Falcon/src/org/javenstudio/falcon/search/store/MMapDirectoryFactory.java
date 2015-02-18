package org.javenstudio.falcon.search.store;

import java.io.File;
import java.io.IOException;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.hornet.store.local.MMapDirectory;

/**
 *  Directly provide MMapDirectory instead of relying on {@link FSDirectory#open}
 *
 * Can set the following parameters:
 * <ul>
 *  <li>unmap -- See {@link MMapDirectory#setUseUnmap(boolean)}</li>
 *  <li>maxChunkSize -- The Max chunk size.  See {@link MMapDirectory#MMapDirectory(File, LockFactory, int)}</li>
 * </ul>
 *
 */
public class MMapDirectoryFactory extends CachingDirectoryFactory {
	private transient static Logger LOG = Logger.getLogger(MMapDirectoryFactory.class);
	
	private boolean mUnmapHack;
	private int mMaxChunk;

	@Override
	public void init(NamedList<?> args) throws ErrorException {
		Params params = Params.toParams(args);
		mMaxChunk = params.getInt("maxChunkSize", MMapDirectory.DEFAULT_MAX_BUFF);
		if (mMaxChunk <= 0)
			throw new IllegalArgumentException("maxChunk must be greater than 0");
		
		mUnmapHack = params.getBool("unmap", true);
	}

	@Override
	protected IDirectory create(String path) throws IOException {
		MMapDirectory mapDirectory = new MMapDirectory(new File(path), null, mMaxChunk);
		try {
			mapDirectory.setUseUnmap(mUnmapHack);
		} catch (Exception e) {
			LOG.warn("Unmap not supported on this JVM, continuing on without setting unmap", e);
		}
		return mapDirectory;
	}
	
}
