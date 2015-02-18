package org.javenstudio.common.indexdb.index.consumer;

import java.io.IOException;
import java.util.Comparator;

import org.javenstudio.common.indexdb.IDocument;
import org.javenstudio.common.indexdb.IField;
import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IFieldInfos;
import org.javenstudio.common.indexdb.IFieldsBuilder;
import org.javenstudio.common.indexdb.codec.IFieldInfosFormat;
import org.javenstudio.common.indexdb.codec.ISegmentWriteState;
import org.javenstudio.common.indexdb.index.DocumentWriter;
import org.javenstudio.common.indexdb.util.ArrayUtil;

public abstract class DocConsumer {

	private final DocumentWriter mWriter;
	private final DocFieldConsumer mConsumer;
	private final IFieldInfosFormat mFieldInfosFormat;
	
	private IFieldsBuilder mFieldsBuilder = null;
	
	// Holds all fields seen in current doc
	private DocConsumerPerField[] mDocFields = new DocConsumerPerField[1];
	private int mDocFieldCount = 0;
	private int mDocFieldGen = 0;
	
	// Hash table for all fields ever seen
	private DocConsumerPerField[] mTotalFieldHash = new DocConsumerPerField[2];
	private int mTotalFieldHashMask = 1;
	private int mTotalFieldCount = 0;
	
	protected DocConsumer(DocumentWriter writer, DocFieldConsumer consumer) { 
		mWriter = writer;
		mConsumer = consumer;
		mFieldInfosFormat = writer.getSegmentWriter().getIndexWriter()
				.getIndexFormat().getFieldInfosFormat();
	}
	
	public final DocumentWriter getDocumentWriter() { 
		return mWriter;
	}
	
	public final DocFieldConsumer getConsumer() { 
		return mConsumer; 
	}
	
	protected abstract IFieldsBuilder createFieldsBuilder();
	
	protected synchronized final IFieldsBuilder getFieldsBuilder() { 
		if (mFieldsBuilder == null)
			mFieldsBuilder = createFieldsBuilder();
		return mFieldsBuilder;
	}
	
	protected final IFieldInfosFormat getFieldInfosFormat() { 
		return mFieldInfosFormat;
	}
	
	public final void processDocument(IFieldInfos.Builder fieldInfos) throws IOException { 
		IDocument doc = mWriter.getDocState().getDocument();
		if (doc == null) 
			return;
		
		startDocument();
		buildFieldHash(fieldInfos, doc);
		
	    for (int i=0; i < mDocFieldCount; i++) {
	        final DocConsumerPerField perField = mDocFields[i];
	        perField.getConsumer().processFields(
	        		perField.getFields(), perField.getFieldCount());
	    }
	}
	
	protected abstract void storeField(IFieldInfo fieldInfo, IField field) 
			throws IOException;
	
	protected void startDocument() throws IOException { 
		mConsumer.startDocument();
	}
	
	public void finishDocument() throws IOException { 
		mConsumer.finishDocument();
	}
	
	public void flush(ISegmentWriteState flushState) throws IOException { 
		DocConsumerPerField.List allFields = getFields();
		
		DocFieldConsumerPerField.List fields = new DocFieldConsumerPerField.List();
		for (DocConsumerPerField field : allFields.values()) { 
			fields.put(field.getFieldInfo().getName(), field.getConsumer());
		}
		
		mConsumer.flush(fields, flushState);
	}
	
	/** 
	 * In flush we reset the fieldHash to not maintain per-field state
	 *  across segments 
	 */
	public void doAfterFlush() {
		mTotalFieldHash = new DocConsumerPerField[2];
		mTotalFieldHashMask = 1;
		mTotalFieldCount = 0;
	}
	
	public void abort() {
	    Throwable th = null;
	    
	    for (DocConsumerPerField field : mTotalFieldHash) {
	    	while (field != null) {
	    		final DocConsumerPerField next = field.mNext;
	    		try {
	    			field.abort();
	    		} catch (Throwable t) {
	    			if (th == null) 
	    				th = t;
	    		}
	    		field = next;
	    	}
	    }
	    
	    try {
	    	mConsumer.abort();
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
	}

	public boolean freeRAM() {
	    return mConsumer.freeRAM();
	}
	
	private DocConsumerPerField.List getFields() { 
		DocConsumerPerField.List fields = new DocConsumerPerField.List();
		
	    for (int i=0; i < mTotalFieldHash.length; i++) {
	        DocConsumerPerField field = mTotalFieldHash[i];
	        while (field != null) {
	        	fields.put(field.getFieldInfo().getName(), field);
	        	field = field.mNext;
	        }
	    }
	    assert fields.size() == mTotalFieldCount;
		
		return fields;
	}
	
	private void buildFieldHash(IFieldInfos.Builder fieldInfos, IDocument doc) 
			throws IOException { 
	    final int thisFieldGen = mDocFieldGen++;
	    
	    // Absorb any new fields first seen in this document.
	    // Also absorb any changes to fields we had already
	    // seen before (eg suddenly turning on norms or
	    // vectors, etc.):
	    mDocFieldCount = 0;
	    
		for (final IField field : doc) { 
			final String fieldName = field.getName(); 
			
			// Make sure we have a PerField allocated
			final int hashPos = fieldName.hashCode() & mTotalFieldHashMask;
			DocConsumerPerField fp = mTotalFieldHash[hashPos];
			while (fp != null && !fp.getFieldInfo().getName().equals(fieldName)) {
				fp = fp.mNext;
			}
			
			if (fp == null) {
		        // TODO FI: we need to genericize the "flags" that a
		        // field holds, and, how these flags are merged; it
		        // needs to be more "pluggable" such that if I want
		        // to have a new "thing" my Fields can do, I can
		        // easily add it
		        IFieldInfo fi = fieldInfos.addOrUpdate(fieldName, field.getFieldType());
		        
		        fp = new DocConsumerPerField(this, fi);
		        fp.mNext = mTotalFieldHash[hashPos];
		        mTotalFieldHash[hashPos] = fp;
		        mTotalFieldCount++;
		        
		        if (mTotalFieldCount >= mTotalFieldHash.length/2)
		        	rehashFieldHash();
		        
			} else { 
				fieldInfos.addOrUpdate(fp.getFieldInfo().getName(), 
						field.getFieldType());
			}
			
			if (thisFieldGen != fp.getLastGen()) {
				// First time we're seeing this field for this doc
				fp.clearFields();

				if (mDocFieldCount == mDocFields.length) {
					final int newSize = mDocFields.length*2;
					DocConsumerPerField newArray[] = new DocConsumerPerField[newSize];
		            System.arraycopy(mDocFields, 0, newArray, 0, mDocFieldCount);
		            mDocFields = newArray;
				}

				mDocFields[mDocFieldCount++] = fp;
				fp.setLastGen(thisFieldGen);
			}

			fp.addField(field);
			
			if (field.getFieldType().isStored()) 
				storeField(fp.getFieldInfo(), field);
		}
		
	    // If we are writing vectors then we must visit
	    // fields in sorted order so they are written in
	    // sorted order.  TODO: we actually only need to
	    // sort the subset of fields that have vectors
	    // enabled; we could save [small amount of] CPU
	    // here.
	    ArrayUtil.quickSort(mDocFields, 0, mDocFieldCount, sFieldsComp);
	    
	}
	
	private void rehashFieldHash() {
		final int newHashSize = (mTotalFieldHash.length*2);
		final DocConsumerPerField newHashArray[] = new DocConsumerPerField[newHashSize];

		// Rehash
		int newHashMask = newHashSize-1;
		for (int j=0; j < mTotalFieldHash.length; j++) {
			DocConsumerPerField fp0 = mTotalFieldHash[j];
			while (fp0 != null) {
				final int hashPos2 = fp0.getFieldInfo().getName().hashCode() & newHashMask;
				DocConsumerPerField nextFP0 = fp0.mNext;
		        fp0.mNext = newHashArray[hashPos2];
		        newHashArray[hashPos2] = fp0;
		        fp0 = nextFP0;
			}
		}

		mTotalFieldHash = newHashArray;
		mTotalFieldHashMask = newHashMask;
	}
	
	private static final Comparator<DocConsumerPerField> sFieldsComp = 
		new Comparator<DocConsumerPerField>() {
			public int compare(DocConsumerPerField o1, DocConsumerPerField o2) {
				return o1.getFieldInfo().getName().compareTo(o2.getFieldInfo().getName());
			}
		};
	
}
