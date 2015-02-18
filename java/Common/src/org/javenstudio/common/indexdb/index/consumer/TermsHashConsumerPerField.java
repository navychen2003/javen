package org.javenstudio.common.indexdb.index.consumer;

import java.io.IOException;
import java.util.HashMap;

import org.javenstudio.common.indexdb.IField;
import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IToken;

abstract class TermsHashConsumerPerField {

	public static class List extends HashMap<String, TermsHashConsumerPerField> { 
		private static final long serialVersionUID = 1L;
		public List() { super(); }
	}
	
	private final TermsHashPerField mTermsHashPerField;
	private final TermsHashConsumer mTermsHashConsumer;
	private final IFieldInfo mFieldInfo;
	
	protected TermsHashConsumerPerField(
			TermsHashPerField termsHashPerField, TermsHashConsumer termsHashConsumer, 
			IFieldInfo fieldInfo) { 
		mTermsHashPerField = termsHashPerField;
		mTermsHashConsumer = termsHashConsumer;
		mFieldInfo = fieldInfo;
	}
	
	final TermsHashPerField getTermsHashPerField() { return mTermsHashPerField; }
	final TermsHashConsumer getTermsHashConsumer() { return mTermsHashConsumer; }
	final IFieldInfo getFieldInfo() { return mFieldInfo; }
	
	protected abstract ParallelPostingsArray createPostingsArray(int size);
	
	public abstract boolean start(IField[] fields, int count) throws IOException;
	
	public abstract void newTerm(int termID, IToken token) throws IOException;
	public abstract void addTerm(int termID, IToken token) throws IOException;
	
	public abstract int getStreamCount();
	
	public void skippingLongTerm(IToken token) throws IOException {}
	public void start(IField field) {}
	public void finish() throws IOException {}
	
}
