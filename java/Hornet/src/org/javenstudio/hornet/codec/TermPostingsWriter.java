package org.javenstudio.hornet.codec;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IIndexOutput;
import org.javenstudio.common.indexdb.ITermState;
import org.javenstudio.common.indexdb.codec.ITermPostingsFormat;

/**
 * Extension of {@link PostingsConsumer} to support pluggable term dictionaries.
 * <p>
 * This class contains additional hooks to interact with the provided
 * term dictionaries such as {@link BlockTreeTermsWriter} and 
 * {@link BlockTermsWriter}. If you want to re-use one of these existing
 * implementations and are only interested in customizing the format of
 * the postings list, extend this class instead.
 * 
 * TODO: find a better name; this defines the API that the
 * terms dict impls use to talk to a postings impl.
 * TermsDict + PostingsReader/WriterBase == PostingsConsumer/Producer
 */
public abstract class TermPostingsWriter extends PostingsConsumer 
		implements ITermPostingsFormat.Writer {

	public abstract IDirectory getDirectory();
	
	public abstract void start(IIndexOutput termsOut) throws IOException;

	public abstract void startTerm() throws IOException;

	/** 
	 * Flush count terms starting at start "backwards", as a
	 *  block. start is a negative offset from the end of the
	 *  terms stack, ie bigger start means further back in
	 *  the stack. 
	 */
	public abstract void flushTermsBlock(int start, int count) throws IOException;

	/** Finishes the current term */
	public abstract void finishTerm(ITermState stats) throws IOException;

	public abstract void setField(IFieldInfo fieldInfo);

	public abstract void close() throws IOException;
	
}
