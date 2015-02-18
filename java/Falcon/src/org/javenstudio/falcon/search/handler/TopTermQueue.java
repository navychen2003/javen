package org.javenstudio.falcon.search.handler;

import java.util.LinkedList;
import java.util.List;

import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.util.PriorityQueue;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.schema.IndexSchema;
import org.javenstudio.falcon.search.schema.SchemaField;
import org.javenstudio.falcon.util.NamedList;

/**
 * Private internal class that counts up frequent terms
 */
final class TopTermQueue extends PriorityQueue<Object> {
	
	static final int HIST_ARRAY_SIZE = 33;
	
	static class TermInfo {
		private final int mDocFreq;
		private final ITerm mTerm;
		
		TermInfo(ITerm t, int df) {
			mTerm = t;
			mDocFreq = df;
		}
		
		public ITerm getTerm() { return mTerm; }
		public int getDocFreq() { return mDocFreq; }
	}
	
	static class TermHistogram { 
	    
	    private int mBuckets[] = new int[HIST_ARRAY_SIZE];
	    private int mMaxBucket = -1;
	    
	    public void add(int[] buckets) {
	    	for (int idx = 0; idx < buckets.length; ++idx) {
	    		if (buckets[idx] != 0) 
	    			mMaxBucket = idx;
	    	}
	    	for (int idx = 0; idx <= mMaxBucket; ++idx) {
	    		mBuckets[idx] = buckets[idx];
	    	}
	    }
	    
	    // TODO? should this be a list or a map?
	    public NamedList<Integer> toNamedList() {
	    	NamedList<Integer> nl = new NamedList<Integer>();
	    	for (int bucket = 0; bucket <= mMaxBucket; bucket++) {
	    		nl.add( ""+ (1 << bucket), mBuckets[bucket]);
	    	}
	    	return nl;
	    }
	}

	private TermHistogram mHistogram;
  	private int mMinFreq = 0;
  	private int mDistinctTerms = 0;

  	public TopTermQueue(int size) {
  		super(size);
  		mHistogram = new TermHistogram();
  	}

  	public TermHistogram getHistogram() { return mHistogram; }
  	public int getMinFreq() { return mMinFreq; }
  	public int getDistinctTerms() { return mDistinctTerms; }
  	
  	public void setDistinctTerms(int val) { mDistinctTerms = val; }
  	public void setMinFreq(int freq) { mMinFreq = freq; }
  	
  	@Override
  	protected final boolean lessThan(Object a, Object b) {
  		TermInfo termInfoA = (TermInfo)a;
  		TermInfo termInfoB = (TermInfo)b;
  		return termInfoA.mDocFreq < termInfoB.mDocFreq;
  	}

  	/**
  	 * This is a destructive call... the queue is empty at the end
  	 */
  	public NamedList<Integer> toNamedList(IndexSchema schema) 
  			throws ErrorException {
  		// reverse the list..
  		List<TermInfo> aslist = new LinkedList<TermInfo>();
  		while (size() > 0) {
  			aslist.add(0, (TermInfo)pop());
  		}

  		NamedList<Integer> list = new NamedList<Integer>();
  		for (TermInfo ti : aslist) {
  			String txt = ti.mTerm.getText();
  			SchemaField ft = schema.getFieldOrNull(ti.mTerm.getField());
  			if (ft != null) 
  				txt = ft.getType().indexedToReadable(txt);
  			
  			list.add(txt, ti.mDocFreq);
  		}
  		
  		return list;
  	}
  	
  	public TermInfo getTopTermInfo() {
  		return (TermInfo)top();
  	}
  	
}
