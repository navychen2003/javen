package org.javenstudio.falcon.search.shard;

import org.javenstudio.falcon.search.ISearchResponse;

public final class ShardResponse {
	
	private ISearchResponse mResponse;
	private ShardRequest mShardRequest;
	
	// the specific shard that this response was received from
	private String mShardAddress; 
	private String mShard;
	
	private int mResponseCode;
	private Throwable mException;
	
	public ShardResponse() {}

	public Throwable getException() { return mException; }
	public void setException(Throwable exception) { mException = exception; }

	public ISearchResponse getResponse() { return mResponse; }
	public void setResponse(ISearchResponse rsp) { mResponse = rsp; }

	public ShardRequest getShardRequest() { return mShardRequest; }
	public void setShardRequest(ShardRequest req) { mShardRequest = req; }
	
	public String getShard() { return mShard; }
	public void setShard(String shard) { mShard = shard; }

	public int getResponseCode() { return mResponseCode; }
	public void setResponseCode(int code) { mResponseCode = code; }

	public String getShardAddress() { return mShardAddress; }
	public void setShardAddress(String addr) { mShardAddress = addr; }
	
	@Override
	public String toString() {
		return "ShardResponse{shard=" + mShard + ",shardAddress=" + mShardAddress
				+ ",request=" + mShardRequest
				+ "}";
	}
	
}
