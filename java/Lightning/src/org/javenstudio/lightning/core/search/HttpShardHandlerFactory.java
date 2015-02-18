package org.javenstudio.lightning.core.search;

import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.HttpClient;
import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.DefaultThreadFactory;
import org.javenstudio.falcon.util.ModifiableParams;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.PluginInfo;
import org.javenstudio.falcon.util.PluginInfoInitialized;
import org.javenstudio.falcon.search.shard.ShardHandler;
import org.javenstudio.falcon.search.shard.ShardHandlerFactory;
import org.javenstudio.lightning.http.util.HttpUtil;

public class HttpShardHandlerFactory extends ShardHandlerFactory 
		implements PluginInfoInitialized {
	static final Logger LOG = Logger.getLogger(HttpShardHandlerFactory.class);

	// URL scheme to be used in distributed search.
	static final String INIT_URL_SCHEME = "urlScheme";

	// The core size of the threadpool servicing requests
	static final String INIT_CORE_POOL_SIZE = "corePoolSize";

	// The maximum size of the threadpool servicing requests
	static final String INIT_MAX_POOL_SIZE = "maximumPoolSize";

	// The amount of time idle threads persist for in the queue, before being killed
	static final String MAX_THREAD_IDLE_TIME = "maxThreadIdleTime";

	// If the threadpool uses a backing queue, what is its maximum size (-1) to use direct handoff
	static final String INIT_SIZE_OF_QUEUE = "sizeOfQueue";

	// Configure if the threadpool favours fairness over throughput
	static final String INIT_FAIRNESS_POLICY = "fairnessPolicy";
	
	// We want an executor that doesn't take up any resources if
	// it's not used, so it could be created statically for
	// the distributed search component if desired.
	//
	// Consider CallerRuns policy and a lower max threads to throttle
	// requests at some point (or should we simply return failure?)
	private ThreadPoolExecutor mCommExecutor = new ThreadPoolExecutor(
			0,
			Integer.MAX_VALUE,
			5, TimeUnit.SECONDS, // terminate idle threads after 5 sec
			new SynchronousQueue<Runnable>(),  // directly hand off tasks
			new DefaultThreadFactory("httpShardExecutor")
		);

	private final Random mRandom = new Random();
	private String mScheme = "http://"; //current default values
	private boolean mAccessPolicy = false;
	
	private HttpClient mDefaultClient;
	//LBHttpSolrServer loadbalancer;
	//default values:
	
	private int mSoTimeout = 0; 
	private int mConnectionTimeout = 0; 
	private int mMaxConnectionsPerHost = 20;
	private int mCorePoolSize = 0;
	private int mMaximumPoolSize = Integer.MAX_VALUE;
	private int mKeepAliveTime = 5;
	private int mQueueSize = -1;
	
	public ThreadPoolExecutor getCommonExecutor() { 
		return mCommExecutor; 
	}
	
	public String getScheme() { return mScheme; }
	protected Random getRandom() { return mRandom; }
	
	/**
	 * Get {@link ShardHandler} that uses the default http client.
	 */
	public ShardHandler getShardHandler() {
		return getShardHandler(mDefaultClient);
	}

	/**
	 * Get {@link ShardHandler} that uses custom http client.
	 */
	public ShardHandler getShardHandler(final HttpClient httpClient){
		return new HttpShardHandler(this, httpClient);
	}

	@Override
	public void init(PluginInfo info) throws ErrorException {
		NamedList<?> args = info.getInitArgs();
		
		mSoTimeout = getParameter(args, HttpUtil.PROP_SO_TIMEOUT, mSoTimeout);
		mScheme = getParameter(args, INIT_URL_SCHEME, "http://");
		mScheme = (mScheme.endsWith("://")) ? mScheme : mScheme + "://";
		mConnectionTimeout = getParameter(args, HttpUtil.PROP_CONNECTION_TIMEOUT, mConnectionTimeout);
		mMaxConnectionsPerHost = getParameter(args, HttpUtil.PROP_MAX_CONNECTIONS_PER_HOST, mMaxConnectionsPerHost);
		mCorePoolSize = getParameter(args, INIT_CORE_POOL_SIZE, mCorePoolSize);
		mMaximumPoolSize = getParameter(args, INIT_MAX_POOL_SIZE, mMaximumPoolSize);
		mKeepAliveTime = getParameter(args, MAX_THREAD_IDLE_TIME, mKeepAliveTime);
		mQueueSize = getParameter(args, INIT_SIZE_OF_QUEUE, mQueueSize);
		mAccessPolicy = getParameter(args, INIT_FAIRNESS_POLICY, mAccessPolicy);

		BlockingQueue<Runnable> blockingQueue = (mQueueSize == -1) ?
				new SynchronousQueue<Runnable>(mAccessPolicy) :
					new ArrayBlockingQueue<Runnable>(mQueueSize, mAccessPolicy);

		mCommExecutor = new ThreadPoolExecutor(
				mCorePoolSize,
				mMaximumPoolSize,
				mKeepAliveTime, TimeUnit.SECONDS,
				blockingQueue,
				new DefaultThreadFactory("httpShardExecutor")
			);

		ModifiableParams clientParams = new ModifiableParams();
		clientParams.set(HttpUtil.PROP_MAX_CONNECTIONS_PER_HOST, mMaxConnectionsPerHost);
		clientParams.set(HttpUtil.PROP_MAX_CONNECTIONS, 10000);
		clientParams.set(HttpUtil.PROP_SO_TIMEOUT, mSoTimeout);
		clientParams.set(HttpUtil.PROP_CONNECTION_TIMEOUT, mConnectionTimeout);
		clientParams.set(HttpUtil.PROP_USE_RETRY, false);
		
		mDefaultClient = HttpUtil.createClient(clientParams);

		//try {
		//  loadbalancer = new LBHttpSolrServer(defaultClient);
		//} catch (MalformedURLException e) {
		//  // should be impossible since we're not passing any URLs here
		//  throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		//}
	}

	@SuppressWarnings("unchecked")
	private <T> T getParameter(NamedList<?> initArgs, String configKey, T defaultValue) {
		T toReturn = defaultValue;
		if (initArgs != null) {
			T temp = (T) initArgs.get(configKey);
			toReturn = (temp != null) ? temp : defaultValue;
		}
		
		if (LOG.isDebugEnabled())
			LOG.info("Setting " + configKey + " to: " + toReturn);
		
		return toReturn;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void close() {
		try {
			mDefaultClient.getConnectionManager().shutdown();
		} catch (Throwable e) {
			LOG.error(e.toString(), e);
		}

	    /*try {
	      loadbalancer.shutdown();
	    } catch (Throwable e) {
	      log.error(e.toString(), e);
	    }
	    try {
	      ExecutorUtil.shutdownNowAndAwaitTermination(commExecutor);
	    } catch (Throwable e) {
	    	log.error(e.toString(), e);
	    }*/
	}
	
}
