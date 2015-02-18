package org.javenstudio.hornet.codec;

import java.io.IOException;

import org.javenstudio.common.indexdb.codec.IFieldsProducer;
import org.javenstudio.common.indexdb.document.AbstractFields;

/** 
 * Abstract API that produces terms, doc, freq, prox and
 *  payloads postings.  
 */
public abstract class FieldsProducer extends AbstractFields 
		implements IFieldsProducer {
	
	public abstract void close() throws IOException;
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{size=" + sizeNoThrow() + "}";
	}
	
	protected int sizeNoThrow() { 
		int size = -1;
		try { size = size(); } catch (Exception e) {}
		return size;
	}
	
}
