package org.javenstudio.common.indexdb.index.consumer;

import java.io.IOException;

import org.javenstudio.common.indexdb.codec.IFieldsFormat;
import org.javenstudio.common.indexdb.index.DocumentWriter;

public class StoredFieldsBuilder extends FieldsBuilder {

	private final IFieldsFormat mFieldsFormat;
	private IFieldsFormat.Writer mFieldsWriter = null;
	
	public StoredFieldsBuilder(DocumentWriter writer) { 
		super(writer);
		
		mFieldsFormat = writer.getSegmentWriter().getIndexWriter()
				.getIndexFormat().getFieldsFormat();
	}
	
	private void initFieldsWriter() throws IOException { 
		if (mFieldsWriter == null) { 
			mFieldsWriter = mFieldsFormat.createWriter(
					getDocumentWriter().getDirectory(), 
					getDocumentWriter().getSegmentInfo().getName());
			mLastDocID = 0;
		}
	}
	
	@Override
	public void flush() throws IOException { 
		if (mFieldsWriter != null) 
			mFieldsWriter.close();
	}
	
	@Override
	public void finishDocument() throws IOException { 
		initFieldsWriter();
		fill(getDocumentWriter().getDocState().getDocID());
		
		if (mFieldsWriter != null && mNumStoredFields > 0) {
			mFieldsWriter.startDocument(mNumStoredFields);
			for (int i = 0; i < mNumStoredFields; i++) {
				mFieldsWriter.writeField(mFieldInfos[i], mStoredFields[i]);
			}
			mLastDocID ++;
		}

		reset();
	}
	
	/** Fills in any hole in the docIDs */
	private void fill(int docID) throws IOException {
	    // We must "catch up" for all docs before us
	    // that had no stored fields:
	    while (mLastDocID < docID) {
	    	mFieldsWriter.startDocument(0);
	    	mLastDocID ++;
	    }
	}
	
}
