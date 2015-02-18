package org.javenstudio.common.indexdb.index.consumer;

import java.io.IOException;
import java.util.HashMap;

import org.javenstudio.common.indexdb.IField;
import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.index.DocState;
import org.javenstudio.common.indexdb.index.DocumentWriter;
import org.javenstudio.common.indexdb.index.SegmentWriter;

public abstract class DocFieldConsumerPerField {

	public static class List extends HashMap<String, DocFieldConsumerPerField> { 
		private static final long serialVersionUID = 1L;
		public List() { super(); }
	}
	
	private final DocumentWriter mWriter;
	private final IFieldInfo mFieldInfo;
	private final DocBeginConsumerPerField mBeginConsumer;
	private final DocEndConsumerPerField mEndConsumer;
	
	protected DocFieldConsumerPerField(DocFieldConsumer consumer, IFieldInfo fieldInfo) { 
		mWriter = consumer.getDocumentWriter();
		mFieldInfo = fieldInfo;
		mBeginConsumer = consumer.getBeginConsumer().addField(this, fieldInfo);
		mEndConsumer = consumer.getEndConsumer().addField(this, fieldInfo);
	}
	
	public final DocumentWriter getDocumentWriter() { 
		return mWriter;
	}
	
	public final SegmentWriter getSegmentWriter() { 
		return mWriter.getSegmentWriter();
	}
	
	public final IFieldInfo getFieldInfo() { 
		return mFieldInfo;
	}
	
	public final DocBeginConsumerPerField getBeginConsumer() { 
		return mBeginConsumer;
	}
	
	public final DocEndConsumerPerField getEndConsumer() { 
		return mEndConsumer;
	}
	
	public final DocState getDocState() { 
		return mWriter.getDocState();
	}
	
	/** Processes all occurrences of a single field */
	public abstract void processFields(IField[] fields, int count) throws IOException;
	
	public void abort() { 
		try { 
			mBeginConsumer.abort();
		} finally { 
			mEndConsumer.abort();
		}
	}
	
}
