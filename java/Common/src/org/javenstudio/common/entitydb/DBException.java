package org.javenstudio.common.entitydb;

/**
 * An exception that indicates there was an error with Content parsing or execution.
 */
public class DBException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	public DBException() {}

    public DBException(String message) {
        super(message);
    }
    
    public DBException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
