package org.javenstudio.common.indexdb.index;

import org.javenstudio.common.indexdb.index.consumer.DocConsumer;
import org.javenstudio.common.indexdb.index.consumer.DocumentInverter;
import org.javenstudio.common.indexdb.index.consumer.DocumentProcessor;
import org.javenstudio.common.indexdb.index.consumer.FreqProxConsumer;
import org.javenstudio.common.indexdb.index.consumer.NormsConsumer;
import org.javenstudio.common.indexdb.index.consumer.TermVectorsConsumer;
import org.javenstudio.common.indexdb.index.consumer.TermsHash;

public abstract class IndexingChain {
	
	public abstract DocConsumer createChain(DocumentWriter writer);
	
	static final IndexingChain sDefaultChain = new IndexingChain() { 
			@Override
			public DocConsumer createChain(DocumentWriter writer) {
			      /**
			      This is the current indexing chain:

			      DocConsumer / DocConsumerPerThread
			        --> code: DocFieldProcessor / DocFieldProcessorPerThread
			          --> DocFieldConsumer / DocFieldConsumerPerThread / DocFieldConsumerPerField
			            --> code: DocFieldConsumers / DocFieldConsumersPerThread / DocFieldConsumersPerField
			              --> code: DocInverter / DocInverterPerThread / DocInverterPerField
			                --> InvertedDocConsumer / InvertedDocConsumerPerThread / InvertedDocConsumerPerField
			                  --> code: TermsHash / TermsHashPerThread / TermsHashPerField
			                    --> TermsHashConsumer / TermsHashConsumerPerThread / TermsHashConsumerPerField
			                      --> code: FreqProxTermsWriter / FreqProxTermsWriterPerThread / FreqProxTermsWriterPerField
			                      --> code: TermVectorsTermsWriter / TermVectorsTermsWriterPerThread / TermVectorsTermsWriterPerField
			                --> InvertedDocEndConsumer / InvertedDocConsumerPerThread / InvertedDocConsumerPerField
			                  --> code: NormsWriter / NormsWriterPerThread / NormsWriterPerField
			              --> code: StoredFieldsWriter / StoredFieldsWriterPerThread / StoredFieldsWriterPerField
			    */

			    // Build up indexing chain:
				
				TermVectorsConsumer termVectorsWriter = new TermVectorsConsumer(writer);
				FreqProxConsumer freqProxWriter = new FreqProxConsumer(writer);
				
				TermsHash termsHash = new TermsHash(writer, freqProxWriter, true, 
						new TermsHash(writer, termVectorsWriter, false, null));
				
				NormsConsumer normsWriter = new NormsConsumer(writer);
				DocumentInverter inverter = new DocumentInverter(writer, termsHash, normsWriter);
				
				return new DocumentProcessor(writer, inverter);
			}
		};
	
	public static final IndexingChain getIndexingChain() { 
		return sDefaultChain;
	}
	
}
