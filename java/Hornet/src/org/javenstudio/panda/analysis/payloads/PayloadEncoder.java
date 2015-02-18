package org.javenstudio.panda.analysis.payloads;

import org.javenstudio.common.indexdb.util.BytesRef;

/**
 * Mainly for use with the DelimitedPayloadTokenFilter, converts char buffers to
 * {@link BytesRef}.
 * <p/>
 * NOTE: This interface is subject to change 
 */
public interface PayloadEncoder {

	public BytesRef encode(char[] buffer);

	/**
	 * Convert a char array to a {@link BytesRef}
	 * @return encoded {@link BytesRef}
	 */
	public BytesRef encode(char[] buffer, int offset, int length);
	
}
