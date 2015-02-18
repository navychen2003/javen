package org.javenstudio.falcon.search.dataimport;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class ImportStatistics {
	
    private AtomicLong mDocCount = new AtomicLong();
    private AtomicLong mDeletedDocCount = new AtomicLong();
    private AtomicLong mFailedDocCount = new AtomicLong();
    private AtomicLong mRowsCount = new AtomicLong();
    private AtomicLong mQueryCount = new AtomicLong();
    private AtomicLong mSkipDocCount = new AtomicLong();

    public ImportStatistics add(ImportStatistics stats) {
    	mDocCount.addAndGet(stats.mDocCount.get());
    	mDeletedDocCount.addAndGet(stats.mDeletedDocCount.get());
    	mRowsCount.addAndGet(stats.mRowsCount.get());
    	mQueryCount.addAndGet(stats.mQueryCount.get());

    	return this;
    }

    public Map<String, Object> getStatsSnapshot() {
    	Map<String, Object> result = new HashMap<String, Object>();
    	result.put("docCount", mDocCount.get());
    	result.put("deletedDocCount", mDeletedDocCount.get());
    	result.put("failedDocCount", mFailedDocCount.get());
    	result.put("rowCount", mRowsCount.get());
    	result.put("queryCount", mRowsCount.get());
    	result.put("skipDocCount", mSkipDocCount.get());
    	return result;
    }
    
    public AtomicLong getDocCountRef() { return mDocCount; }
    public AtomicLong getDeletedDocCountRef() { return mDeletedDocCount; }
    public AtomicLong getFailedDocCountRef() { return mFailedDocCount; }
    public AtomicLong getRowsCountRef() { return mRowsCount; }
    public AtomicLong getQueryCountRef() { return mQueryCount; }
    public AtomicLong getSkipDocCountRef() { return mSkipDocCount; }
    
    public long getDocCount() { return mDocCount.get(); }
    public long getDeletedDocCount() { return mDeletedDocCount.get(); }
    public long getFailedDocCount() { return mFailedDocCount.get(); }
    public long getRowsCount() { return mRowsCount.get(); }
    public long getQueryCount() { return mQueryCount.get(); }
    public long getSkipDocCount() { return mSkipDocCount.get(); }
    
    public long increaseDocCount() { return mDocCount.incrementAndGet(); }
    public long increaseDeletedDocCount() { return mDeletedDocCount.incrementAndGet(); }
    public long increaseFailedDocCount() { return mFailedDocCount.incrementAndGet(); }
    public long increaseRowsCount() { return mRowsCount.incrementAndGet(); }
    public long increaseQueryCount() { return mQueryCount.incrementAndGet(); }
    public long increaseSkipDocCount() { return mSkipDocCount.incrementAndGet(); }
    
}
