package org.javenstudio.common.indexdb;

/** 
 * Exception thrown if there are any problems while
 *  executing a merge. 
 */
public class MergeException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	private final IDirectory mDir;

	public MergeException(String message, IDirectory dir) {
		super(message);
		mDir = dir;
	}

	public MergeException(Throwable exc, IDirectory dir) {
		super(exc);
		mDir = dir;
	}
  
	/** 
	 * Returns the {@link Directory} of the index that hit
	 *  the exception. 
	 */
	public IDirectory getDirectory() {
		return mDir;
	}
  
}
