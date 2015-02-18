package org.javenstudio.hornet.query;

/**
 * Custom Exception to be thrown when the DocTermsIndex for a field cannot be generated
 */
public class DocTermsIndexException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public DocTermsIndexException(final String fieldName, final RuntimeException cause) {
    	super("Can't initialize DocTermsIndex to generate (function) FunctionValues for field: " 
    			+ fieldName, cause);
	}
	
}
