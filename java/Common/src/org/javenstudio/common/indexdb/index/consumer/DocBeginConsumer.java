package org.javenstudio.common.indexdb.index.consumer;

import java.io.IOException;

import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.codec.ISegmentWriteState;
import org.javenstudio.common.indexdb.index.DocumentWriter;

public abstract class DocBeginConsumer {

	private final DocumentWriter mWriter;
	
	protected DocBeginConsumer(DocumentWriter writer) { 
		mWriter = writer;
	}
	
	public final DocumentWriter getDocumentWriter() { 
		return mWriter;
	}
	
	public abstract DocBeginConsumerPerField addField(
			DocFieldConsumerPerField consumer, IFieldInfo fieldInfo);
	
	public abstract void startDocument() throws IOException;
	public abstract void finishDocument() throws IOException;
	public abstract void flush(DocBeginConsumerPerField.List fields, 
			ISegmentWriteState state) throws IOException;
	public abstract void abort();
	
	/** Attempt to free RAM, returning true if any RAM was freed */
	public abstract boolean freeRAM();
	
}
