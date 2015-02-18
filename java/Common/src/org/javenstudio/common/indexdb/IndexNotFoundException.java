package org.javenstudio.common.indexdb;

import java.io.FileNotFoundException;

/**
 * Signals that no index was found in the Directory. Possibly because the
 * directory is empty, however can also indicate an index corruption.
 */
public final class IndexNotFoundException extends FileNotFoundException {
	private static final long serialVersionUID = 1L;

	public IndexNotFoundException(String msg) {
		super(msg);
	}

}
