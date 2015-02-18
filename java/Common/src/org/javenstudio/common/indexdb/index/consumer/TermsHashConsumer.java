package org.javenstudio.common.indexdb.index.consumer;

import java.io.IOException;

import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.codec.ISegmentWriteState;
import org.javenstudio.common.indexdb.index.DocumentWriter;

abstract class TermsHashConsumer {

	private final DocumentWriter mWriter;
	
	protected TermsHashConsumer(DocumentWriter writer) { 
		mWriter = writer;
	}
	
	final DocumentWriter getDocumentWriter() { 
		return mWriter;
	}
	
	public abstract TermsHashConsumerPerField addField(
			TermsHashPerField termsHashPerField, IFieldInfo fieldInfo);
	
	public void startDocument() throws IOException {}
	public void finishDocument(TermsHash termsHash) throws IOException {}
	public void flush(TermsHashConsumerPerField.List fieldsToFlush, ISegmentWriteState state) 
			throws IOException {}
	public void abort() {}
	
}
