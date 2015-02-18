package org.javenstudio.falcon.search.update;

public class UpdateIndexerConfig {

    private final String mClassName;
    
    private final int mAutoCommitMaxDocs; 
    private final int mAutoCommitMaxTime; 
    private final int mCommitIntervalLowerBound; 
    private final int mAutoSoftCommitMaxDocs; 
    private final int mAutoSoftCommitMaxTime;
    
    // is opening a new searcher part of hard autocommit?
    private final boolean mOpenSearcher; 

    /**
     * @param autoCommitMaxDocs set -1 as default
     * @param autoCommitMaxTime set -1 as default
     * @param commitIntervalLowerBound set -1 as default
     */
    public UpdateIndexerConfig(String className, 
    		int autoCommitMaxDocs, int autoCommitMaxTime, 
    		int autoSoftCommitMaxDocs, int autoSoftCommitMaxTime, 
    		int commitIntervalLowerBound, boolean openSearcher) {
    	mClassName = className;
    	mAutoCommitMaxDocs = autoCommitMaxDocs;
    	mAutoCommitMaxTime = autoCommitMaxTime;
      	mAutoSoftCommitMaxDocs = autoSoftCommitMaxDocs;
      	mAutoSoftCommitMaxTime = autoSoftCommitMaxTime;
      	mCommitIntervalLowerBound = commitIntervalLowerBound;
      	mOpenSearcher = openSearcher;
    }
	
    public final String getClassName() { return mClassName; }
    public final int getAutoCommitMaxDocs() { return mAutoCommitMaxDocs; }
    public final int getAutoCommitMaxTime() { return mAutoCommitMaxTime; }
    public final int getCommitIntervalLowerBound() { return mCommitIntervalLowerBound; }
    public final int getAutoSoftCommitMaxDocs() { return mAutoSoftCommitMaxDocs; }
    public final int getAutoSoftCommitMaxTime() { return mAutoSoftCommitMaxTime; }
    public final boolean isOpenSearcher() { return mOpenSearcher; }
    
}
