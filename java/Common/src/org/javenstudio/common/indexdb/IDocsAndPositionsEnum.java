package org.javenstudio.common.indexdb;

import java.io.IOException;

import org.javenstudio.common.indexdb.util.BytesRef;

public interface IDocsAndPositionsEnum extends IDocsEnum {

	/** 
	 * Flag to pass to {@link TermsEnum#docsAndPositions(Bits,DocsAndPositionsEnum,int)}
	 *  if you require offsets in the returned enum. 
	 */
	public static final int FLAG_OFFSETS = 0x1;

	/** 
	 * Flag to pass to  {@link TermsEnum#docsAndPositions(Bits,DocsAndPositionsEnum,int)}
	 *  if you require payloads in the returned enum. 
	 */
	public static final int FLAG_PAYLOADS = 0x2;
	
	/** 
	 * Returns the next position.  You should only call this
	 *  up to {@link DocsEnum#freq()} times else
	 *  the behavior is not defined.  If positions were not
	 *  indexed this will return -1; this only happens if
	 *  offsets were indexed and you passed needsOffset=true
	 *  when pulling the enum.  
	 */
	public int nextPosition() throws IOException;

	/** 
	 * Returns start offset for the current position, or -1
	 *  if offsets were not indexed. 
	 */
	public int startOffset() throws IOException;

	/** 
	 * Returns end offset for the current position, or -1 if
	 *  offsets were not indexed. 
	 */
	public int endOffset() throws IOException;

	/** 
	 * Returns the payload at this position, or null if no
	 *  payload was indexed.  Only call this once per
	 *  position. 
	 */
	public BytesRef getPayload() throws IOException;

	/** @return true if has payload data. */
	//public boolean hasPayload();
	
}
