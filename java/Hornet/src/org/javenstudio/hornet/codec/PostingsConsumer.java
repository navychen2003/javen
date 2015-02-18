package org.javenstudio.hornet.codec;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDocIdSetIterator;
import org.javenstudio.common.indexdb.IDocsEnum;
import org.javenstudio.common.indexdb.IFixedBitSet;
import org.javenstudio.common.indexdb.IMergeState;
import org.javenstudio.common.indexdb.ITermState;
import org.javenstudio.common.indexdb.IndexOptions;
import org.javenstudio.common.indexdb.codec.IPostingsConsumer;
import org.javenstudio.common.indexdb.index.term.DocsAndPositionsEnum;
import org.javenstudio.common.indexdb.index.term.PerTermState;
import org.javenstudio.common.indexdb.util.BytesRef;

/**
 * Abstract API that consumes postings for an individual term.
 * <p>
 * The lifecycle is:
 * <ol>
 *    <li>PostingsConsumer is returned for each term by
 *        {@link TermsConsumer#startTerm(BytesRef)}. 
 *    <li>{@link #startDoc(int, int)} is called for each
 *        document where the term occurs, specifying id 
 *        and term frequency for that document.
 *    <li>If positions are enabled for the field, then
 *        {@link #addPosition(int, BytesRef, int, int)}
 *        will be called for each occurrence in the 
 *        document.
 *    <li>{@link #finishDoc()} is called when the producer
 *        is done adding positions to the document.
 * </ol>
 * 
 */
public abstract class PostingsConsumer implements IPostingsConsumer {

	/** Adds a new doc in this term. */
	public abstract void startDoc(int docID, int freq) throws IOException;

	/** 
	 * Add a new position & payload, and start/end offset.  A
	 *  null payload means no payload; a non-null payload with
	 *  zero length also means no payload.  Caller may reuse
	 *  the {@link BytesRef} for the payload between calls
	 *  (method must fully consume the payload). 
	 */
	public abstract void addPosition(int position, BytesRef payload, 
			int startOffset, int endOffset) throws IOException;

	/** 
	 * Called when we are done adding positions & payloads
	 * for each doc. 
	 */
	public abstract void finishDoc() throws IOException;

	/** 
	 * Default merge impl: append documents, mapping around 
	 * deletes 
	 */
	@Override
	public ITermState merge(final IMergeState mergeState, 
			final IDocsEnum postings, final IFixedBitSet visitedDocs) throws IOException {
	    int df = 0;
	    long totTF = 0;

	    IndexOptions indexOptions = mergeState.getFieldInfo().getIndexOptions();
	    if (indexOptions == IndexOptions.DOCS_ONLY) {
	    	while (true) {
	    		final int doc = postings.nextDoc();
	    		if (doc == IDocIdSetIterator.NO_MORE_DOCS) 
	    			break;
	    		
	    		visitedDocs.set(doc);
	    		
	    		this.startDoc(doc, -1);
	    		this.finishDoc();
	    		
	    		df++;
	    	}
	    	totTF = -1;
	    	
	    } else if (indexOptions == IndexOptions.DOCS_AND_FREQS) {
	    	while (true) {
	    		final int doc = postings.nextDoc();
	    		if (doc == IDocIdSetIterator.NO_MORE_DOCS) 
	    			break;
	    		
	    		visitedDocs.set(doc);
	    		final int freq = postings.getFreq();
	    		
	    		this.startDoc(doc, freq);
	    		this.finishDoc();
	    		
	    		df++;
	    		totTF += freq;
	    	}
	    	
	    } else if (indexOptions == IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) {
	    	final DocsAndPositionsEnum postingsEnum = (DocsAndPositionsEnum) postings;
	    	while (true) {
	    		final int doc = postingsEnum.nextDoc();
	    		if (doc == IDocIdSetIterator.NO_MORE_DOCS) 
	    			break;
	    		
	    		visitedDocs.set(doc);
	    		final int freq = postingsEnum.getFreq();
	    		this.startDoc(doc, freq);
	    		
	    		totTF += freq;
	    		
	    		for (int i=0; i < freq; i++) {
	    			final int position = postingsEnum.nextPosition();
	    			final BytesRef payload = postingsEnum.getPayload();
	    			this.addPosition(position, payload, -1, -1);
	    		}
	    		
	    		this.finishDoc();
	    		df++;
	    	}
	    	
	    } else {
	    	assert indexOptions == IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS;
	    	final DocsAndPositionsEnum postingsEnum = (DocsAndPositionsEnum) postings;
	    	while (true) {
	    		final int doc = postingsEnum.nextDoc();
	    		if (doc == IDocIdSetIterator.NO_MORE_DOCS) 
	    			break;
	    		
	    		visitedDocs.set(doc);
	    		final int freq = postingsEnum.getFreq();
	    		this.startDoc(doc, freq);
	    		
	    		totTF += freq;
	    		
	    		for (int i=0; i < freq; i++) {
	    			final int position = postingsEnum.nextPosition();
	    			final BytesRef payload = postingsEnum.getPayload();
	    			this.addPosition(position, payload, 
	    					postingsEnum.startOffset(), postingsEnum.endOffset());
	    		}
	    		
	    		this.finishDoc();
	    		df++;
	    	}
	    }
	    
	    return new PerTermState(df, indexOptions == IndexOptions.DOCS_ONLY ? -1 : totTF);
	}
	
}
