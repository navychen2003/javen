package org.javenstudio.common.indexdb;

import java.io.FileNotFoundException;

/**
 * This exception is thrown when you try to list a
 * non-existent directory.
 */
public class NoSuchDirectoryException extends FileNotFoundException {
	private static final long serialVersionUID = 1L;

	public NoSuchDirectoryException(String message) {
		super(message);
	}
	
}
