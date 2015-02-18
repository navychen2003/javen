package org.javenstudio.falcon.search.hits;

/**
 * <b>This API is experimental and subject to change</b>
 */
public class ShardFacetCount {
	
    private final String mName;
    // the indexed form of the name... used for comparisons.
    private String mIndexed; 
    private long mCount;
    // term number starting at 0 (used in bit arrays)
    private int mTermNum; 

    public ShardFacetCount(String name) { 
    	mName = name; 
    }
    
    public String getName() { return mName; }
    
    public void setIndexedName(String form) { mIndexed = form; }
    public String getIndexedName() { return mIndexed; }
    
    public void setCount(long count) { mCount = count; }
    public long getCount() { return mCount; }
    
    public void setTermNum(int num) { mTermNum = num; }
    public int getTermNum() { return mTermNum; }
    
    @Override
    public String toString() {
    	return "ShardFacetCount{term=" + mName + ",termNum=" + mTermNum + ",count=" + mCount + "}";
    }
    
}
