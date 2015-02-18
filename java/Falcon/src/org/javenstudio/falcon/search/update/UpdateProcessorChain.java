package org.javenstudio.falcon.search.update;

import java.util.ArrayList;
import java.util.List;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.PluginInfo;
import org.javenstudio.falcon.util.PluginInfoInitialized;
import org.javenstudio.falcon.search.ISearchCore;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.ISearchResponse;

/**
 * Manages a chain of UpdateRequestProcessorFactories.
 * <p>
 * Chain can be configured via config.xml:
 * </p>
 * <pre class="prettyprint">
 * &lt;updateRequestProcessors name="key" default="true"&gt;
 *   &lt;processor class="PathToClass1" /&gt;
 *   &lt;processor class="PathToClass2" /&gt;
 *   &lt;processor class="lightning.LogUpdateProcessorFactory" &gt;
 *     &lt;int name="maxNumToLog"&gt;100&lt;/int&gt;
 *   &lt;/processor&gt;
 *   &lt;processor class="lightning.RunUpdateProcessorFactory" /&gt;
 * &lt;/updateRequestProcessors&gt;
 * </pre>
 * <p>
 * Allmost all processor chains should end with an instance of 
 * {@link IndexProcessorFactory} unless the user is explicitly 
 * executing the update commands in an alternative custom 
 * <code>UpdateRequestProcessorFactory</code>.
 * </p>
 *
 * @see UpdateProcessorFactory
 * @see #init
 * @see #createProcessor
 * @since 1.3
 */
public final class UpdateProcessorChain implements PluginInfoInitialized {
  	//private static Logger LOG = Logger.getLogger(UpdateRequestProcessorChain.class);

	public static UpdateProcessorChain createDefaultChain(ISearchCore core) { 
		// construct the default chain
		UpdateProcessorFactory[] factories = new UpdateProcessorFactory[]{
				//new LogUpdateProcessorFactory(),
				//new DistributedUpdateProcessorFactory(),
				new IndexProcessorFactory(core)
			};
		
		return new UpdateProcessorChain(core, factories);
	}
	
  	private UpdateProcessorFactory[] mChain;
  	private final ISearchCore mCore;

  	public UpdateProcessorChain(ISearchCore core) {
  		mCore = core;
  	}

  	/**
  	 * Initializes the chain using the factories specified by the <code>PluginInfo</code>.
  	 * if the chain includes the <code>RunUpdateProcessorFactory</code>, but 
  	 * does not include an implementation of the 
  	 * <code>DistributingUpdateProcessorFactory</code> interface, then an 
  	 * instance of <code>DistributedUpdateProcessorFactory</code> will be 
  	 * injected immediately prior to the <code>RunUpdateProcessorFactory</code>.
  	 *
  	 * @see DistributingUpdateProcessorFactory
  	 * @see IndexProcessorFactory
  	 * @see DistributedUpdateProcessorFactory
  	 */
  	@Override
  	public void init(PluginInfo info) throws ErrorException {
  		final String infomsg = "UpdateProcessorChain \"" + 
  				(info.getName() != null ? info.getName() : "") + "\"" + 
  				(info.isDefault() ? " (default)" : "");

  		// wrap in an ArrayList so we know we know we can do fast index lookups 
  		// and that add(int,Object) is supported
  		List<UpdateProcessorFactory> list = new ArrayList<UpdateProcessorFactory>(
  				mCore.initPlugins(info.getChildren("processor"), UpdateProcessorFactory.class));

  		if (list.isEmpty()){
  			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR,
  					infomsg + " require at least one processor");
  		}

  		int numDistrib = 0;
  		int runIndex = -1;
  		
  		// hi->lo incase multiple run instances, add before first one
  		// (no idea why someone might use multiple run instances, but just in case)
  		for (int i = list.size()-1; 0 <= i; i--) {
  			UpdateProcessorFactory factory = list.get(i);
  			//if (factory instanceof DistributingUpdateProcessorFactory) {
  			//	numDistrib ++;
  			//}
  			if (factory instanceof IndexProcessorFactory) {
  				runIndex = i;
  			}
  		}
  		
  		if (numDistrib > 1) {
  			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR,
  					infomsg + " may not contain more then one instance of DistributingUpdateProcessorFactory");
  		}
  		
  		if (runIndex >= 0 && numDistrib == 0) {
  			// by default, add distrib processor immediately before run
  			//DistributedUpdateProcessorFactory distrib 
  			//	= new DistributedUpdateProcessorFactory();
  			//distrib.init(new NamedList());
  			//list.add(runIndex, distrib);

  			//LOG.info("inserting DistributedUpdateProcessorFactory into " + infomsg);
  		}

  		mChain = list.toArray(new UpdateProcessorFactory[list.size()]); 
  	}

  	/**
  	 * Creates a chain backed directly by the specified array. Modifications to 
  	 * the array will affect future calls to <code>createProcessor</code>
  	 */
  	public UpdateProcessorChain(ISearchCore core, UpdateProcessorFactory[] chain) {
  		mChain = chain;
  		mCore =  core;
  	}

  	/**
  	 * Uses the factories in this chain to creates a new 
  	 * <code>UpdateRequestProcessor</code> instance specific for this request.  
  	 * If the <code>DISTRIB_UPDATE_PARAM</code> is present in the request and is 
  	 * non-blank, then any factory in this chain prior to the instance of 
  	 * <code>{@link DistributingUpdateProcessorFactory}</code> will be skipped, 
  	 * and the <code>UpdateRequestProcessor</code> returned will be from that 
  	 * <code>DistributingUpdateProcessorFactory</code>
  	 *
  	 * @see UpdateProcessorFactory#getInstance
  	 * @see DistributingUpdateProcessorFactory#DISTRIB_UPDATE_PARAM
  	 */
  	public UpdateProcessor createProcessor(ISearchRequest req, ISearchResponse rsp) 
  			throws ErrorException {
  		UpdateProcessor processor = null;
  		UpdateProcessor last = null;
    
  		//final String distribPhase = req.getParams().get(DistributingUpdateProcessorFactory.DISTRIB_UPDATE_PARAM, "");
  		//final boolean skipToDistrib = !distribPhase.trim().isEmpty();

  		for (int i = mChain.length-1; i>=0; i--) {
  			processor = mChain[i].getInstance(req, rsp, last);
  			last = processor == null ? last : processor;
  			//if (skipToDistrib && mChain[i] instanceof DistributingUpdateProcessorFactory) 
  			//	break;
  		}
    
  		return last;
  	}

  	/**
  	 * Returns the underlying array of factories used in this chain.  
  	 * Modifications to the array will affect future calls to 
  	 * <code>createProcessor</code>
  	 */
  	public UpdateProcessorFactory[] getFactories() {
  		return mChain;
  	}

}
