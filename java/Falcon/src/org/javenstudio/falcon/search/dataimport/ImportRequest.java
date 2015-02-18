package org.javenstudio.falcon.search.dataimport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContentStream;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.falcon.util.StrHelper;

public class ImportRequest {
	static final Logger LOG = Logger.getLogger(ImportRequest.class);

	//TODO: find a different home for these two...
	private final List<String> mSources;
	private final Params mParams;
	private final ContentStream mContentStream;
	private final ImportDebug mDebugInfo;
	private final String mCommand;
	private final String mEntity;
	
	private final boolean mDebug;
	private final boolean mSyncMode;
	private final boolean mCommit; 
	private final boolean mOptimize;
	private final boolean mClean;
	private final int mStart;
	private final long mRows; 
	
	public ImportRequest(Params params, ContentStream stream) 
			throws ErrorException { 
		mSources = new ArrayList<String>();
		mParams = params;
	    mContentStream = stream;
	    
	    mCommand = (String) params.get("command");
	    mEntity = (String) params.get("entity");
	    
	    String val = (String) params.get("debug");
	    boolean debugMode = (val != null) ? 
	    		StrHelper.parseBool(val, false) : false;
	    
	    if (debugMode) {
	    	mDebug = true;
	    	mDebugInfo = new ImportDebug(params);
	    	
	    } else {
	    	mDebug = false;
	    	mDebugInfo = null;
	    }
	    
	    val = (String) params.get("clean");
	    if (val != null) {
	    	mClean = StrHelper.parseBool(val, true);
	    	
	    } else if (ImportContext.DELTA_IMPORT_CMD.equals(mCommand) || 
	    		ImportContext.IMPORT_CMD.equals(mCommand)) {
	    	mClean = false;
	    	
	    } else  {
	    	mClean = mDebug ? false : true;
	    }
	    
	    mOptimize = StrHelper.parseBool((String) params.get("optimize"), false);
	    if (mOptimize) {
	    	mCommit = true;
	    	
	    } else {
	    	mCommit = StrHelper.parseBool((String) params.get("commit"), 
	    			(mDebug ? false : true));
	    }
	    
	    val = (String) params.get("rows");
	    if (val != null) 
	    	mRows = Integer.parseInt(val);
	    else 
	    	mRows = mDebug ? 10 : Long.MAX_VALUE;
	    
	    val = (String) params.get("start");
	    if (val != null) 
	    	mStart = Integer.parseInt(val);
	    else 
	    	mStart = 0;
	    
	    val = (String) params.get("synchronous");
	    if (val != null)
	    	mSyncMode = StrHelper.parseBool(val, false);
	    else
	    	mSyncMode = false;
	    
	    if (LOG.isDebugEnabled()) {
	    	LOG.debug("command=" + mCommand + " entity=" + mEntity 
	    			+ " debug=" + mDebug + " clean=" + mClean 
	    			+ " commit=" + mCommit + " optimize=" + mOptimize 
	    			+ " rows=" + mRows + " start=" + mStart 
	    			+ " syncMode=" + mSyncMode);
	    }
	}
	
	public String getCommand() { return mCommand; }
	public String getEntityName() { return mEntity; }
	
	public Params getParams() { return mParams; }
	public ContentStream getContentStream() { return mContentStream; }
	public ImportDebug getDebugInfo() { return mDebugInfo; }
	
	public int getStart() { return mStart; }
	public long getRows() { return mRows; }
	
	public boolean isSyncMode() { return mSyncMode; }
	public boolean isDebug() { return mDebug; }
	public boolean isClean() { return mClean; }
	public boolean isCommit() { return mCommit; }
	public boolean isOptimize() { return mOptimize; }
	
	public Collection<String> getSources() { return mSources; }
	public void addSource(String name) { mSources.add(name); }
	
}
