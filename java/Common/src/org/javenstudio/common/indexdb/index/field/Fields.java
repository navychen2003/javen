package org.javenstudio.common.indexdb.index.field;

import java.io.IOException;

import org.javenstudio.common.indexdb.IFields;
import org.javenstudio.common.indexdb.IFieldsEnum;
import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.common.indexdb.document.AbstractFields;
import org.javenstudio.common.indexdb.index.term.Terms;

/** 
 * Flex API for access to fields and terms
 */
public abstract class Fields extends AbstractFields implements IFields {

	//public final static Fields[] EMPTY_ARRAY = new Fields[0];
	
	/** 
	 * Returns an iterator that will step through all fields
	 *  names.  This will not return null.
	 */
	public abstract IFieldsEnum iterator();

	/** 
	 * Get the {@link Terms} for this field.  This will return
	 *  null if the field does not exist. 
	 */
	public abstract ITerms getTerms(String field) throws IOException;

	/** 
	 * Returns the number of terms for all fields, or -1 if this 
	 *  measure isn't stored by the codec. Note that, just like 
	 *  other term measures, this measure does not take deleted 
	 *  documents into account. 
	 */
	public abstract int size() throws IOException;
  
}
