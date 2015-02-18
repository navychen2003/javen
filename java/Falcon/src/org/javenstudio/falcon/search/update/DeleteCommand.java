package org.javenstudio.falcon.search.update;

import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.CharsRef;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.schema.IndexSchema;
import org.javenstudio.falcon.search.schema.SchemaField;

/**
 *
 */
public class DeleteCommand extends UpdateCommand {
	
	private final IndexSchema mSchema;
	
	private String mId;    // external (printable) id, for delete-by-id
	private String mQuery; // query string for delete-by-query
	private BytesRef mIndexedId;
	private int mCommitWithin = -1;

	public DeleteCommand(ISearchRequest req, IndexSchema schema) {
		super(req);
		mSchema = schema;
	}

	@Override
	public String getName() {
		return "delete";
	}

	public int getCommitWithin() { 
		return mCommitWithin; 
	}
	
	public void setCommitWithin(int val) { 
		mCommitWithin = val;
	}
	
	public boolean isDeleteById() {
		return mQuery == null;
	}

	public void clear() {
		mId = null;
		mQuery = null;
		mIndexedId = null;
		mVersion = 0;
	}

	/** 
	 * Returns the indexed ID for this delete. 
	 * The returned BytesRef is retained across multiple calls, 
	 * and should not be modified. 
	 */
	public BytesRef getIndexedId() throws ErrorException {
		if (mIndexedId == null) {
			IndexSchema schema = mSchema;
			SchemaField sf = schema.getUniqueKeyField();
			if (sf != null && mId != null) {
				mIndexedId = new BytesRef();
				sf.getType().readableToIndexed(mId, mIndexedId);
			}
		}
		return mIndexedId;
	}

	public String getId() throws ErrorException {
		if (mId == null && mIndexedId != null) {
			IndexSchema schema = mSchema;
			SchemaField sf = schema.getUniqueKeyField();
			if (sf != null) {
				CharsRef ref = new CharsRef();
				sf.getType().indexedToReadable(mIndexedId, ref);
				mId = ref.toString();
			}
		}
		return mId;
	}

	public String getQueryString() {
		return mQuery;
	}

	public void setQueryString(String query) {
		mQuery = query;
	}

	public void setIndexedId(BytesRef indexedId) {
		mIndexedId = indexedId;
		mId = null;
	}

	public void setId(String id) {
		mId = id;
		mIndexedId = null;
	}

	@Override
	protected void toString(StringBuilder sb) {
		if (mId != null) 
			sb.append(",id=").append(mId);
		if (mIndexedId != null) 
			sb.append(",indexedId=").append(mIndexedId);
		if (mQuery != null) 
			sb.append(",query=`").append(mQuery).append('`');
		sb.append(",commitWithin=").append(mCommitWithin);
	}

}
