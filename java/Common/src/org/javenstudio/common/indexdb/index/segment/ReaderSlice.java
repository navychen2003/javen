package org.javenstudio.common.indexdb.index.segment;

/**
 * Subreader slice from a parent composite reader.
 *
 */
public final class ReaderSlice {
	
	//public static final ReaderSlice[] EMPTY_ARRAY = new ReaderSlice[0];
	
	private final int mStart;
	private final int mLength;
	private final int mReaderIndex;

	public ReaderSlice(int start, int length, int readerIndex) {
		mStart = start;
		mLength = length;
		mReaderIndex = readerIndex;
	}

	public final int getStart() { return mStart; }
	public final int getLength() { return mLength; }
	public final int getReaderIndex() { return mReaderIndex; }
	
	@Override
	public String toString() {
		return "ReaderSlice: start=" + mStart + " length=" + mLength 
				+ " readerIndex=" + mReaderIndex;
	}
	
}