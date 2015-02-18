package org.javenstudio.falcon.search.update;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.ISearchResponse;
import org.javenstudio.falcon.util.ContentStream;
import org.javenstudio.falcon.util.Params;

/**
 * Load a {@link ContentStream} into Lightning
 *
 * This should be thread safe and can be called from multiple threads
 */
public abstract class ContentLoader {

	/**
	 * This should be called once for each RequestHandler
	 */
	public ContentLoader init(Params args) throws ErrorException {
		return this;
	}
	  
	public String getDefaultWT() {
		return null;
	}

	/**
	 * Loaders are responsible for closing the stream
	 *
	 * @param req The input {@link QueryRequest}
	 * @param rsp The response, in case the Loader wishes to add anything
	 * @param stream The {@link ContentStream} to add
	 */
	public abstract void load(ISearchRequest req, ISearchResponse rsp, ContentStream stream, 
			UpdateProcessor processor) throws ErrorException;

}
