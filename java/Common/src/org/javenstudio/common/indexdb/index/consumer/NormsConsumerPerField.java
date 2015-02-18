package org.javenstudio.common.indexdb.index.consumer;

import org.javenstudio.common.indexdb.IFieldInfo;

final class NormsConsumerPerField extends DocEndConsumerPerField {

	public NormsConsumerPerField(DocumentInverterPerField inverterPerField, 
			NormsConsumer consumer, IFieldInfo fieldInfo) { 
		super(inverterPerField, consumer, fieldInfo);
	}
	
}
