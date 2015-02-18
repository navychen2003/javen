package org.javenstudio.common.indexdb;

import java.io.IOException;

/**
 * This exception is thrown when the <code>write.lock</code>
 * could not be released.
 * @see Lock#release()
 */
public class LockReleaseFailedException extends IOException {
	private static final long serialVersionUID = 1L;

	public LockReleaseFailedException(String message) {
		super(message);
	}
	
}
