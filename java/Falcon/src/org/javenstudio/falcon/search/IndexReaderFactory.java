package org.javenstudio.falcon.search;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IDirectoryReader;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedListPlugin;

/**
 * Factory used to build a new IndexReader instance.
 */
public abstract class IndexReaderFactory implements NamedListPlugin {
	
	/**
	 * Potentially initializes {@link #termInfosIndexDivisor}.  Overriding classes should 
	 * call super.init() in order to make sure termInfosIndexDivisor is set.
	 * <p>
	 * <code>init</code> will be called just once, immediately after creation.
	 * <p>
	 * The args are user-level initialization parameters that may be specified
	 * when declaring an indexReaderFactory in config.xml
	 *
	 */
	@Override
	public void init(NamedList<?> args) {
		// do nothing
	}

	/**
	 * Creates a new IndexReader instance using the given Directory.
	 * 
	 * @param indexDir indexDir index location
	 * @param core {@link Core} instance where this reader will be used. NOTE:
	 * this Core instance may not be fully configured yet, but basic things like
	 * {@link Core#getDescriptor()}, {@link Core#getSchema()} and
	 * {@link Core#getConfig()} are valid.
	 * @return An IndexReader instance
	 * @throws IOException If there is a low-level I/O error.
	 */
	public abstract IDirectoryReader newReader(IDirectory indexDir, ISearchCore core)
			throws ErrorException;
	
}
