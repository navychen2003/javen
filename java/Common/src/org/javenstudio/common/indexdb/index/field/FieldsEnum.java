package org.javenstudio.common.indexdb.index.field;

import java.io.IOException;

import org.javenstudio.common.indexdb.IFieldsEnum;
import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.common.indexdb.index.term.Terms;

/** 
 * Enumerates indexed fields.  You must first call {@link
 *  #next} before calling {@link #terms}.
 *
 */
public abstract class FieldsEnum implements IFieldsEnum {

	/** 
	 * Increments the enumeration to the next field. Returns
	 * null when there are no more fields.
	 */
	public abstract String next();

	// TODO: would be nice to require/fix all impls so they
	// never return null here... we have to fix the writers to
	// never write 0-terms fields... or maybe allow a non-null
	// Terms instance in just this case

	/** 
	 * Get {@link Terms} for the current field.  After {@link #next} returns
	 *  null this method should not be called. This method may
	 *  return null in some cases, which means the provided
	 *  field does not have any terms. 
	 */
	public abstract ITerms getTerms() throws IOException;
	
	// TODO: should we allow pulling Terms as well?  not just
	// the iterator?
  
	//public final static FieldsEnum[] EMPTY_ARRAY = new FieldsEnum[0];

	/** Provides zero fields */
	public final static FieldsEnum EMPTY = new FieldsEnum() {
			@Override
			public String next() {
				return null;
			}

			@Override
			public ITerms getTerms() {
				throw new IllegalStateException("this method should never be called");
			}
		};
	
}
