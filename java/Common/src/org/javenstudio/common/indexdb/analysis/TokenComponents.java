package org.javenstudio.common.indexdb.analysis;

import java.io.IOException;
import java.io.Reader;

import org.javenstudio.common.indexdb.ITokenStream;

/**
 * This class encapsulates the outer components of a token stream. It provides
 * access to the source ({@link Tokenizer}) and the outer end (sink), an
 * instance of {@link TokenFilter} which also serves as the
 * {@link TokenStream} returned by
 * {@link Analyzer#tokenStream(String, Reader)} and
 * {@link Analyzer#reusableTokenStream(String, Reader)}.
 */
public class TokenComponents {

	protected final Tokenizer mSource;
	protected final ITokenStream mSink;

	/**
	 * Creates a new {@link TokenComponents} instance.
	 * 
	 * @param source
	 *          the analyzer's tokenizer
	 * @param result
	 *          the analyzer's resulting token stream
	 */
	public TokenComponents(final Tokenizer source, final ITokenStream result) {
		mSource = source;
		mSink = result;
	}

	/**
	 * Creates a new {@link TokenComponents} instance.
	 * 
	 * @param source
	 *          the analyzer's tokenizer
	 */
	public TokenComponents(final Tokenizer source) {
		mSource = source;
		mSink = source;
	}

	/**
	 * Resets the encapsulated components with the given reader. This method by
	 * default returns <code>true</code> indicating that the components have
	 * been reset successfully. Subclasses of {@link ReusableAnalyzerBase} might use
	 * their own {@link TokenComponents} returning <code>false</code> if
	 * the components cannot be reset.
	 * 
	 * @param reader
	 *          a reader to reset the source component
	 * @return <code>true</code> if the components were reset, otherwise
	 *         <code>false</code>
	 * @throws IOException
	 *           if the component's reset method throws an {@link IOException}
	 */
	protected boolean reset(final Reader reader) throws IOException {
		mSource.reset(reader);
		return true;
	}

	/**
	 * Returns the sink {@link TokenStream}
	 * 
	 * @return the sink {@link TokenStream}
	 */
	protected ITokenStream getTokenStream() {
		return mSink;
	}
	
}
