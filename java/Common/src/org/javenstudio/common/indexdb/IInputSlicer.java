package org.javenstudio.common.indexdb;

import java.io.Closeable;
import java.io.IOException;

/**
 * Allows to create one or more sliced {@link IndexInput} instances from a single 
 * file handle. Some {@link Directory} implementations may be able to efficiently map slices of a file
 * into memory when only certain parts of a file are required.   
 * 
 */
public interface IInputSlicer extends Closeable {

	/**
	 * Returns an {@link IndexInput} slice starting at the given offset with the given length.
	 */
	public IIndexInput openSlice(long offset, long length) throws IOException;

	/**
	 * Returns an {@link IndexInput} slice starting at offset <i>0</i> with a
	 * length equal to the length of the underlying file
	 */
	public IIndexInput openFullSlice() throws IOException;
	
}
