package org.javenstudio.common.indexdb.codec;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDocsEnum;
import org.javenstudio.common.indexdb.IFixedBitSet;
import org.javenstudio.common.indexdb.IMergeState;
import org.javenstudio.common.indexdb.ITermState;
import org.javenstudio.common.indexdb.util.BytesRef;

public interface IPostingsConsumer {

	/** Adds a new doc in this term. */
	public void startDoc(int docID, int freq) throws IOException;

	/** 
	 * Add a new position & payload, and start/end offset.  A
	 *  null payload means no payload; a non-null payload with
	 *  zero length also means no payload.  Caller may reuse
	 *  the {@link BytesRef} for the payload between calls
	 *  (method must fully consume the payload). 
	 */
	public void addPosition(int position, BytesRef payload, 
			int startOffset, int endOffset) throws IOException;

	/** 
	 * Called when we are done adding positions & payloads
	 * for each doc. 
	 */
	public void finishDoc() throws IOException;

	/** 
	 * Default merge impl: append documents, mapping around 
	 * deletes 
	 */
	public ITermState merge(final IMergeState mergeState, 
			final IDocsEnum postings, final IFixedBitSet visitedDocs) 
			throws IOException;
	
}
