package org.javenstudio.common.indexdb.index.consumer;

import java.io.IOException;
import java.util.HashMap;

import org.javenstudio.common.indexdb.IField;
import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IToken;

public abstract class DocBeginConsumerPerField {

	public static class List extends HashMap<String, DocBeginConsumerPerField> { 
		private static final long serialVersionUID = 1L;
		public List() { super(); }
	}
	
	private final DocFieldConsumerPerField mFieldConsumer;
	private final DocBeginConsumer mBeginConsumer;
	private final IFieldInfo mFieldInfo;
	
	protected DocBeginConsumerPerField(
			DocFieldConsumerPerField fieldConsumer, DocBeginConsumer beginConsumer, 
			IFieldInfo fieldInfo) { 
		mFieldConsumer = fieldConsumer;
		mBeginConsumer = beginConsumer;
		mFieldInfo = fieldInfo;
	}
	
	public final DocFieldConsumerPerField getFieldConsumer() { return mFieldConsumer; }
	public final DocBeginConsumer getBeginConsumer() { return mBeginConsumer; }
	public final IFieldInfo getFieldInfo() { return mFieldInfo; }
	
	// Called once per field, and is given all IndexableField
	// occurrences for this field in the document.  Return
	// true if you wish to see inverted tokens for these
	// fields:
	public boolean start(IField[] fields, int count) throws IOException { 
		return false;
	}

	// Called before a field instance is being processed
	public void start(IField field) { 
	}
  
	// Called once per inverted token
	public abstract void add(IToken token) throws IOException;

	// Called once per field per document, after all IndexableFields
	// are inverted
	public void finish() throws IOException { 
	}

	// Called on hitting an aborting exception
	public void abort() { 
	}
  
}
