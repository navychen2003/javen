package org.javenstudio.falcon.search.shard;

import java.util.ArrayList;
import java.util.List;

import org.javenstudio.falcon.util.ModifiableParams;

// todo... when finalized make accessors
public class ShardRequest {
	
	public final static String[] ALL_SHARDS = null;

	public final static int PURPOSE_PRIVATE         = 0x01;
	public final static int PURPOSE_GET_TERM_DFS    = 0x02;
	public final static int PURPOSE_GET_TOP_IDS     = 0x04;
	public final static int PURPOSE_REFINE_TOP_IDS  = 0x08;
	public final static int PURPOSE_GET_FACETS      = 0x10;
	public final static int PURPOSE_REFINE_FACETS   = 0x20;
	public final static int PURPOSE_GET_FIELDS      = 0x40;
	public final static int PURPOSE_GET_HIGHLIGHTS  = 0x80;
	public final static int PURPOSE_GET_DEBUG       = 0x100;
	public final static int PURPOSE_GET_STATS       = 0x200;
	public final static int PURPOSE_GET_TERMS       = 0x400;
	public final static int PURPOSE_GET_TOP_GROUPS  = 0x800;

	/** list of responses... filled out by framework */
	private List<ShardResponse> mResponses = new ArrayList<ShardResponse>();

	/** actual shards to send the request to, filled out by framework */
	private String[] mActualShards;
	
	// the purpose of this request
	private int mPurpose; 

	// the shards this request should be sent to, null for all
	private String[] mShards; 

	private ModifiableParams mParams;

	// TODO: one could store a list of numbers to correlate where returned docs
	// go in the top-level response rather than looking up by id...
	// this would work well if we ever transitioned to using internal ids and
	// didn't require a uniqueId

	public ShardRequest() {}
	
	public int getPurpose() { return mPurpose; }
	public void setPurpose(int p) { mPurpose = p; }
	
	public void setParams(ModifiableParams params) { mParams = params; }
	public ModifiableParams getParams() { return mParams; }
	
	public List<ShardResponse> getResponses() { return mResponses; }
	
	public void setShards(String[] vals) { mShards = vals; }
	
	public int getShardCount() { return mShards != null ? mShards.length : 0; }
	public String getShardAt(int index) { return mShards[index]; }
	
	public void setActualShards(String[] vals) { mActualShards = vals; }
	
	public int getActualShardCount() { return mActualShards != null ? mActualShards.length : 0; }
	public String getActualShardAt(int index) { return mActualShards[index]; }
	
	@Override
	public String toString() {
		return "ShardRequest:{params=" + "" //params
				+ ", purpose=" + Integer.toHexString(mPurpose)
				+ ", nResponses =" + mResponses.size()
				+ "}";
	}
	
}
