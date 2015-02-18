package org.javenstudio.hornet.search.hits;

//Refers to one hit:
final class ShardRef {

	// Which shard (index into shardHits[]):
	protected final int mShardIndex;

	// Which hit within the shard:
	protected int mHitIndex;

	public ShardRef(int shardIndex) {
		mShardIndex = shardIndex;
		mHitIndex = 0;
	}

	public int getShardIndex() { return mShardIndex; }
	public int getHitIndex() { return mHitIndex; }
	
	void setHitIndex(int index) { mHitIndex = index; }
	
	@Override
	public String toString() {
		return "ShardRef(shardIndex=" + mShardIndex + " hitIndex=" + mHitIndex + ")";
	}
	
}
