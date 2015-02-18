package org.javenstudio.falcon.search.update;

import org.javenstudio.falcon.ErrorException;

/**
 * This is a good place for subclassed update handlers to process the document before it is 
 * indexed.  You may wish to add/remove fields or check if the requested user is allowed to 
 * update the given document...
 * 
 * Perhaps you continue adding an error message (without indexing the document)...
 * perhaps you throw an error and halt indexing (remove anything already indexed??)
 * 
 * By default, this just passes the request to the next processor in the chain.
 * 
 * @since 1.3
 */
public abstract class UpdateProcessor {
	
	protected final UpdateProcessor mNext;

	public UpdateProcessor(UpdateProcessor next) {
		mNext = next;
	}

	public void processAdd(AddCommand cmd) throws ErrorException {
		if (mNext != null) 
			mNext.processAdd(cmd);
	}

	public void processDelete(DeleteCommand cmd) throws ErrorException {
		if (mNext != null) 
			mNext.processDelete(cmd);
	}

	public void processMergeIndexes(MergeCommand cmd) throws ErrorException {
		if (mNext != null) 
			mNext.processMergeIndexes(cmd);
	}

	public void processCommit(CommitCommand cmd) throws ErrorException {
		if (mNext != null) 
			mNext.processCommit(cmd);
	}

	public void processRollback(RollbackCommand cmd) throws ErrorException {
		if (mNext != null) 
			mNext.processRollback(cmd);
	}

	public void finish() throws ErrorException {
		if (mNext != null) 
			mNext.finish();    
	}
	
}
