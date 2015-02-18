package org.javenstudio.falcon.search;

import org.javenstudio.falcon.ErrorException;

public interface SearchWriterCloser {

	public void closeWriter(SearchWriter writer) throws ErrorException;
	
}
