package org.javenstudio.falcon.search.shard;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ModifiableParams;
import org.javenstudio.falcon.search.ResponseBuilder;

public abstract class ShardHandler {
	
	public abstract void checkDistributed(ResponseBuilder rb) throws ErrorException;
	public abstract void submit(ShardRequest sreq, String shard, 
			ModifiableParams params) throws ErrorException;
	
	public abstract ShardResponse takeCompletedIncludingErrors() throws ErrorException;
	public abstract ShardResponse takeCompletedOrError() throws ErrorException;
	
	public abstract void cancelAll() throws ErrorException;
	
}
