package org.javenstudio.hornet.codec;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReader;
import org.javenstudio.common.indexdb.IDocument;
import org.javenstudio.common.indexdb.IField;
import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IFieldInfos;
import org.javenstudio.common.indexdb.IMergeState;
import org.javenstudio.common.indexdb.codec.IFieldsFormat;
import org.javenstudio.common.indexdb.util.Bits;

/**
 * Codec API for writing stored fields:
 * <p>
 * <ol>
 *   <li>For every document, {@link #startDocument(int)} is called,
 *       informing the Codec how many fields will be written.
 *   <li>{@link #writeField(FieldInfo, IndexableField)} is called for 
 *       each field in the document.
 *   <li>After all documents have been written, {@link #finish(FieldInfos, int)} 
 *       is called for verification/sanity-checks.
 *   <li>Finally the writer is closed ({@link #close()})
 * </ol>
 * 
 */
public abstract class FieldsWriter implements IFieldsFormat.Writer {
	//private static final Logger LOG = Logger.getLogger(FieldsWriter.class);
 
	/** 
	 * Called before writing the stored fields of the document.
	 *  {@link #writeField(FieldInfo, IndexableField)} will be called
	 *  <code>numStoredFields</code> times. Note that this is
	 *  called even if the document has no stored fields, in
	 *  this case <code>numStoredFields</code> will be zero. 
	 */
	public abstract void startDocument(int numStoredFields) throws IOException;
  
	/** Writes a single stored field. */
	public abstract void writeField(IFieldInfo info, IField field) throws IOException;

	/** 
	 * Aborts writing entirely, implementation should remove
	 *  any partially-written files, etc. 
	 */
	public abstract void abort();
  
	/** 
	 * Called before {@link #close()}, passing in the number
	 *  of documents that were written. Note that this is 
	 *  intentionally redundant (equivalent to the number of
	 *  calls to {@link #startDocument(int)}, but a Codec should
	 *  check that this is the case to detect the JRE bug described 
	 *  in LUCENE-1282. 
	 */
	public abstract void finish(IFieldInfos fis, int numDocs) throws IOException;
  
	/** 
	 * Merges in the stored fields from the readers in 
	 *  <code>mergeState</code>. The default implementation skips
	 *  over deleted documents, and uses {@link #startDocument(int)},
	 *  {@link #writeField(FieldInfo, IndexableField)}, and {@link #finish(FieldInfos, int)},
	 *  returning the number of documents that were written.
	 *  Implementations can override this method for more sophisticated
	 *  merging (bulk-byte copying, etc). 
	 */
	@Override
	public int merge(IMergeState mergeState) throws IOException {
	    int docCount = 0;
	    
	    for (int idx=0; idx < mergeState.getReaderCount(); idx ++) {
	    	final IAtomicReader reader = mergeState.getReaderAt(idx);
	    	final int maxDoc = reader.getMaxDoc();
	    	final Bits liveDocs = reader.getLiveDocs();
	    	
	    	for (int i = 0; i < maxDoc; i++) {
	    		if (liveDocs != null && !liveDocs.get(i)) {
	    			// skip deleted docs
	    			continue;
	    		}
	    		
	    		// TODO: this could be more efficient using
	    		// FieldVisitor instead of loading/writing entire
	    		// doc; ie we just have to renumber the field number
	    		// on the fly?
	    		// NOTE: it's very important to first assign to doc then pass it to
	    		// fieldsWriter.addDocument; see LUCENE-1282
	    		IDocument doc = reader.getDocument(i);
	    		addDocument(doc, mergeState.getFieldInfos());
	    		docCount++;
	    		
	    		mergeState.checkAbort(300);
	    	}
	    }
	    
	    finish(mergeState.getFieldInfos(), docCount);
	    
	    return docCount;
	}
  
	/** sugar method for startDocument() + writeField() for every stored field in the document */
	protected final void addDocument(IDocument doc, IFieldInfos fieldInfos) throws IOException {
		int storedCount = 0;
		for (IField field : doc) {
			if (field.getFieldType().isStored()) 
				storedCount++;
		}
    
		startDocument(storedCount);

		for (IField field : doc) {
			if (field.getFieldType().isStored()) 
				writeField(fieldInfos.getFieldInfo(field.getName()), field);
		}
		
		//if (LOG.isDebugEnabled())
		//	LOG.debug("addDocument: " + doc + " storedCount=" + storedCount);
	}

}
