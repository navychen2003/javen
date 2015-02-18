package org.javenstudio.falcon.search.handler;

import java.io.IOException;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.ISearchCore;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.ISearchResponse;
import org.javenstudio.falcon.search.params.UpdateParams;
import org.javenstudio.falcon.search.update.ContentLoader;
import org.javenstudio.falcon.search.update.UpdateHelper;
import org.javenstudio.falcon.search.update.UpdateProcessor;
import org.javenstudio.falcon.search.update.UpdateProcessorChain;
import org.javenstudio.falcon.util.ContentStream;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.Params;

/**
 * Shares common code between various handlers that manipulate 
 * {@link ContentStream} objects.
 */
public abstract class UpdateHandlerBase {
  	//private static Logger LOG = Logger.getLogger(ContentStreamHandlerBase.class);

  	protected final ISearchCore mCore;
  	protected boolean mHttpCaching = false;
  
  	protected UpdateHandlerBase(ISearchCore core) { 
  		mCore = core;
  	}
  	
  	public ISearchCore getSearchCore() { 
  		return mCore;
  	}
  	
  	public void init(NamedList<?> args) throws ErrorException {
  		// Caching off by default
  		mHttpCaching = false;
  		if (args != null) {
  			Object caching = args.get("httpCaching");
  			if (caching != null) 
  				mHttpCaching = Boolean.parseBoolean(caching.toString());
  		}
  	}
  
  	public void handleRequestBody(ISearchRequest req, ISearchResponse rsp) 
  			throws ErrorException {
  		Params params = req.getParams();
  		
  		UpdateProcessorChain processorChain =
            mCore.getUpdateProcessingChain(params.get(UpdateParams.UPDATE_CHAIN));

  		try {
	  		UpdateProcessor processor = 
	  				processorChain.createProcessor(req, rsp);
	  		
	  		try {
	  			ContentLoader documentLoader = newLoader(req, processor);
	
	  			Iterable<ContentStream> streams = req.getContentStreams();
	  			if (streams == null) {
	  				if (!UpdateHelper.handleCommit(req, processor, params, false) && 
	  					!UpdateHelper.handleRollback(req, processor, params, false)) {
	  					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
	  							"missing content stream");
	  				}
	  				
	  			} else {
	  				for (ContentStream stream : streams) {
	  					documentLoader.load(req, rsp, stream, processor);
	  				}
	
	  				// Perhaps commit from the parameters
	  				UpdateHelper.handleCommit(req, processor, params, false);
	  				UpdateHelper.handleRollback(req, processor, params, false);
	  			}
	  			
	  		} finally {
	  			// finish the request
	  			processor.finish();
	  		}
	  		
  		} catch (IOException ex) { 
  			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
  		}
  	}

  	protected abstract ContentLoader newLoader(ISearchRequest req, 
  			UpdateProcessor processor) throws ErrorException;
  	
}
