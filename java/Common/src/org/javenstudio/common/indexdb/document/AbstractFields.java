package org.javenstudio.common.indexdb.document;

import java.io.IOException;

import org.javenstudio.common.indexdb.IFields;
import org.javenstudio.common.indexdb.IFieldsEnum;
import org.javenstudio.common.indexdb.ITerms;

public abstract class AbstractFields implements IFields {

	/** 
	 * Returns the number of terms for all fields, or -1 if this 
	 *  measure isn't stored by the codec. Note that, just like 
	 *  other term measures, this measure does not take deleted 
	 *  documents into account. 
	 */
	// TODO: deprecate?
	@Override
	public long getUniqueTermCount() throws IOException {
		long numTerms = 0;
		IFieldsEnum it = iterator();
		while (true) {
			String field = it.next();
			if (field == null) 
				break;
			
			ITerms terms = getTerms(field);
			if (terms != null) {
				final long termCount = terms.size();
				if (termCount == -1) 
					return -1;
          
				numTerms += termCount;
			}
		}
		return numTerms;
	}
	
}
