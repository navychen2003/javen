package org.javenstudio.falcon.search.update;

import org.javenstudio.common.indexdb.document.Document;
import org.javenstudio.common.indexdb.index.term.Term;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.schema.IndexSchema;
import org.javenstudio.falcon.search.schema.SchemaField;

/**
 *
 */
public class AddCommand extends UpdateCommand {
	
	private final IndexSchema mSchema;
	
	// optional id in "internal" indexed form... if it is needed and not supplied,
	// it will be obtained from the doc.
	private BytesRef mIndexedId;

	// Higher level InputDocument, normally used to construct the Document
	// to index.
	private InputDocument mInputDoc;

	private Term mUpdateTerm;

	private int mCommitWithin = -1;
	private boolean mOverwrite = true;
   
	public AddCommand(ISearchRequest req, IndexSchema schema) {
		super(req);
		mSchema = schema;
	}

	@Override
	public String getName() {
		return "add";
	}

	/** 
	 * Reset state to reuse this object with a different document 
	 * in the same request 
	 */
	public void clear() {
		mInputDoc = null;
		mIndexedId = null;
		mUpdateTerm = null;
		mVersion = 0;
	}

	public boolean isOverwrite() { 
		return mOverwrite; 
	}
	
	public void setOverwrite(boolean overwrite) { 
		mOverwrite = overwrite; 
	}
	
	public int getCommitWithin() { 
		return mCommitWithin; 
	}
	
	public void setCommitWithin(int val) { 
		mCommitWithin = val;
	}
	
	public InputDocument getInputDocument() {
		return mInputDoc;
	}

	public void setInputDocument(InputDocument doc) { 
		mInputDoc = doc;
	}
	
	public Term getUpdateTerm() { 
		return mUpdateTerm;
	}
	
	/** 
	 * Creates and returns a Document to index. 
	 * Any changes made to the returned Document
	 * will not be reflected in the InputDocument, 
	 * or future calls to this method.
	 */
	public Document getIndexDocument() throws ErrorException {
		return DocumentBuilder.toDocument(getInputDocument(), mSchema);
	}

	/** 
	 * Returns the indexed ID for this document. 
	 * The returned BytesRef is retained across multiple calls, 
	 * and should not be modified. 
	 */
	public BytesRef getIndexedId() throws ErrorException {
		if (mIndexedId == null) {
			IndexSchema schema = mSchema;
			SchemaField sf = schema.getUniqueKeyField();
			
			if (sf != null && mInputDoc != null) {
				InputField field = mInputDoc.getField(sf.getName());

				int count = (field == null) ? 0 : field.getValueCount();
				if (count == 0) {
					if (mOverwrite) {
						throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
								"Document is missing mandatory uniqueKey field: " + sf.getName());
					}
					
				} else if (count  > 1) {
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"Document contains multiple values for uniqueKey field: " + field);
					
				} else {
					mIndexedId = new BytesRef();
					sf.getType().readableToIndexed(
							field.getFirstValue().toString(), mIndexedId);
				}
			}
		}
		
		return mIndexedId;
	}

	public void setIndexedId(BytesRef indexedId) {
		mIndexedId = indexedId;
	}

	public String getPrintableId() {
		IndexSchema schema = mSchema;
		SchemaField sf = schema.getUniqueKeyField();
		
		if (mInputDoc != null && sf != null) {
			InputField field = mInputDoc.getField(sf.getName());
			if (field != null) 
				return field.getFirstValue().toString();
		}
		
		return "(null)";
	}

	/**
	 * @return String id to hash
	 */
	public String getHashableId() throws ErrorException {
		IndexSchema schema = mSchema;
		SchemaField sf = schema.getUniqueKeyField();
		
		String id = null;
		if (sf != null) {
			if (mInputDoc != null) {
				InputField field = mInputDoc.getField(sf.getName());
        
				int count = (field == null) ? 0 : field.getValueCount();
				if (count == 0) {
					if (mOverwrite) {
						throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
								"Document is missing mandatory uniqueKey field: " + sf.getName());
					}
					
				} else if (count > 1) {
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
							"Document contains multiple values for uniqueKey field: " + field);
					
				} else {
					return field.getFirstValue().toString();
				}
			}
		}
		
		return id;
	}
  
	@Override
	protected void toString(StringBuilder sb) {
		sb.append(",id=").append(getPrintableId());
		if (!mOverwrite) 
			sb.append(",overwrite=").append(mOverwrite);
		if (mCommitWithin != -1) 
			sb.append(",commitWithin=").append(mCommitWithin);
	}
	
 }
