package org.javenstudio.falcon.search;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IDirectoryReader;
import org.javenstudio.falcon.ErrorException;

/**
 * Default IndexReaderFactory implementation. Returns a standard
 * {@link DirectoryReader}.
 * 
 * @see DirectoryReader#open(Directory)
 */
public class DefaultReaderFactory extends IndexReaderFactory {
  
	@Override
	public IDirectoryReader newReader(IDirectory indexDir, 
			ISearchCore core) throws ErrorException {
		try {
			return SearchHelper.openDirectory(indexDir, core.getIndexFormat());
		} catch (IOException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
	}
	
}
