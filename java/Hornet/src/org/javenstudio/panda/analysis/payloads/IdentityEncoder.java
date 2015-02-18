package org.javenstudio.panda.analysis.payloads;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import org.javenstudio.common.indexdb.util.BytesRef;

/**
 *  Does nothing other than convert the char array to a byte array 
 *  using the specified encoding.
 */
public class IdentityEncoder extends AbstractEncoder implements PayloadEncoder{
	
	protected Charset mCharset = Charset.forName("UTF-8");
  
	public IdentityEncoder() {}

	public IdentityEncoder(Charset charset) {
		mCharset = charset;
	}

	@Override
	public BytesRef encode(char[] buffer, int offset, int length) {
		final ByteBuffer bb = mCharset.encode(CharBuffer.wrap(buffer, offset, length));
		if (bb.hasArray()) {
			return new BytesRef(bb.array(), bb.arrayOffset() + bb.position(), bb.remaining());
			
		} else {
			// normally it should always have an array, but who knows?
			final byte[] b = new byte[bb.remaining()];
			bb.get(b);
			
			return new BytesRef(b);
		}
	}
	
}
