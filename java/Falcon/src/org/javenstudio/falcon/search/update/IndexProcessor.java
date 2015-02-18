package org.javenstudio.falcon.search.update;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.ISearchCore;
import org.javenstudio.falcon.search.ISearchRequest;

public class IndexProcessor extends UpdateProcessor {
	static final Logger LOG = Logger.getLogger(IndexProcessor.class);

	@SuppressWarnings("unused")
	private final ISearchRequest mRequest;
	private final UpdateIndexer mIndexer;

	private boolean mChangesSinceCommit = false;

	public IndexProcessor(ISearchCore core, 
			ISearchRequest req, UpdateProcessor next) {
		super(next);
		mRequest = req;
		mIndexer = core.getUpdateIndexer();
	}

	@Override
	public void processAdd(AddCommand cmd) throws ErrorException {
		//if (LOG.isDebugEnabled())
		//	LOG.debug("processAdd: " + cmd);
		
		mIndexer.addDoc(cmd);
		super.processAdd(cmd);
		mChangesSinceCommit = true;
	}

	@Override
	public void processDelete(DeleteCommand cmd) throws ErrorException {
		if (LOG.isDebugEnabled())
			LOG.debug("processDelete: " + cmd);
		
		if (cmd.isDeleteById()) 
			mIndexer.delete(cmd);
		else 
			mIndexer.deleteByQuery(cmd);
		
		super.processDelete(cmd);
		mChangesSinceCommit = true;
	}

	@Override
	public void processMergeIndexes(MergeCommand cmd) throws ErrorException {
		if (LOG.isDebugEnabled())
			LOG.debug("processMergeIndexes: " + cmd);
		
		mIndexer.mergeIndexes(cmd);
		super.processMergeIndexes(cmd);
	}

	@Override
	public void processCommit(CommitCommand cmd) throws ErrorException {
		if (LOG.isDebugEnabled())
			LOG.debug("processCommit: " + cmd);
		
		mIndexer.commit(cmd);
		super.processCommit(cmd);
		mChangesSinceCommit = false;
	}

	@Override
	public void processRollback(RollbackCommand cmd) throws ErrorException {
		if (LOG.isDebugEnabled())
			LOG.debug("processRollback: " + cmd);
		
		mIndexer.rollback(cmd);
		super.processRollback(cmd);
		mChangesSinceCommit = false;
	}

	@Override
	public void finish() throws ErrorException {
		if (LOG.isDebugEnabled())
			LOG.debug("finish");
		
		mIndexer.finish(mChangesSinceCommit);
		super.finish();
	}
	
}
