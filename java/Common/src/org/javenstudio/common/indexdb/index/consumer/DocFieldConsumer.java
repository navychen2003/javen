package org.javenstudio.common.indexdb.index.consumer;

import java.io.IOException;

import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.codec.ISegmentWriteState;
import org.javenstudio.common.indexdb.index.DocumentWriter;

public abstract class DocFieldConsumer {

	private final DocumentWriter mWriter;
	private final DocBeginConsumer mBeginConsumer;
	private final DocEndConsumer mEndConsumer;
	
	public DocFieldConsumer(DocumentWriter writer, 
			DocBeginConsumer beginConsumer, DocEndConsumer endConsumer) { 
		mWriter = writer;
		mBeginConsumer = beginConsumer;
		mEndConsumer = endConsumer;
	}
	
	public abstract DocFieldConsumerPerField addField(IFieldInfo fieldInfo);
	
	public final DocumentWriter getDocumentWriter() { 
		return mWriter;
	}
	
	public final DocBeginConsumer getBeginConsumer() { 
		return mBeginConsumer;
	}
	
	public final DocEndConsumer getEndConsumer() { 
		return mEndConsumer;
	}
	
	public void startDocument() throws IOException { 
		mBeginConsumer.startDocument();
		mEndConsumer.startDocument();
	}
	
	public void finishDocument() throws IOException { 
	    // TODO: allow endConsumer.finishDocument to also return
	    // a DocWriter
		mEndConsumer.finishDocument();
		mBeginConsumer.finishDocument();
	}
	
	public void flush(DocFieldConsumerPerField.List fields, ISegmentWriteState state) 
			throws IOException { 
		DocBeginConsumerPerField.List beginFields = new DocBeginConsumerPerField.List();
		DocEndConsumerPerField.List endFields = new DocEndConsumerPerField.List();
		
		for (DocFieldConsumerPerField field : fields.values()) { 
			beginFields.put(field.getFieldInfo().getName(), field.getBeginConsumer());
			endFields.put(field.getFieldInfo().getName(), field.getEndConsumer());
		}
		
		mBeginConsumer.flush(beginFields, state);
		mEndConsumer.flush(endFields, state);
	}
	
	public void abort() { 
		try { 
			mBeginConsumer.abort();
		} finally { 
			mEndConsumer.abort();
		}
	}
	
	public boolean freeRAM() {
		return mBeginConsumer.freeRAM();
	}
	
}
