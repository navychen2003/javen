package org.javenstudio.common.indexdb.index.consumer;

import java.io.IOException;

import org.javenstudio.common.indexdb.IField;
import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IFieldsBuilder;
import org.javenstudio.common.indexdb.codec.IFieldInfosFormat;
import org.javenstudio.common.indexdb.codec.ISegmentWriteState;
import org.javenstudio.common.indexdb.index.DocumentWriter;

/**
 * This is a DocConsumer that gathers all fields under the
 * same name, and calls per-field consumers to process field
 * by field.  This class doesn't doesn't do any "real" work
 * of its own: it just forwards the fields to a
 * DocFieldConsumer.
 */
public class DocumentProcessor extends DocConsumer {

	public DocumentProcessor(DocumentWriter writer, DocFieldConsumer consumer) { 
		super(writer, consumer);
	}
	
	@Override
	protected IFieldsBuilder createFieldsBuilder() { 
		return new StoredFieldsBuilder(getDocumentWriter());
	}
	
	@Override
	protected void storeField(IFieldInfo fieldInfo, IField field) throws IOException { 
		getFieldsBuilder().addField(fieldInfo, field);
	}
	
	@Override
	protected void startDocument() throws IOException { 
		super.startDocument();
		getFieldsBuilder().startDocument();
	}
	
	@Override
	public void finishDocument() throws IOException { 
	    try {
	    	getFieldsBuilder().finishDocument();
	    } finally {
	    	super.finishDocument();
	    }
	}
	
	@Override
	public void flush(ISegmentWriteState state) throws IOException { 
		getFieldsBuilder().flush();
		super.flush(state);
		
	    // Important to save after asking consumer to flush so
	    // consumer can alter the FieldInfo* if necessary.  EG,
	    // FreqProxTermsWriter does this with FieldInfo.storePayload.
		IFieldInfosFormat.Writer infosWriter = getFieldInfosFormat().createWriter(
				getDocumentWriter().getDirectory(), 
				getDocumentWriter().getSegmentInfo().getName());
	    infosWriter.writeFieldInfos(state.getFieldInfos());
	}
	
	@Override
	public void abort() {
		Throwable th = null;
		
	    try {
	    	getFieldsBuilder().abort();
	    } catch (Throwable t) {
	    	if (th == null) 
	    		th = t;
	    }
		
	    // If any errors occured, throw it.
	    if (th != null) {
	    	if (th instanceof RuntimeException) throw (RuntimeException) th;
	    	if (th instanceof Error) throw (Error) th;
	    	// defensive code - we should not hit unchecked exceptions
	    	throw new RuntimeException(th);
	    }
	    
	    super.abort();
	}
	
}
