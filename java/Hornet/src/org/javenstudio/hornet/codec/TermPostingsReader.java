package org.javenstudio.hornet.codec;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IDocsAndPositionsEnum;
import org.javenstudio.common.indexdb.IDocsEnum;
import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IIndexInput;
import org.javenstudio.common.indexdb.ITermState;
import org.javenstudio.common.indexdb.codec.ITermPostingsFormat;
import org.javenstudio.common.indexdb.util.Bits;

/** 
 * The core terms dictionaries (BlockTermsReader,
 *  BlockTreeTermsReader) interact with a single instance
 *  of this class to manage creation of {@link DocsEnum} and
 *  {@link DocsAndPositionsEnum} instances.  It provides an
 *  IndexInput (termsIn) where this class may read any
 *  previously stored data that it had written in its
 *  corresponding {@link PostingsWriterBase} at indexing
 *  time. 
 *
 * TODO: find a better name; this defines the API that the
 * terms dict impls use to talk to a postings impl.
 * TermsDict + PostingsReader/WriterBase == PostingsConsumer/Producer
 */
public abstract class TermPostingsReader implements ITermPostingsFormat.Reader {

	public abstract IDirectory getDirectory();
	
	public abstract void init(IIndexInput termsIn) throws IOException;

	/** Return a newly created empty TermState */
	public abstract ITermState newTermState() throws IOException;

	/** Actually decode metadata for next term */
	public abstract void nextTerm(IFieldInfo fieldInfo, ITermState state) 
			throws IOException;

	/** 
	 * Must fully consume state, since after this call that
	 *  TermState may be reused. 
	 */
	public abstract IDocsEnum getDocs(IFieldInfo fieldInfo, ITermState state, 
			Bits skipDocs, IDocsEnum reuse, int flags) throws IOException;

	/** 
	 * Must fully consume state, since after this call that
	 *  TermState may be reused. 
	 */
	public abstract IDocsAndPositionsEnum getDocsAndPositions(
			IFieldInfo fieldInfo, ITermState state, Bits skipDocs, IDocsAndPositionsEnum reuse,
			int flags) throws IOException;

	/** 
	 * Reads data for all terms in the next block; this
	 *  method should merely load the byte[] blob but not
	 *  decode, which is done in {@link #nextTerm}. 
	 */
	public abstract void readTermsBlock(IIndexInput termsIn, IFieldInfo fieldInfo, 
			ITermState termState) throws IOException;
	
	public abstract void close() throws IOException;
	
}
