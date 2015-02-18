package org.javenstudio.common.indexdb.index.term;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDocsAndPositionsEnum;
import org.javenstudio.common.indexdb.util.BytesRef;

/** Also iterates through positions. */
public abstract class DocsAndPositionsEnum extends DocsEnum 
		implements IDocsAndPositionsEnum {

	/** 
	 * Returns the next position.  You should only call this
	 *  up to {@link DocsEnum#freq()} times else
	 *  the behavior is not defined.  If positions were not
	 *  indexed this will return -1; this only happens if
	 *  offsets were indexed and you passed needsOffset=true
	 *  when pulling the enum.  
	 */
	public abstract int nextPosition() throws IOException;

	/** 
	 * Returns start offset for the current position, or -1
	 *  if offsets were not indexed. 
	 */
	public abstract int startOffset() throws IOException;

	/** 
	 * Returns end offset for the current position, or -1 if
	 *  offsets were not indexed. 
	 */
	public abstract int endOffset() throws IOException;

	/** 
	 * Returns the payload at this position, or null if no
	 *  payload was indexed.  Only call this once per
	 *  position. 
	 */
	public abstract BytesRef getPayload() throws IOException;

	/** @return true if has payload data. */
	//public abstract boolean hasPayload();
	
}
