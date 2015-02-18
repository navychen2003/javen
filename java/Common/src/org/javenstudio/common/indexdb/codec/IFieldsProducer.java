package org.javenstudio.common.indexdb.codec;

import java.io.Closeable;
import java.io.IOException;

import org.javenstudio.common.indexdb.IFields;

/** 
 * Abstract API that produces terms, doc, freq, prox and
 *  payloads postings.  
 */
public interface IFieldsProducer extends IFields, Closeable {

	public void close() throws IOException;
	
}
