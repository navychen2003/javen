package org.javenstudio.falcon.search.update;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedListPlugin;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.ISearchResponse;

/**
 * A factory to generate an UpdateRequestProcessor for each request.  
 * 
 * If the factory needs access to {@link Core} in initialization, it could 
 * implement {@link CoreAware}
 * 
 * @since 1.3
 */
public abstract class UpdateProcessorFactory implements NamedListPlugin {
	
	@Override
	public void init(NamedList<?> args) throws ErrorException {
		// could process the Node
	}
  
	public abstract UpdateProcessor getInstance(ISearchRequest req, ISearchResponse rsp, 
			UpdateProcessor next) throws ErrorException;
	
}
