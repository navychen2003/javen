package org.javenstudio.panda.analysis;

import java.io.IOException;
import java.io.Reader;

/**
 * Subclasses of CharFilter can be chained to filter a Reader
 * They can be used as {@link java.io.Reader} with additional offset
 * correction. {@link Tokenizer}s will automatically use {@link #correctOffset}
 * if a CharFilter subclass is used.
 * <p>
 * This class is abstract: at a minimum you must implement {@link #read(char[], int, int)},
 * transforming the input in some way from {@link #input}, and {@link #correct(int)}
 * to adjust the offsets to match the originals.
 * <p>
 * You can optionally provide more efficient implementations of additional methods 
 * like {@link #read()}, {@link #read(char[])}, {@link #read(java.nio.CharBuffer)},
 * but this is not required.
 * <p>
 * For examples and integration with {@link Analyzer}, see the 
 * {@link org.apache.lucene.analysis Analysis package documentation}.
 */
// the way java.io.FilterReader should work!
public abstract class CharFilter extends Reader {
	
	/** 
	 * The underlying character-input stream. 
	 */
	protected final Reader mInput;

	protected final Reader getInput() { 
		return mInput;
	}
	
	/**
	 * Create a new CharFilter wrapping the provided reader.
	 * @param input a Reader, can also be a CharFilter for chaining.
	 */
	public CharFilter(Reader input) {
		super(input);
		mInput = input;
	}
  
	/** 
	 * Closes the underlying input stream.
	 * <p>
	 * <b>NOTE:</b> 
	 * The default implementation closes the input Reader, so
	 * be sure to call <code>super.close()</code> when overriding this method.
	 */
	@Override
	public void close() throws IOException {
		mInput.close();
	}

	/**
	 * Subclasses override to correct the current offset.
	 *
	 * @param currentOff current offset
	 * @return corrected offset
	 */
	protected abstract int correct(int currentOff);
  
	/**
	 * Chains the corrected offset through the input
	 * CharFilter(s).
	 */
	public final int correctOffset(int currentOff) {
		final int corrected = correct(currentOff);
		return (mInput instanceof CharFilter) ? ((CharFilter) mInput).correctOffset(corrected) : corrected;
	}
	
}
