package org.javenstudio.panda.query;

import java.io.IOException;
import java.io.Reader;

/** 
 * An efficient implementation of JavaCC's CharStream interface.  <p>Note that
 * this does not do line-number counting, but instead keeps track of the
 * character position of the token in the input, as required by Lucene's {@link
 * org.apache.lucene.analysis.Token} API. 
 */
public final class FastCharStream implements CharStream {
	
	private char[] mBuffer = null;

	private int mBufferLength = 0;  	// end of valid chars
	private int mBufferPosition = 0;  	// next char to read

	private int mTokenStart = 0;    	// offset in buffer
	private int mBufferStart = 0;   	// position in file of buffer

	private Reader mInput;            	// source of chars

	/** Constructs from a Reader. */
	public FastCharStream(Reader r) {
		mInput = r;
	}

	@Override
	public final char readChar() throws IOException {
		if (mBufferPosition >= mBufferLength)
			refill();
		
		return mBuffer[mBufferPosition++];
	}

	private final void refill() throws IOException {
		int newPosition = mBufferLength - mTokenStart;

		if (mTokenStart == 0) {   		// token won't fit in buffer
			if (mBuffer == null) {		// first time: alloc buffer
				mBuffer = new char[2048];
				
			} else if (mBufferLength == mBuffer.length) { // grow buffer
				char[] newBuffer = new char[mBuffer.length*2];
				System.arraycopy(mBuffer, 0, newBuffer, 0, mBufferLength);
				mBuffer = newBuffer;
			}
		} else {	// shift token to front
			System.arraycopy(mBuffer, mTokenStart, mBuffer, 0, newPosition);
		}

		mBufferLength = newPosition;  	// update state
		mBufferPosition = newPosition;
		mBufferStart += mTokenStart;
		mTokenStart = 0;

		int charsRead = 	// fill space in buffer
				mInput.read(mBuffer, newPosition, mBuffer.length-newPosition);
		
		if (charsRead == -1)
			throw new IOException("read past eof");
		else
			mBufferLength += charsRead;
	}

	@Override
	public final char beginToken() throws IOException {
		mTokenStart = mBufferPosition;
		return readChar();
	}

	@Override
	public final void backup(int amount) {
		mBufferPosition -= amount;
	}

	@Override
	public final String getImage() {
		return new String(mBuffer, mTokenStart, mBufferPosition - mTokenStart);
	}

	@Override
	public final char[] getSuffix(int len) {
		char[] value = new char[len];
		System.arraycopy(mBuffer, mBufferPosition - len, value, 0, len);
		return value;
	}

	@Override
	public final void done() {
		try {
			mInput.close();
		} catch (IOException e) {
			// ignore
		}
	}

	@Override
	public final int getColumn() {
		return mBufferStart + mBufferPosition;
	}
	
	@Override
	public final int getLine() {
		return 1;
	}
	
	@Override
	public final int getEndColumn() {
		return mBufferStart + mBufferPosition;
	}
	
	@Override
	public final int getEndLine() {
		return 1;
	}
	
	@Override
	public final int getBeginColumn() {
		return mBufferStart + mTokenStart;
	}
	
	@Override
	public final int getBeginLine() {
		return 1;
	}
	
}
