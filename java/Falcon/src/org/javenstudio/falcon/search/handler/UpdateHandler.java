package org.javenstudio.falcon.search.handler;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.ISearchCore;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.update.ContentLoader;
import org.javenstudio.falcon.search.update.ContentLoaders;
import org.javenstudio.falcon.search.update.UpdateProcessor;
import org.javenstudio.falcon.util.NamedList;

/**
 * UpdateHandler that uses content-type to pick the right Loader
 */
public class UpdateHandler extends UpdateHandlerBase {
	//private static Logger LOG = Logger.getLogger(UpdateRequestHandler.class);

	private final ContentLoaders mLoaders;

	public UpdateHandler(ISearchCore core) { 
		super(core);
		mLoaders = new ContentLoaders(core);
	}
	
	@Override
	public void init(NamedList<?> args) throws ErrorException {
		super.init(args);
    
		mLoaders.initLoaders(args);
	}

	@Override
	protected ContentLoader newLoader(ISearchRequest req, UpdateProcessor processor) {
		return mLoaders;
	}
	
}
