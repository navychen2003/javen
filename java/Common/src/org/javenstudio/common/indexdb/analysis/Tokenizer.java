package org.javenstudio.common.indexdb.analysis;

import java.io.Reader;
import java.io.IOException;


/** 
 * A Tokenizer is a TokenStream whose input is a Reader.
 * <p>
 * This is an abstract class; subclasses must override {@link #incrementToken()}
 * <p>
 * NOTE: Subclasses overriding {@link #incrementToken()} must
 * call {@link AttributeSource#clearAttributes()} before
 * setting attributes.
 */
public abstract class Tokenizer extends TokenStream {
	
	/** The text source for this Tokenizer. */
	private Reader mInput;

	/** Construct a token stream processing the given input. */
	protected Tokenizer(Reader input) {
		mInput = input;
	}
  
	protected final Reader getInput() { 
		return mInput;
	}
	
	/** By default, closes the input Reader. */
	@Override
	public void close() throws IOException {
		if (mInput != null) {
			mInput.close();
			// LUCENE-2387: don't hold onto Reader after close, so
			// GC can reclaim
			mInput = null;
		}
	}
  
	/** 
	 * Return the corrected offset. If {@link #input} is a {@link CharStream} subclass
	 * this method calls {@link CharStream#correctOffset}, else returns <code>currentOff</code>.
	 * @param currentOff offset as seen in the output
	 * @return corrected offset based on the input
	 * @see CharStream#correctOffset
	 */
	protected final int correctOffset(int currentOff) {
		return (mInput instanceof CharReader) ? 
				((CharReader)mInput).correctOffset(currentOff) : currentOff;
	}

	/** 
	 * Expert: Reset the tokenizer to a new reader.  Typically, an
	 *  analyzer (in its reusableTokenStream method) will use
	 *  this to re-use a previously created tokenizer. 
	 */
	public void reset(Reader input) throws IOException {
		Reader r = mInput;
		mInput = input;
		
		if (r != null && r != input)
			r.close();
	}
	
}

