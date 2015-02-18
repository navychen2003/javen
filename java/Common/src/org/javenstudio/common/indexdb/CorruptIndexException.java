package org.javenstudio.common.indexdb;

import java.io.IOException;

/**
 * This exception is thrown when Indexdb detects
 * an inconsistency in the index.
 */
public class CorruptIndexException extends IOException {
	private static final long serialVersionUID = 1L;

	public CorruptIndexException(String message) {
		super(message);
	}
	
}
