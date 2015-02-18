package org.javenstudio.common.indexdb.store;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A {@link DataOutput} wrapping a plain {@link OutputStream}.
 */
public class OutputStreamDataOutput extends DataOutput implements Closeable {
	
	private final OutputStream mOutput;
  
	public OutputStreamDataOutput(OutputStream os) {
		mOutput = os;
	}
  
	@Override
	public void writeByte(byte b) throws IOException {
		mOutput.write(b);
	}
  
	@Override
	public void writeBytes(byte[] b, int offset, int length) throws IOException {
		mOutput.write(b, offset, length);
	}

	@Override
	public void close() throws IOException {
		mOutput.close();
	}
	
}
