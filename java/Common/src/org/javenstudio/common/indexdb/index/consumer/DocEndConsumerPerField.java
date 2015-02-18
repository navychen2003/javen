package org.javenstudio.common.indexdb.index.consumer;

import java.io.IOException;
import java.util.HashMap;

import org.javenstudio.common.indexdb.IFieldInfo;

public abstract class DocEndConsumerPerField {

	public static class List extends HashMap<String, DocEndConsumerPerField> { 
		private static final long serialVersionUID = 1L;
		public List() { super(); }
	}
	
	private final DocFieldConsumerPerField mFieldConsumer;
	private final DocEndConsumer mEndConsumer;
	private final IFieldInfo mFieldInfo;
	
	protected DocEndConsumerPerField(
			DocFieldConsumerPerField fieldConsumer, DocEndConsumer endConsumer, 
			IFieldInfo fieldInfo) { 
		mFieldConsumer = fieldConsumer;
		mEndConsumer = endConsumer;
		mFieldInfo = fieldInfo;
	}
	
	public final DocFieldConsumerPerField getFieldConsumer() { return mFieldConsumer; }
	public final DocEndConsumer getEndConsumer() { return mEndConsumer; }
	public final IFieldInfo getFieldInfo() { return mFieldInfo; }
	
	public void finish() throws IOException {}
	public void abort() {}

}
