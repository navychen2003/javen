package org.javenstudio.panda.analysis.payloads;

import org.javenstudio.common.indexdb.util.BytesRef;

/**
 * Base class for payload encoders.
 *
 */
public abstract class AbstractEncoder implements PayloadEncoder {
	
	@Override
	public BytesRef encode(char[] buffer) {
		return encode(buffer, 0, buffer.length);
	}
	
}
