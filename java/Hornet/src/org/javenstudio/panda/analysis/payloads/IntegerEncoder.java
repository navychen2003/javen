package org.javenstudio.panda.analysis.payloads;

import org.javenstudio.common.indexdb.util.ArrayUtil;
import org.javenstudio.common.indexdb.util.BytesRef;

/**
 *  Encode a character array Integer as a {@link BytesRef}.
 * <p/>
 * See {@link PayloadHelper#encodeInt(int, byte[], int)}.
 */
public class IntegerEncoder extends AbstractEncoder implements PayloadEncoder {

	@Override
	public BytesRef encode(char[] buffer, int offset, int length) {
		//TODO: improve this so that we don't have to new Strings
		int payload = ArrayUtil.parseInt(buffer, offset, length);
		byte[] bytes = PayloadHelper.encodeInt(payload);
		BytesRef result = new BytesRef(bytes);
		return result;
	}
	
}