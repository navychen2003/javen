package org.javenstudio.hornet.codec;

import java.io.IOException;
import java.util.Comparator;

import org.javenstudio.common.indexdb.IAtomicReader;
import org.javenstudio.common.indexdb.IDataInput;
import org.javenstudio.common.indexdb.IDocIdSetIterator;
import org.javenstudio.common.indexdb.IDocsAndPositionsEnum;
import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IFieldInfos;
import org.javenstudio.common.indexdb.IFields;
import org.javenstudio.common.indexdb.IFieldsEnum;
import org.javenstudio.common.indexdb.IMergeState;
import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.codec.ITermVectorsFormat;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.indexdb.util.BytesRef;

/**
 * Codec API for writing term vectors:
 * <p>
 * <ol>
 *   <li>For every document, {@link #startDocument(int)} is called,
 *       informing the Codec how many fields will be written.
 *   <li>{@link #startField(FieldInfo, int, boolean, boolean)} is called for 
 *       each field in the document, informing the codec how many terms
 *       will be written for that field, and whether or not positions
 *       or offsets are enabled.
 *   <li>Within each field, {@link #startTerm(BytesRef, int)} is called
 *       for each term.
 *   <li>If offsets and/or positions are enabled, then 
 *       {@link #addPosition(int, int, int)} will be called for each term
 *       occurrence.
 *   <li>After all documents have been written, {@link #finish(FieldInfos, int)} 
 *       is called for verification/sanity-checks.
 *   <li>Finally the writer is closed ({@link #close()})
 * </ol>
 * 
 */
public abstract class TermVectorsWriter implements ITermVectorsFormat.Writer {
  
	/** 
	 * Called before writing the term vectors of the document.
	 *  {@link #startField(FieldInfo, int, boolean, boolean)} will 
	 *  be called <code>numVectorFields</code> times. Note that if term 
	 *  vectors are enabled, this is called even if the document 
	 *  has no vector fields, in this case <code>numVectorFields</code> 
	 *  will be zero. 
	 */
	public abstract void startDocument(int numVectorFields) throws IOException;
  
	/** 
	 * Called before writing the terms of the field.
	 *  {@link #startTerm(BytesRef, int)} will be called <code>numTerms</code> times. 
	 */
	public abstract void startField(IFieldInfo info, int numTerms, 
			boolean positions, boolean offsets) throws IOException;
  
	/** 
	 * Adds a term and its term frequency <code>freq</code>.
	 * If this field has positions and/or offsets enabled, then
	 * {@link #addPosition(int, int, int)} will be called 
	 * <code>freq</code> times respectively.
	 */
	public abstract void startTerm(BytesRef term, int freq) throws IOException;
  
	/** Adds a term position and offsets */
	public abstract void addPosition(int position, int startOffset, int endOffset) 
			throws IOException;
  
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
	 *
	 * TODO: we should probably nuke this and make a more efficient 4.x format
	 * PreFlex-RW could then be slow and buffer (its only used in tests...)
	 */
	@Override
	public void addProx(int numProx, IDataInput positions, IDataInput offsets) 
			throws IOException {
		int position = 0;
		int lastOffset = 0;

		for (int i = 0; i < numProx; i++) {
			final int startOffset;
			final int endOffset;
      
			if (positions == null) 
				position = -1;
			else 
				position += positions.readVInt();
      
			if (offsets == null) {
				startOffset = endOffset = -1;
			} else {
				startOffset = lastOffset + offsets.readVInt();
				endOffset = startOffset + offsets.readVInt();
				lastOffset = endOffset;
			}
			
			addPosition(position, startOffset, endOffset);
		}
	}
  
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
	@Override
	public int merge(IMergeState mergeState) throws IOException {
	    int docCount = 0;
	    
	    for (int idx=0; idx < mergeState.getReaderCount(); idx ++) {
	    	final IAtomicReader reader = mergeState.getReaderAt(idx);
	    	final int maxDoc = reader.getMaxDoc();
	    	final Bits liveDocs = reader.getLiveDocs();
	    	
	    	for (int docID = 0; docID < maxDoc; docID++) {
	    		if (liveDocs != null && !liveDocs.get(docID)) {
	    			// skip deleted docs
	    			continue;
	    		}
	    		
	    		// NOTE: it's very important to first assign to vectors then pass it to
	    		// termVectorsWriter.addAllDocVectors; see LUCENE-1282
	    		IFields vectors = reader.getTermVectors(docID);
	    		addAllDocVectors(vectors, mergeState.getFieldInfos());
	    		docCount++;
	    		
	    		mergeState.checkAbort(300);
	    	}
	    }
	    
	    finish(mergeState.getFieldInfos(), docCount);
	    
	    return docCount;
	}
  
	/** 
	 * Safe (but, slowish) default method to write every
	 *  vector field in the document.  This default
	 *  implementation requires that the vectors implement
	 *  both Fields.size and Terms.size. 
	 */
	protected final void addAllDocVectors(IFields vectors, IFieldInfos fieldInfos) 
			throws IOException {
		if (vectors == null) {
	    	startDocument(0);
	    	return;
	    }
	
	    final int numFields = vectors.size();
	    if (numFields == -1) 
	    	throw new IllegalStateException("vectors.size() must be implemented (it returned -1)");
	    
	    startDocument(numFields);
	    
	    final IFieldsEnum fieldsEnum = vectors.iterator();
	    String fieldName;
	    String lastFieldName = null;
	
	    while ((fieldName = fieldsEnum.next()) != null) {
	    	final IFieldInfo fieldInfo = fieldInfos.getFieldInfo(fieldName);
	
	    	assert lastFieldName == null || fieldName.compareTo(lastFieldName) > 0 : 
	    		"lastFieldName=" + lastFieldName + " fieldName=" + fieldName;
	    	lastFieldName = fieldName;
	
	    	final ITerms terms = fieldsEnum.getTerms();
	    	if (terms == null) {
	    		// FieldsEnum shouldn't lie...
	    		continue;
	    	}
	    	
	    	final int numTerms = (int) terms.size();
	    	if (numTerms == -1) 
	    		throw new IllegalStateException("terms.size() must be implemented (it returned -1)");
	    	
	    	final ITermsEnum termsEnum = terms.iterator(null);
	    	
	    	IDocsAndPositionsEnum docsAndPositionsEnum = null;
	    	boolean startedField = false;
	
	    	// NOTE: this is tricky, because TermVectors allow
	    	// indexing offsets but NOT positions.  So we must
	    	// lazily init the field by checking whether first
	    	// position we see is -1 or not.
	
	    	int termCount = 0;
	    	while (termsEnum.next() != null) {
	    		termCount ++;
	
	    		final int freq = (int) termsEnum.getTotalTermFreq();
	    		if (startedField) 
	    			startTerm(termsEnum.getTerm(), freq);
	
	    		// TODO: we need a "query" API where we can ask (via
	    		// flex API) what this term was indexed with...
	    		// Both positions & offsets:
	    		docsAndPositionsEnum = termsEnum.getDocsAndPositions(null, null);
	    		final boolean hasOffsets;
	    		boolean hasPositions = false;
	    		if (docsAndPositionsEnum == null) {
	    			// Fallback: no offsets
	    			docsAndPositionsEnum = termsEnum.getDocsAndPositions(null, null, 0);
	    			hasOffsets = false;
	    		} else {
	    			hasOffsets = true;
	    		}
	
	    		if (docsAndPositionsEnum != null) {
	    			final int docID = docsAndPositionsEnum.nextDoc();
	    			assert docID != IDocIdSetIterator.NO_MORE_DOCS;
	    			assert docsAndPositionsEnum.getFreq() == freq;
	
	    			for (int posUpto=0; posUpto < freq; posUpto++) {
	    				final int pos = docsAndPositionsEnum.nextPosition();
	    				if (!startedField) {
	    					assert numTerms > 0;
	    					hasPositions = pos != -1;
	    					startField(fieldInfo, numTerms, hasPositions, hasOffsets);
	    					startTerm(termsEnum.getTerm(), freq);
	    					startedField = true;
	    				}
	    				final int startOffset;
	    				final int endOffset;
	    				if (hasOffsets) {
	    					startOffset = docsAndPositionsEnum.startOffset();
	    					endOffset = docsAndPositionsEnum.endOffset();
	    					assert startOffset != -1;
	    					assert endOffset != -1;
	    				} else {
	    					startOffset = -1;
	    					endOffset = -1;
	    				}
	    				assert !hasPositions || pos >= 0;
	    				addPosition(pos, startOffset, endOffset);
	    			}
	    		} else {
	    			if (!startedField) {
	    				assert numTerms > 0;
	    				startField(fieldInfo, numTerms, hasPositions, hasOffsets);
	    				startTerm(termsEnum.getTerm(), freq);
	    				startedField = true;
	    			}
	    		}
	    	}
	    	
	    	assert termCount == numTerms;
    	}
	}
  
	/** 
	 * Return the BytesRef Comparator used to sort terms
	 *  before feeding to this API. 
	 */
	public abstract Comparator<BytesRef> getComparator() throws IOException;
	
}
