package org.javenstudio.common.indexdb.codec;

import java.io.Closeable;
import java.io.IOException;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IDocsAndPositionsEnum;
import org.javenstudio.common.indexdb.IDocsEnum;
import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IIndexInput;
import org.javenstudio.common.indexdb.IIndexOutput;
import org.javenstudio.common.indexdb.ITermState;
import org.javenstudio.common.indexdb.util.Bits;

public interface ITermPostingsFormat {

	public static interface Reader extends Closeable {

		public void init(IIndexInput termsIn) throws IOException;

		/** Return a newly created empty TermState */
		public ITermState newTermState() throws IOException;

		/** Actually decode metadata for next term */
		public void nextTerm(IFieldInfo fieldInfo, ITermState state) 
				throws IOException;

		/** 
		 * Must fully consume state, since after this call that
		 *  TermState may be reused. 
		 */
		public IDocsEnum getDocs(IFieldInfo fieldInfo, ITermState state, 
				Bits skipDocs, IDocsEnum reuse, int flags) throws IOException;

		/** 
		 * Must fully consume state, since after this call that
		 *  TermState may be reused. 
		 */
		public IDocsAndPositionsEnum getDocsAndPositions(
				IFieldInfo fieldInfo, ITermState state, Bits skipDocs, IDocsAndPositionsEnum reuse,
				int flags) throws IOException;

		/** 
		 * Reads data for all terms in the next block; this
		 *  method should merely load the byte[] blob but not
		 *  decode, which is done in {@link #nextTerm}. 
		 */
		public void readTermsBlock(IIndexInput termsIn, IFieldInfo fieldInfo, 
				ITermState termState) throws IOException;
		
		public void close() throws IOException;
		
	}
	
	public static interface Writer extends IPostingsConsumer, Closeable {

		public void start(IIndexOutput termsOut) throws IOException;

		public void startTerm() throws IOException;

		/** 
		 * Flush count terms starting at start "backwards", as a
		 *  block. start is a negative offset from the end of the
		 *  terms stack, ie bigger start means further back in
		 *  the stack. 
		 */
		public void flushTermsBlock(int start, int count) throws IOException;

		/** Finishes the current term */
		public void finishTerm(ITermState stats) throws IOException;

		public void setField(IFieldInfo fieldInfo);

		public void close() throws IOException;
		
	}
	
	public IIndexContext getContext();
	public IIndexFormat getIndexFormat();
	
	public String getTermsCodecName();
	public String getFreqCodecName();
	public String getProxCodecName();
	public String getFormatName();
	
	public String getPostingsFreqFileName(String segment);
	public String getPostingsProxFileName(String segment);
	
	public Writer createWriter(IDirectory dir, ISegmentWriteState state) 
			throws IOException;
	
	public Reader createReader(IDirectory dir, ISegmentReadState state) 
			throws IOException;
	
}
