package org.javenstudio.common.indexdb.index.consumer;

import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.index.DocumentWriter;

/** 
 * This is a DocFieldConsumer that inverts each field,
 *  separately, from a Document, and accepts a
 *  InvertedTermsConsumer to process those terms. 
 */
public final class DocumentInverter extends DocFieldConsumer {

	public DocumentInverter(DocumentWriter writer, 
			DocBeginConsumer beginConsumer, DocEndConsumer endConsumer) { 
		super(writer, beginConsumer, endConsumer);
	}
	
	@Override
	public DocFieldConsumerPerField addField(IFieldInfo fieldInfo) { 
		return new DocumentInverterPerField(this, fieldInfo);
	}
	
}
