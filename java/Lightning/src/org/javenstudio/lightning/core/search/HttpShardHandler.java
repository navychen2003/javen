package org.javenstudio.lightning.core.search;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;

import org.apache.http.client.HttpClient;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.CommonParams;
import org.javenstudio.falcon.util.ModifiableParams;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.falcon.util.StrHelper;
import org.javenstudio.falcon.search.ResponseBuilder;
import org.javenstudio.falcon.search.ISearchCore;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.params.ShardParams;
import org.javenstudio.falcon.search.shard.ShardHandler;
import org.javenstudio.falcon.search.shard.ShardRequest;
import org.javenstudio.falcon.search.shard.ShardResponse;

public class HttpShardHandler extends ShardHandler {

	private HttpShardHandlerFactory mHttpShardHandlerFactory;
	private CompletionService<ShardResponse> mCompletionService;
	
	private Set<Future<ShardResponse>> mPending;
	private Map<String,List<String>> mShardToURLs;
	
	//private HttpClient mHttpClient;

	public HttpShardHandler(HttpShardHandlerFactory httpShardHandlerFactory, 
			HttpClient httpClient) {
		//mHttpClient = httpClient;
		mHttpShardHandlerFactory = httpShardHandlerFactory;
		mCompletionService = new ExecutorCompletionService<ShardResponse>(
				httpShardHandlerFactory.getCommonExecutor());
		mPending = new HashSet<Future<ShardResponse>>();

		// maps "localhost:8983|localhost:7574" to a shuffled 
		// List("http://localhost:8983","http://localhost:7574")
		// This is primarily to keep track of what order we should 
		// use to query the replicas of a shard
		// so that we use the same replica for all phases of 
		// a distributed request.
		mShardToURLs = new HashMap<String,List<String>>();
	}

	public ISearchCore getSearchCore() { 
		throw new UnsupportedOperationException();
	}
	
	// Not thread safe... don't use in Callable.
	// Don't modify the returned URL list.
	private List<String> getURLs(String shard) {
		List<String> urls = mShardToURLs.get(shard);
		if (urls == null) {
			urls = StrHelper.splitSmart(shard, "|", true);

			// convert shard to URL
			for (int i=0; i < urls.size(); i++) {
				urls.set(i, mHttpShardHandlerFactory.getScheme() + urls.get(i));
			}

			//
			// Shuffle the list instead of use round-robin by default.
			// This prevents accidental synchronization where multiple shards could get in sync
			// and query the same replica at the same time.
			//
			if (urls.size() > 1)
				Collections.shuffle(urls, mHttpShardHandlerFactory.getRandom());
			
			mShardToURLs.put(shard, urls);
		}
		
		return urls;
	}

	@Override
	public void submit(final ShardRequest sreq, final String shard, 
			final ModifiableParams params) {
		// do this outside of the callable for thread safety reasons
		final List<String> urls = getURLs(shard);

		Callable<ShardResponse> task = new Callable<ShardResponse>() {
			@Override
			public ShardResponse call() throws Exception {

				ShardResponse srsp = new ShardResponse();
				srsp.setShardRequest(sreq);
				//srsp.setShard(shard);
				
				//SearchResponse ssr = null; //new SimpleResponse();
				//srsp.setResponse(ssr);
				//long startTime = System.currentTimeMillis();

				try {
					params.remove(CommonParams.WT); // use default (currently javabin)
					params.remove(CommonParams.VERSION);

					// Request req = new QueryRequest(Request.METHOD.POST, "/select");
					// use generic request to avoid extra processing of queries
					//QueryRequest req = new QueryRequest(null, params);
					//req.setMethod(ServletMethod.POST);

					// no need to set the response parser as binary is the default
					// req.setResponseParser(new BinaryResponseParser());

					// if there are no shards available for a slice, urls.size()==0
					if (urls.size() == 0) {
						// TODO: what's the right error code here? We should use the same thing when
						// all of the servers for a shard are down.
						throw new ErrorException(ErrorException.ErrorCode.SERVICE_UNAVAILABLE, 
								"no servers hosting shard: " + shard);
					}

					if (urls.size() <= 1) {
						//String url = urls.get(0);
						//srsp.setShardAddress(url);
						//SolrServer server = new HttpSolrServer(url, httpClient);
						//ssr.nl = server.request(req);
					} else {
						//LBHttpSolrServer.Rsp rsp = httpShardHandlerFactory.loadbalancer.request(new LBHttpSolrServer.Req(req, urls));
						//ssr.nl = rsp.getResponse();
						//srsp.setShardAddress(rsp.getServer());
					}
				} catch (Throwable th) {
					srsp.setException(th);
					if (th instanceof ErrorException) 
					  srsp.setResponseCode(((ErrorException)th).getCode());
					else 
					  srsp.setResponseCode(-1);
				}

				//ssr.setElapsedTime(System.currentTimeMillis() - startTime);

				return srsp;
			}
		};

		mPending.add(mCompletionService.submit(task));
	}

	/** 
	 * returns a ShardResponse of the last response correlated with a ShardRequest. 
	 * This won't return early if it runs into an error.  
	 */
	@Override
	public ShardResponse takeCompletedIncludingErrors() throws ErrorException {
		return take(false);
	}

	/** 
	 * returns a ShardResponse of the last response correlated with a ShardRequest,
	 * or immediately returns a ShardResponse if there was an error detected
	 */
	@Override
	public ShardResponse takeCompletedOrError() throws ErrorException {
		return take(true);
	}
  
	private ShardResponse take(boolean bailOnError) throws ErrorException {
		while (mPending.size() > 0) {
			try {
				Future<ShardResponse> future = mCompletionService.take();
				mPending.remove(future);
				
				ShardResponse rsp = future.get();
				if (bailOnError && rsp.getException() != null) 
					return rsp; // if exception, return immediately
				
				// add response to the response list... we do this after the take() and
				// not after the completion of "call" so we know when the last response
				// for a request was received.  Otherwise we might return the same
				// request more than once.
				rsp.getShardRequest().getResponses().add(rsp);
				if (rsp.getShardRequest().getResponses().size() == 
						rsp.getShardRequest().getActualShardCount()) 
					return rsp;
				
			} catch (InterruptedException e) {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
				
			} catch (ExecutionException e) {
				// should be impossible... the problem with catching the exception
				// at this level is we don't know what ShardRequest it applied to
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
						"Impossible Exception", e);
			}
		}
		
		return null;
	}

	@Override
	public void cancelAll() {
		for (Future<ShardResponse> future : mPending) {
			// TODO: any issues with interrupting?  shouldn't be if
			// there are finally blocks to release connections.
			future.cancel(true);
		}
	}
  
	@Override
	public void checkDistributed(ResponseBuilder rb) throws ErrorException {
		ISearchRequest req = rb.getRequest();
		Params params = req.getParams();

		//rb.isDistrib = params.getBool("distrib", getSearchCore().getDescriptor()
		//    .getContainer().isZooKeeperAware());
		String shards = params.get(ShardParams.SHARDS);

		// for back compat, a shards param with URLs like localhost:8983/solr will mean that this
		// search is distributed.
		boolean hasShardURL = shards != null && shards.indexOf('/') > 0;
		rb.setDistributed(hasShardURL | rb.isDistributed());
    
    /*
    if (rb.isDistrib) {
      // since the cost of grabbing cloud state is still up in the air, we grab it only
      // if we need it.
      ClusterState clusterState = null;
      Map<String,Slice> slices = null;
      CoreDescriptor coreDescriptor = req.getCore().getCoreDescriptor();
      CloudDescriptor cloudDescriptor = coreDescriptor.getCloudDescriptor();
      ZkController zkController = coreDescriptor.getCoreContainer().getZkController();

      if (shards != null) {
        List<String> lst = StrHelper.splitSmart(shards, ",", true);
        rb.shards = lst.toArray(new String[lst.size()]);
        rb.slices = new String[rb.shards.length];

        if (zkController != null) {
          // figure out which shards are slices
          for (int i=0; i<rb.shards.length; i++) {
            if (rb.shards[i].indexOf('/') < 0) {
              // this is a logical shard
              rb.slices[i] = rb.shards[i];
              rb.shards[i] = null;
            }
          }
        }
      } else if (zkController != null) {
        // we weren't provided with a list of slices to query, so find the list that will cover the complete index

        clusterState =  zkController.getClusterState();

        // This can be more efficient... we only record the name, even though we
        // have the shard info we need in the next step of mapping slice->shards
        
        // Stores the comma-separated list of specified collections.
        // Eg: "collection1,collection2,collection3"
        String collections = params.get("collection");
        if (collections != null) {
          // If there were one or more collections specified in the query, split
          // each parameter and store as a seperate member of a List.
          List<String> collectionList = StrHelper.splitSmart(collections, ",",
              true);
          
          // First create an empty HashMap to add the slice info to.
          slices = new HashMap<String,Slice>();
          
          // In turn, retrieve the slices that cover each collection from the
          // cloud state and add them to the Map 'slices'.
          for (int i = 0; i < collectionList.size(); i++) {
            String collection = collectionList.get(i);
            //ClientUtils.appendMap(collection, slices, clusterState.getSlices(collection));
          }
        } else {
          // If no collections were specified, default to the collection for
          // this core.
          slices = clusterState.getSlices(cloudDescriptor.getCollectionName());
          if (slices == null) {
            throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
                "Could not find collection:"
                    + cloudDescriptor.getCollectionName());
          }
        }
        
        // Store the logical slices in the ResponseBuilder and create a new
        // String array to hold the physical shards (which will be mapped
        // later).
        rb.slices = slices.keySet().toArray(new String[slices.size()]);
        rb.shards = new String[rb.slices.length];

        //rb.slices = new String[slices.size()];
        // for (int i=0; i<rb.slices.length; i++) {
        // rb.slices[i] = slices.get(i).getName();
        // }
      }

      //
      // Map slices to shards
      //
      if (zkController != null) {
        for (int i=0; i<rb.shards.length; i++) {
          if (rb.shards[i] == null) {
            if (clusterState == null) {
              clusterState =  zkController.getClusterState();
              slices = clusterState.getSlices(cloudDescriptor.getCollectionName());
            }
            String sliceName = rb.slices[i];

            Slice slice = slices.get(sliceName);

            if (slice==null) {
              // Treat this the same as "all servers down" for a slice, and let things continue
              // if partial results are acceptable
              rb.shards[i] = "";
              continue;
              // throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, "no such shard: " + sliceName);
            }

            Map<String, Replica> sliceShards = slice.getReplicasMap();

            // For now, recreate the | delimited list of equivalent servers
            Set<String> liveNodes = clusterState.getLiveNodes();
            StringBuilder sliceShardsStr = new StringBuilder();
            boolean first = true;
            for (ZkNodeProps nodeProps : sliceShards.values()) {
              ZkCoreNodeProps coreNodeProps = new ZkCoreNodeProps(nodeProps);
              if (!liveNodes.contains(coreNodeProps.getNodeName())
                  || !coreNodeProps.getState().equals(
                      ZkStateReader.ACTIVE)) continue;
              if (first) {
                first = false;
              } else {
                sliceShardsStr.append('|');
              }
              String url = coreNodeProps.getCoreUrl();
              if (url.startsWith("http://"))
                url = url.substring(7);
              sliceShardsStr.append(url);
            }

            rb.shards[i] = sliceShardsStr.toString();
          }
        }
      }
    }
    String shards_rows = params.get(ShardParams.SHARDS_ROWS);
    if(shards_rows != null) {
      rb.shards_rows = Integer.parseInt(shards_rows);
    }
    String shards_start = params.get(ShardParams.SHARDS_START);
    if(shards_start != null) {
      rb.shards_start = Integer.parseInt(shards_start);
    }*/
		
	}

}
