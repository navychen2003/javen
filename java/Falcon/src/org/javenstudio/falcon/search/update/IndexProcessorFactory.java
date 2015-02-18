package org.javenstudio.falcon.search.update;

import org.javenstudio.falcon.search.ISearchCore;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.ISearchResponse;

/**
 * Executes the update commands using the underlying UpdateHandler.
 * Allmost all processor chains should end with an instance of 
 * <code>RunUpdateProcessorFactory</code> unless the user is explicitly 
 * executing the update commands in an alternative custom 
 * <code>UpdateRequestProcessorFactory</code>
 * 
 * @since 1.3
 * @see DistributingUpdateProcessorFactory
 */
public class IndexProcessorFactory extends UpdateProcessorFactory {
	
	private final ISearchCore mCore;
	
	public IndexProcessorFactory(ISearchCore core) { 
		mCore = core;
	}
	
	@Override
	public UpdateProcessor getInstance(ISearchRequest req, ISearchResponse rsp, 
			UpdateProcessor next) {
		return new IndexProcessor(mCore, req, next);
	}
	
}
