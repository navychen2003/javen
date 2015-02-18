package org.javenstudio.falcon.search.dataimport;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.params.UpdateParams;
import org.javenstudio.falcon.search.schema.IndexSchema;
import org.javenstudio.falcon.search.update.AddCommand;
import org.javenstudio.falcon.search.update.CommitCommand;
import org.javenstudio.falcon.search.update.DeleteCommand;
import org.javenstudio.falcon.search.update.InputDocument;
import org.javenstudio.falcon.search.update.RollbackCommand;
import org.javenstudio.falcon.search.update.UpdateProcessor;

/**
 * <p> Writes documents to system. </p>
 * <p/>
 * <b>This API is experimental and may change in the future.</b>
 *
 * @since 1.3
 */
public class ImportWriterImpl extends ImportWriterBase {

	private final IndexSchema mSchema;
	private final UpdateProcessor mProcessor;
	private final ISearchRequest mRequest;
	private final int mCommitWithin;
  
	public ImportWriterImpl(UpdateProcessor processor, ISearchRequest req) 
			throws ErrorException {
		mSchema = req.getSearchCore().getSchema();
		mProcessor = processor;
		mRequest = req;
		mCommitWithin = (req != null) ? 
				req.getParams().getInt(UpdateParams.COMMIT_WITHIN, -1) : -1;
	}
  
	@Override
	public void init(ImportContext context) throws ErrorException {
		// do nothing
	}
  
	@Override
	public void close() throws ErrorException {
		mProcessor.finish();
	}
  
	@Override
	public boolean isRollbackSupported() { 
		return mRequest.getSearchCore().getUpdateIndexer().isRollbackSupported();
	}
	
	@Override
	public void upload(InputDocument d) throws ErrorException {
		AddCommand command = new AddCommand(mRequest, mSchema);
		command.setInputDocument(d);
		command.setCommitWithin(mCommitWithin);
		mProcessor.processAdd(command);
	}
  
	@Override
	public void deleteDoc(Object id) throws ErrorException {
		DeleteCommand delCmd = new DeleteCommand(mRequest, mSchema);
		delCmd.setId(id.toString());
		mProcessor.processDelete(delCmd);
	}

	@Override
	public void deleteByQuery(String query) throws ErrorException {
		DeleteCommand delCmd = new DeleteCommand(mRequest, mSchema);
		delCmd.setQueryString(query);
		mProcessor.processDelete(delCmd);
	}

	@Override
	public void commit(boolean optimize) throws ErrorException {
		CommitCommand commit = new CommitCommand(mRequest, optimize);
		mProcessor.processCommit(commit);
	}

	@Override
	public void rollback() throws ErrorException {
		RollbackCommand rollback = new RollbackCommand(mRequest);
		mProcessor.processRollback(rollback);
	}

	@Override
	public void deleteAll() throws ErrorException {
		DeleteCommand deleteCommand = new DeleteCommand(mRequest, mSchema);
		deleteCommand.setQueryString("*:*");
		mProcessor.processDelete(deleteCommand);
	}
	
}
