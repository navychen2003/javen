package org.javenstudio.common.indexdb.store;

import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import org.javenstudio.common.indexdb.IIndexOutput;

/** 
 * Writes bytes through to a primary IndexOutput, computing
 *  checksum.  Note that you cannot use seek().
 *
 */
public class ChecksumIndexOutput extends IndexOutput {
	
	private final IIndexOutput mMain;
	private final Checksum mDigest;

	public ChecksumIndexOutput(IIndexOutput main) {
		super(main.getContext());
		mMain = main;
		mDigest = new CRC32();
	}

	@Override
	public void writeByte(byte b) throws IOException {
		mDigest.update(b);
		mMain.writeByte(b);
	}

	@Override
	public void writeBytes(byte[] b, int offset, int length) throws IOException {
		mDigest.update(b, offset, length);
		mMain.writeBytes(b, offset, length);
	}

	public long getChecksum() {
		return mDigest.getValue();
	}

	@Override
	public void flush() throws IOException {
		mMain.flush();
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

	/** writes the checksum */
	public void finishCommit() throws IOException {
		mMain.writeLong(getChecksum());
	}

	@Override
	public long length() throws IOException {
		return mMain.length();
	}
	
}
