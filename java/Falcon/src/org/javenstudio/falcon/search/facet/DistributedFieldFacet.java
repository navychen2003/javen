package org.javenstudio.falcon.search.facet;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.hornet.search.OpenBitSet;
import org.javenstudio.falcon.search.ResponseBuilder;
import org.javenstudio.falcon.search.hits.ShardFacetCount;

/**
 * <b>This API is experimental and subject to change</b>
 */
public class DistributedFieldFacet extends FieldFacet {
	
	private Map<String,ShardFacetCount> mCounts = 
			new HashMap<String,ShardFacetCount>(128);
	
	// a List<String> of refinements needed, one for each shard.
    private List<String>[] mToRefine; 

    // a bitset for each shard, keeping track of which terms seen
    private OpenBitSet[] mCounted; 
    
    //private SchemaField mSchemaField; // currently unneeded

    // the max possible count for a term appearing on no list
    private long mMissingMaxPossible;
    
    // the max possible count for a missing term for each shard (indexed by shardNum)
    private long[] mMissingMax;
    
    private ShardFacetCount[] mCountSorted;
    
    private int mTermNum;
    private int mInitialLimit;     // how many terms requested in first phase
    private int mInitialMinCount;  // mincount param sent to each shard
    
    private boolean mNeedRefinements;
    
    public DistributedFieldFacet(ResponseBuilder rb, String facetStr) throws ErrorException {
    	super(rb, facetStr);
    	// sf = rb.req.getSchema().getField(field);
    	mMissingMax = new long[rb.getShardCount()];
    	mCounted = new OpenBitSet[rb.getShardCount()];
    }

    public boolean needRefinements() { return mNeedRefinements; }
    public void setNeedRefinements(boolean need) { mNeedRefinements = need; }
    
    public int getTermNum() { return mTermNum; }
    public long getMissingMaxPossible() { return mMissingMaxPossible; }
    
    public int getInitialLimit() { return mInitialLimit; }
    public void setInitialLimit(int val) { mInitialLimit = val; }
    
    public int getInitialMinCount() { return mInitialMinCount; }
    public void setInitialMinCount(int count) { mInitialMinCount = count; }
    
    public List<String> getRefineListAt(int index) { return mToRefine[index]; }
    public void setRefineListAt(int index, List<String> lst) { mToRefine[index] = lst; }
    public void setRefineLists(List<String>[] lsts) { mToRefine = lsts; }
    
    public OpenBitSet getCountedBitSetAt(int index) { return mCounted[index]; }
    public ShardFacetCount[] getSortedShardFacetCount() { return mCountSorted; }
    
    public ShardFacetCount getShardFacetCount(String name) { 
    	return mCounts.get(name);
    }
    
    public void add(int shardNum, NamedList<?> shardCounts, int numRequested) throws ErrorException {
    	// shardCounts could be null if there was an exception
    	int sz = shardCounts == null ? 0 : shardCounts.size();
    	int numReceived = sz;
    	long last = 0;

    	OpenBitSet terms = new OpenBitSet(mTermNum + sz);

    	for (int i=0; i < sz; i++) {
    		String name = shardCounts.getName(i);
    		long count = ((Number)shardCounts.getVal(i)).longValue();
    		
    		if (name == null) {
    			mMissingCount += count;
    			numReceived --;
    			
    		} else {
    			ShardFacetCount sfc = mCounts.get(name);
    			if (sfc == null) {
    				sfc = new ShardFacetCount(name);
    				//sfc.name = name;
    				sfc.setIndexedName(mFieldType == null ? sfc.getName() : 
    					mFieldType.toInternal(sfc.getName()));
    				sfc.setTermNum(mTermNum ++);
    				
    				mCounts.put(name, sfc);
    			}
    			
    			sfc.setCount(sfc.getCount() + count);
    			terms.fastSet(sfc.getTermNum());
          
    			last = count;
    		}
    	}

    	// the largest possible missing term is initialMincount if we received less
    	// than the number requested.
    	if (numRequested<0 || numRequested != 0 && numReceived < numRequested) 
    		last = mInitialMinCount;

    	mMissingMaxPossible += last;
    	mMissingMax[shardNum] = last;
    	mCounted[shardNum] = terms;
	}

    public ShardFacetCount[] getLexSorted() {
    	ShardFacetCount[] arr = mCounts.values().toArray(new ShardFacetCount[mCounts.size()]);
    	
    	Arrays.sort(arr, new Comparator<ShardFacetCount>() {
    			@Override
	    		public int compare(ShardFacetCount o1, ShardFacetCount o2) {
	    			return o1.getIndexedName().compareTo(o2.getIndexedName());
	    		}
	    	});
    	
    	mCountSorted = arr;
    	
    	return arr;
    }

    public ShardFacetCount[] getCountSorted() {
    	ShardFacetCount[] arr = mCounts.values().toArray(new ShardFacetCount[mCounts.size()]);
    	
    	Arrays.sort(arr, new Comparator<ShardFacetCount>() {
    			@Override
    			public int compare(ShardFacetCount o1, ShardFacetCount o2) {
    				if (o2.getCount() < o1.getCount()) 
    					return -1;
    				else if (o1.getCount() < o2.getCount()) 
    					return 1;
    				else
    					return o1.getIndexedName().compareTo(o2.getIndexedName());
    			}
    		});
    	
    	mCountSorted = arr;
    	
    	return arr;
    }

    // returns the max possible value this ShardFacetCount could have for this shard
    // (assumes the shard did not report a count for this value)
    public long maxPossible(ShardFacetCount sfc, int shardNum) {
    	return mMissingMax[shardNum];
    	// TODO: could store the last term in the shard to tell if this term
    	// comes before or after it.  If it comes before, we could subtract 1
    }
    
}
