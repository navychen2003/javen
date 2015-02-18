package org.javenstudio.hornet.codec;

import java.io.IOException;

import org.javenstudio.common.indexdb.IFields;
import org.javenstudio.common.indexdb.codec.ITermVectorsFormat;

/**
 * Codec API for reading term vectors:
 */
public abstract class TermVectorsReader implements ITermVectorsFormat.Reader {

	/** 
	 * Returns term vectors for this document, or null if
	 *  term vectors were not indexed. If offsets are
	 *  available they are in an {@link OffsetAttribute}
	 *  available from the {@link DocsAndPositionsEnum}. 
	 */
	public abstract IFields getFields(int doc) throws IOException;

	/** 
	 * Create a clone that one caller at a time may use to
	 *  read term vectors. 
	 */
	public abstract TermVectorsReader clone();
	
}
