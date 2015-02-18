package org.javenstudio.falcon.search.update;

import org.javenstudio.falcon.search.ISearchRequest;

/** 
 * An index update command encapsulated in an object (Command pattern)
 *
 */
public abstract class UpdateCommand implements Cloneable {
	
	// XML Constants
	public static final String ADD = "add";
	public static final String DELETE = "delete";
	public static final String OPTIMIZE = "optimize";
	public static final String COMMIT = "commit";
	public static final String ROLLBACK = "rollback";
	public static final String WAIT_SEARCHER = "waitSearcher";
	public static final String SOFT_COMMIT = "softCommit";

	public static final String OVERWRITE = "overwrite";
	public static final String VERSION = "version";
  
	// NOTE: This constant is for use with the <add> XML tag, not the HTTP param with same name
	public static final String COMMIT_WITHIN = "commitWithin";
	
	// update command is being buffered.
	public static int BUFFERING = 0x00000001; 
	
	// update command is from replaying a log.
	public static int REPLAY    = 0x00000002; 
	
	// update command is a missing update being provided by a peer.
	public static int PEER_SYNC = 0x00000004; 
	
	// this update should not count toward triggering of autocommits.
	public static int IGNORE_AUTOCOMMIT = 0x00000008; 
	
	// clear caches associated with the update log. 
	// used when applying reordered DBQ updates when doing an add.
	public static int CLEAR_CACHES = 0x00000010; 

	
	protected ISearchRequest mRequest;
	protected long mVersion;
	protected int mFlags;

	public UpdateCommand(ISearchRequest req) {
		mRequest = req;
	}

	public abstract String getName();

	public long getVersion() {
		return mVersion;
	}
	
	public void setVersion(long version) {
		mVersion = version;
	}

	public void setFlags(int flags) {
		mFlags = flags;
	}

	public int getFlags() {
		return mFlags;
	}

	public ISearchRequest getRequest() {
		return mRequest;
	}

	public void setRequest(ISearchRequest req) {
		mRequest = req;
	}

	@Override
	public UpdateCommand clone() {
		try {
			return (UpdateCommand) super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(getName()).append("{");
		sb.append("flags=").append(mFlags);
		sb.append(",version=").append(mVersion);
		
		toString(sb);
		
		sb.append("}");
		return sb.toString();
	}
	
	protected void toString(StringBuilder sb) { 
		// do nothing
	}
	
}
