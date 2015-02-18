package org.javenstudio.falcon.search.update;

import java.util.List;
import java.util.Locale;

import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.PluginInfoInitialized;

public abstract class UpdateLog implements PluginInfoInitialized {
	static final Logger LOG = Logger.getLogger(UpdateLog.class);

	public static final int ADD = 0x01;
	public static final int DELETE = 0x02;
	public static final int DELETE_BY_QUERY = 0x03;
	public static final int COMMIT = 0x04;
	// Flag indicating that this is a buffered operation, and that a gap exists before buffering started.
	// for example, if full index replication starts and we are buffering updates, then this flag should
	// be set to indicate that replaying the log would not bring us into sync (i.e. peersync should
	// fail if this flag is set on the last update in the tlog).
	public static final int FLAG_GAP = 0x10;
	// mask off flags to get the operation
	public static final int OPERATION_MASK = 0x0f; 
	
	public enum SyncLevel { 
		NONE, FLUSH, FSYNC;
		
		public static SyncLevel getSyncLevel(String level){
			if (level == null) 
				return SyncLevel.FLUSH;
			
			try{
				return SyncLevel.valueOf(level.toUpperCase(Locale.ROOT));
				
			} catch (Exception ex){
				if (LOG.isWarnEnabled()) {
					LOG.warn("There was an error reading the SyncLevel" 
							+ " - default to " + SyncLevel.FLUSH, ex);
				}
				return SyncLevel.FLUSH;
			}
		}
	}
	
	public static class DBQ {
		private String mQueryString;	// the query string
		private long mVersion; 			// positive version of the DBQ

		public DBQ(String q, long version) { 
			mQueryString = q; 
			mVersion = version;
		}
		
		public final String getQueryString() { return mQueryString; }
		public final long getVersion() { return mVersion; }
		
		@Override
		public String toString() {
			return "DBQ{version=" + mVersion + ", q=" + mQueryString + "}";
		}
	}
	
	public static enum State { 
		REPLAYING, BUFFERING, APPLYING_BUFFERED, ACTIVE 
	}
  
	public abstract void init(UpdateIndexer handler) throws ErrorException;
	
	public abstract UpdateIndexer getUpdateIndexer();
	public abstract VersionInfo getVersionInfo();
	public abstract State getState();
	public abstract boolean hasUncommittedChanges();
	
	public abstract List<DBQ> getDBQNewer(long version) throws ErrorException;
	public abstract Long lookupVersion(BytesRef indexedId) throws ErrorException;
	
	public void add(AddCommand cmd) throws ErrorException { 
		add(cmd, false);
	}
	
	public abstract void add(AddCommand cmd, boolean clearCaches) throws ErrorException;
	
	public abstract void delete(DeleteCommand cmd) throws ErrorException;
	public abstract void deleteAll() throws ErrorException;
	public abstract void deleteByQuery(DeleteCommand cmd) throws ErrorException;
	
	public abstract void preCommit(CommitCommand cmd) throws ErrorException;
	public abstract void postCommit(CommitCommand cmd) throws ErrorException;
	public abstract void preSoftCommit(CommitCommand cmd) throws ErrorException;
	public abstract void postSoftCommit(CommitCommand cmd) throws ErrorException;
	
	public abstract void finish(SyncLevel level) throws ErrorException;
	public abstract void close(boolean committed) throws ErrorException;
	
}
