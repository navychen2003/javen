package org.javenstudio.falcon.search.hits;

import org.javenstudio.common.indexdb.search.FieldDoc;
import org.javenstudio.falcon.util.NamedList;

public class ShardDoc extends FieldDoc {
	
	private String mShard;
	private String mShardAddress;  // TODO
  
    // the position of this doc within the shard... this can be used
    // to short-circuit comparisons if the shard is equal, and can
    // also be used to break ties within the same shard.
	private int mOrderInShard;
	
    // this is currently the uniqueKeyField but
    // may be replaced with internal docid in a future release.
	private Object mId;

	private Float mScore;

	// sort field values for *all* docs in a particular shard.
	// this doc's values are in position orderInShard

	// TODO: store the ResultItem here?
	// Store the order in the merged list for lookup when getting stored fields?
	// (other components need this ordering to store data in order, like highlighting)
	// but we shouldn't expose uniqueKey (have a map by it) until the stored-field
	// retrieval stage.

	private NamedList<?> mSortFieldValues;
	
	// the ordinal position in the merged response arraylist 
	private int mPositionInResponse;

	public ShardDoc(float score, Object[] fields, Object uniqueId, String shard) {
		super(-1, score, fields);
		mId = uniqueId;
		mShard = shard;
	}

	public ShardDoc() {
		super(-1, Float.NaN);
	}

	public String getShard() { return mShard; }
	public void setShard(String shard) { mShard = shard; }
	
	public String getShardAddress() { return mShardAddress; }
	public void setShardAddress(String addr) { mShardAddress = addr; }
	
	public int getOrderInShard() { return mOrderInShard; }
	public void setOrderInShard(int order) { mOrderInShard = order; }
	
	public int getPositionInResponse() { return mPositionInResponse; }
	public void setPositionInResponse(int pos) { mPositionInResponse = pos; }
	
	public Object getId() { return mId; }
	public void setId(Object id) { mId = id; }
	
	public NamedList<?> getSortFieldValues() { return mSortFieldValues; }
	public void setSortFieldValues(NamedList<?> vals) { mSortFieldValues = vals; }
	
	public Float getShardScore() { return mScore; }
	public void setShardScore(Float score) { mScore = score; }
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ShardDoc shardDoc = (ShardDoc) o;

		if (mId != null ? !mId.equals(shardDoc.mId) : shardDoc.mId != null) 
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		return mId != null ? mId.hashCode() : 0;
	}

	@Override
	public String toString(){
		return "ShardDoc{id=" + mId
            + " ,score=" + mScore
            + " ,shard=" + mShard
            + " ,orderInShard=" + mOrderInShard
            + " ,positionInResponse=" + mPositionInResponse
            + " ,sortFieldValues=" + mSortFieldValues 
            + "}";
	}
	
}
