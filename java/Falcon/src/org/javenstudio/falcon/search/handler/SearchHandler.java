package org.javenstudio.falcon.search.handler;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.ResponseBuilder;
import org.javenstudio.falcon.search.SearchCloseHook;
import org.javenstudio.falcon.search.ISearchCore;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.ISearchResponse;
import org.javenstudio.falcon.search.component.SearchComponents;
import org.javenstudio.falcon.search.shard.ShardHandler;
import org.javenstudio.falcon.search.shard.ShardHandlerFactory;
import org.javenstudio.falcon.util.CommonParams;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.PluginInfo;
import org.javenstudio.falcon.util.PluginInfoInitialized;
import org.javenstudio.falcon.util.RecursiveTimer;

/**
 * TODO: generalize how a comm component can fit into search component framework
 * TODO: statics should be per-core singletons
 */
public class SearchHandler implements PluginInfoInitialized {
	//private static Logger LOG = Logger.getLogger(SearchHandler.class);
	
	private final ISearchCore mCore;
	private final SearchComponents mComponents;
	
	private ShardHandlerFactory mShardHandlerFactory;
	private PluginInfo mShardHandlerInfo;

	public SearchHandler(ISearchCore core) { 
		mCore = core;
		mComponents = new SearchComponents(mCore);
		
		if (core == null) 
			throw new NullPointerException("SearchCore is null");
	}
	
	public final ISearchCore getSearchCore() { return mCore; }
	public final SearchComponents getComponents() { return mComponents; }
	
	@Override
	public void init(PluginInfo info) throws ErrorException {
		init(info.getInitArgs());
		
		for (PluginInfo child : info.getChildren()) {
			if ("shardHandlerFactory".equals(child.getType())){
				mShardHandlerInfo = child;
				break;
			}
		}
	}

	public void init(NamedList<?> args) throws ErrorException { 
		initHandler(args);
	}
	
	/**
	 * Initialize the components based on name. 
	 * Note, if using <code>INIT_FIRST_COMPONENTS</code> or <code>INIT_LAST_COMPONENTS</code>,
	 * then the {@link DebugComponent} will always occur last. 
	 * If this is not desired, then one must explicitly declare all components using
	 * the <code>INIT_COMPONENTS</code> syntax.
	 */
	protected void initHandler(NamedList<?> initArgs) throws ErrorException {
		final ISearchCore core = mCore;
		
		mComponents.initComponents(initArgs);
		
		if (mShardHandlerInfo == null) {
			mShardHandlerFactory = core.getShardHandlerFactory();
      
		} else {
			mShardHandlerFactory = core.createPlugin(mShardHandlerInfo, 
					ShardHandlerFactory.class);
			
			core.addCloseHook(new SearchCloseHook() {
					@Override
					public void preClose(ISearchCore core) {
						mShardHandlerFactory.close();
					}
					@Override
					public void postClose(ISearchCore core) {
					}
				});
		}
	}
  
	public void handleRequestBody(ISearchRequest req, ISearchResponse rsp) 
			throws ErrorException {
		ResponseBuilder rb = new ResponseBuilder(req, rsp, mComponents);
		//if (rb.requestInfo != null) 
		//	rb.requestInfo.setResponseBuilder(rb);

		boolean dbg = req.getParams().getBool(CommonParams.DEBUG_QUERY, false);
		rb.setDebug(dbg);
		if (dbg == false) { 
			//if it's true, we are doing everything anyway.
			//PluginUtils.getDebugInterests(req.getParams().getParams(CommonParams.DEBUG), rb);
		}

		final RecursiveTimer timer = rb.isDebug() ? new RecursiveTimer() : null;

		ShardHandler shardHandler1 = mShardHandlerFactory.getShardHandler();
		shardHandler1.checkDistributed(rb);

		if (timer == null) 
			mComponents.prepare(rb);
		else 
			mComponents.prepare(rb, timer);

		if (!rb.isDistributed()) {
			// a normal non-distributed request

			// The semantics of debugging vs not debugging are different enough that
			// it makes sense to have two control loops
			if (!rb.isDebug()) {
				mComponents.process(rb);
				
			} else {
				mComponents.process(rb, timer);
				timer.stop();

				// add the timing info
				if (rb.isDebugTimings()) 
					rb.addDebugInfo("timing", timer.asNamedList());
			}

		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"distributed search not supported");
		}
	}

}
