package org.javenstudio.common.indexdb.store;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * A {@link DataInput} wrapping a plain {@link InputStream}.
 */
public class InputStreamDataInput extends DataInput implements Closeable {
	
	private final InputStream mInput;
  
	public InputStreamDataInput(InputStream is) {
		mInput = is;
	}

	@Override
	public byte readByte() throws IOException {
		int v = mInput.read();
		if (v == -1) throw new EOFException();
		return (byte) v;
	}
  
	@Override
	public void readBytes(byte[] b, int offset, int len) throws IOException {
		while (len > 0) {
			final int cnt = mInput.read(b, offset, len);
			if (cnt < 0) {
				// Partially read the input, but no more data available in the stream.
				throw new EOFException();
			}
			len -= cnt;
			offset += cnt;
		}
	}

	@Override
	public void close() throws IOException {
		mInput.close();
	}
	
}
