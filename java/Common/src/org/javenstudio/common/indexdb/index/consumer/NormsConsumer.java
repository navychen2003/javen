package org.javenstudio.common.indexdb.index.consumer;

import java.io.IOException;

import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.codec.ISegmentWriteState;
import org.javenstudio.common.indexdb.index.DocumentWriter;

//TODO FI: norms could actually be stored as doc store

/** 
 * Writes norms.  Each thread X field accumulates the norms
 *  for the doc/fields it saw, then the flush method below
 *  merges all of these together into a single _X.nrm file.
 */
public class NormsConsumer extends DocEndConsumer {

	public NormsConsumer(DocumentWriter writer) { 
		super(writer);
	}
	
	@Override
	public DocEndConsumerPerField addField(DocFieldConsumerPerField consumer, IFieldInfo fieldInfo) { 
		return new NormsConsumerPerField((DocumentInverterPerField)consumer, this, fieldInfo);
	}
	
	@Override
	public void startDocument() throws IOException {}
	
	@Override
	public void finishDocument() throws IOException {}
	
	@Override
	public void flush(DocEndConsumerPerField.List fields, ISegmentWriteState state) throws IOException {
	}
	
	@Override
	public void abort() {}
	
}
