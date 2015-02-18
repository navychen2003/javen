package org.javenstudio.common.indexdb.store;

import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import org.javenstudio.common.indexdb.IIndexInput;

/** 
 * Writes bytes through to a primary IndexOutput, computing
 *  checksum as it goes. Note that you cannot use seek().
 *
 */
public class ChecksumIndexInput extends IndexInput {
	
	private final IIndexInput mMain;
	private final Checksum mDigest;

	public ChecksumIndexInput(IIndexInput main) {
		super(main.getContext());
		mMain = main;
		mDigest = new CRC32();
	}

	@Override
	public byte readByte() throws IOException {
		final byte b = mMain.readByte();
		mDigest.update(b);
		return b;
	}

	@Override
	public void readBytes(byte[] b, int offset, int len) throws IOException {
		mMain.readBytes(b, offset, len);
		mDigest.update(b, offset, len);
	}
  
	public long getChecksum() {
		return mDigest.getValue();
	}

	@Override
	public void close() throws IOException {
		mMain.close();
	}

	@Override
	public long getFilePointer() {
		return mMain.getFilePointer();
	}

	@Override
	public void seek(long pos) {
		throw new UnsupportedOperationException();
	}

	@Override
	public long length() {
		return mMain.length();
	}
	
}
