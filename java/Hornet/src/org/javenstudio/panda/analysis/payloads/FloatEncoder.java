package org.javenstudio.panda.analysis.payloads;

import org.javenstudio.common.indexdb.util.BytesRef;

/**
 *  Encode a character array Float as a {@link BytesRef}.
 * <p/>
 * @see PayloadHelper#encodeFloat(float, byte[], int)
 */
public class FloatEncoder extends AbstractEncoder implements PayloadEncoder {

	@Override
	public BytesRef encode(char[] buffer, int offset, int length) {
		//TODO: improve this so that we don't have to new Strings
		float payload = Float.parseFloat(new String(buffer, offset, length));
		byte[] bytes = PayloadHelper.encodeFloat(payload);
		BytesRef result = new BytesRef(bytes);
		return result;
	}
	
}
