package org.javenstudio.common.indexdb.codec;

import java.io.Closeable;
import java.io.IOException;

import org.javenstudio.common.indexdb.CorruptIndexException;
import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IField;
import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IFieldInfos;
import org.javenstudio.common.indexdb.IFieldVisitor;
import org.javenstudio.common.indexdb.IMergeState;
import org.javenstudio.common.indexdb.ISegmentInfo;

/** Controls the format of stored fields */
public interface IFieldsFormat {

	public static interface Reader extends Cloneable, Closeable {

		/** Visit the stored fields for document <code>n</code> */
		public void visitDocument(int n, IFieldVisitor visitor) 
				throws CorruptIndexException, IOException;

		public Reader clone();
		
	}
	
	public static interface Writer extends Closeable {
		 
		/** 
		 * Called before writing the stored fields of the document.
		 *  {@link #writeField(FieldInfo, IndexableField)} will be called
		 *  <code>numStoredFields</code> times. Note that this is
		 *  called even if the document has no stored fields, in
		 *  this case <code>numStoredFields</code> will be zero. 
		 */
		public void startDocument(int numStoredFields) throws IOException;
	  
		/** Writes a single stored field. */
		public void writeField(IFieldInfo info, IField field) throws IOException;

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
		 * Merges in the stored fields from the readers in 
		 *  <code>mergeState</code>. The default implementation skips
		 *  over deleted documents, and uses {@link #startDocument(int)},
		 *  {@link #writeField(FieldInfo, IndexableField)}, and {@link #finish(FieldInfos, int)},
		 *  returning the number of documents that were written.
		 *  Implementations can override this method for more sophisticated
		 *  merging (bulk-byte copying, etc). 
		 */
		public int merge(IMergeState mergeState) throws IOException;
		
	}
	
	public IIndexContext getContext();
	public IIndexFormat getIndexFormat();
	
	public String getIndexCodecName();
	public String getDataCodecName();
	
	public String getFieldsFileName(String segment);
	public String getFieldsIndexFileName(String segment);
	
	public Reader createReader(IDirectory dir, String segment, 
			ISegmentInfo si, IFieldInfos fn) throws IOException;
	
	public Writer createWriter(IDirectory dir, String segment) 
			throws IOException;
	
}
