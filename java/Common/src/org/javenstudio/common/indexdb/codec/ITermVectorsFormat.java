package org.javenstudio.common.indexdb.codec;

import java.io.Closeable;
import java.io.IOException;
import java.util.Comparator;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDataInput;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IFieldInfos;
import org.javenstudio.common.indexdb.IFields;
import org.javenstudio.common.indexdb.IMergeState;
import org.javenstudio.common.indexdb.ISegmentInfo;
import org.javenstudio.common.indexdb.util.BytesRef;

public interface ITermVectorsFormat {

	public static interface Reader extends Cloneable, Closeable {

		/** 
		 * Returns term vectors for this document, or null if
		 *  term vectors were not indexed. If offsets are
		 *  available they are in an {@link OffsetAttribute}
		 *  available from the {@link DocsAndPositionsEnum}. 
		 */
		public IFields getFields(int doc) throws IOException;

		/** 
		 * Create a clone that one caller at a time may use to
		 *  read term vectors. 
		 */
		public Reader clone();
		
	}
	
	public static interface Writer extends Closeable {
		  
		/** 
		 * Called before writing the term vectors of the document.
		 *  {@link #startField(FieldInfo, int, boolean, boolean)} will 
		 *  be called <code>numVectorFields</code> times. Note that if term 
		 *  vectors are enabled, this is called even if the document 
		 *  has no vector fields, in this case <code>numVectorFields</code> 
		 *  will be zero. 
		 */
		public void startDocument(int numVectorFields) throws IOException;
	  
		/** 
		 * Called before writing the terms of the field.
		 *  {@link #startTerm(BytesRef, int)} will be called <code>numTerms</code> times. 
		 */
		public void startField(IFieldInfo info, int numTerms, 
				boolean positions, boolean offsets) throws IOException;
	  
		/** 
		 * Adds a term and its term frequency <code>freq</code>.
		 * If this field has positions and/or offsets enabled, then
		 * {@link #addPosition(int, int, int)} will be called 
		 * <code>freq</code> times respectively.
		 */
		public void startTerm(BytesRef term, int freq) throws IOException;
	  
		/** Adds a term position and offsets */
		public void addPosition(int position, int startOffset, int endOffset) 
				throws IOException;
	  
		/** 
		 * Aborts writing entirely, implementation should remove
		 *  any partially-written files, etc. 
		 */
		public void abort();

		/** 
		 * Called before {@link #close()}, passing in the number
		 *  of documents that were written. Note that this is 
		 *  intentionally redundant (equivalent to the number of
		 *  calls to {@link #startDocument(int)}, but a Codec should
		 *  check that this is the case to detect the JRE bug described 
		 *  in LUCENE-1282. 
		 */
		public void finish(IFieldInfos fis, int numDocs) throws IOException;
	  
		/** 
		 * Called by IndexWriter when writing new segments.
		 * <p>
		 * This is an expert API that allows the codec to consume 
		 * positions and offsets directly from the indexer.
		 * <p>
		 * The default implementation calls {@link #addPosition(int, int, int)},
		 * but subclasses can override this if they want to efficiently write 
		 * all the positions, then all the offsets, for example.
		 * <p>
		 * NOTE: This API is extremely expert and subject to change or removal!!!
		 */
		public void addProx(int numProx, IDataInput positions, IDataInput offsets) 
				throws IOException;
	  
		/** 
		 * Merges in the term vectors from the readers in 
		 *  <code>mergeState</code>. The default implementation skips
		 *  over deleted documents, and uses {@link #startDocument(int)},
		 *  {@link #startField(FieldInfo, int, boolean, boolean)}, 
		 *  {@link #startTerm(BytesRef, int)}, {@link #addPosition(int, int, int)},
		 *  and {@link #finish(FieldInfos, int)},
		 *  returning the number of documents that were written.
		 *  Implementations can override this method for more sophisticated
		 *  merging (bulk-byte copying, etc). 
		 */
		public int merge(IMergeState mergeState) throws IOException;
	  
		/** 
		 * Return the BytesRef Comparator used to sort terms
		 *  before feeding to this API. 
		 */
		public Comparator<BytesRef> getComparator() throws IOException;
		
	}
	
	public IIndexContext getContext();
	public IIndexFormat getIndexFormat();
	
	public String getVectorsIndexFileName(String segment);
	public String getVectorsDocumentsFileName(String segment);
	public String getVectorsFieldsFileName(String segment);
	
	public String getFieldsCodecName();
	public String getDocumentsCodecName();
	public String getIndexCodecName();
	
	public ITermVectorsFormat.Reader createReader(IDirectory dir, ISegmentInfo si, 
			IFieldInfos fieldInfos) throws IOException;
	
	public ITermVectorsFormat.Writer createWriter(IDirectory dir, String segment) 
			throws IOException;
	
}
