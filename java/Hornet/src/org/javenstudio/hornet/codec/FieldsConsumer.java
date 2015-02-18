package org.javenstudio.hornet.codec;

import java.io.IOException;

import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IFields;
import org.javenstudio.common.indexdb.IFieldsEnum;
import org.javenstudio.common.indexdb.IMergeState;
import org.javenstudio.common.indexdb.ITermState;
import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.common.indexdb.codec.IFieldsConsumer;
import org.javenstudio.common.indexdb.codec.ITermsConsumer;
import org.javenstudio.common.indexdb.index.term.MergeTermState;
import org.javenstudio.common.util.Logger;

/** 
 * Abstract API that consumes terms, doc, freq, prox, offset and
 * payloads postings.  Concrete implementations of this
 * actually do "something" with the postings (write it into
 * the index in a specific format).
 * <p>
 * The lifecycle is:
 * <ol>
 *   <li>FieldsConsumer is created by 
 *       {@link PostingsFormat#fieldsConsumer(SegmentWriteState)}.
 *   <li>For each field, {@link #addField(FieldInfo)} is called,
 *       returning a {@link TermsConsumer} for the field.
 *   <li>After all fields are added, the consumer is {@link #close}d.
 * </ol>
 *
 */
public abstract class FieldsConsumer implements IFieldsConsumer {
	private static final Logger LOG = Logger.getLogger(FieldsConsumer.class);

	/** Add a new field */
	public abstract ITermsConsumer addField(IFieldInfo field) throws IOException;
  
	/** Called when we are done adding everything. */
	public abstract void close() throws IOException;

	@Override
	public ITermState merge(IMergeState mergeState, IFields fields) throws IOException { 
	    IFieldsEnum fieldsEnum = fields.iterator();
	    assert fieldsEnum != null;
	    
	    MergeTermState termState = new MergeTermState();
	    String field;
	    
	    while ((field = fieldsEnum.next()) != null) {
	    	mergeState.setFieldInfo(mergeState.getFieldInfos().getFieldInfo(field));
	    	assert mergeState.getFieldInfo() != null : "FieldInfo for field is null: "+ field;
	    	
	    	ITerms terms = fieldsEnum.getTerms();
	    	
	    	if (LOG.isDebugEnabled()) {
	    		LOG.debug("merge: field=" + field + " fieldInfo=" 
	    				+ mergeState.getFieldInfo() + " terms=" + terms);
	    	}
	    	
	    	if (terms != null) {
	    		final ITermsConsumer termsConsumer = addField(mergeState.getFieldInfo());
	    		ITermState state = termsConsumer.merge(mergeState, terms.iterator(null));
	    		termState.mergeFrom(state);
	    	}
	    }
	    
	    return termState;
	}

}
