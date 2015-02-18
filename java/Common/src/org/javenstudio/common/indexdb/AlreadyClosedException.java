package org.javenstudio.common.indexdb;

/**
 * This exception is thrown when there is an attempt to
 * access something that has already been closed.
 */
public class AlreadyClosedException extends IllegalStateException {
	private static final long serialVersionUID = 1L;

	public AlreadyClosedException(String message) {
		super(message);
	}
  
}
