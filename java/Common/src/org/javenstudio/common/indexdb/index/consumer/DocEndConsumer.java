package org.javenstudio.common.indexdb.index.consumer;

import java.io.IOException;

import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.codec.ISegmentWriteState;
import org.javenstudio.common.indexdb.index.DocumentWriter;

public abstract class DocEndConsumer {

	private final DocumentWriter mWriter;
	
	protected DocEndConsumer(DocumentWriter writer) { 
		mWriter = writer;
	}
	
	public final DocumentWriter getDocumentWriter() { 
		return mWriter;
	}
	
	public abstract DocEndConsumerPerField addField(
			DocFieldConsumerPerField consumer, IFieldInfo fieldInfo);
	
	public abstract void startDocument() throws IOException;
	public abstract void finishDocument() throws IOException;
	public abstract void flush(DocEndConsumerPerField.List fields, 
			ISegmentWriteState state) throws IOException;
	public abstract void abort();
	
}
