package org.javenstudio.common.indexdb;

import java.io.Closeable;
import java.io.IOException;

public interface ITokenStream extends Closeable {

	/**
	 * Consumers (i.e., {@link IndexWriter}) use this method to advance the stream to
	 * the next token. Implementing classes must implement this method and update
	 * the appropriate {@link AttributeImpl}s with the attributes of the next
	 * token.
	 * <P>
	 * The producer must make no assumptions about the attributes after the method
	 * has been returned: the caller may arbitrarily change it. If the producer
	 * needs to preserve the state for subsequent calls, it can use
	 * {@link #captureState} to create a copy of the current attribute state.
	 * <p>
	 * This method is called for every token of a document, so an efficient
	 * implementation is crucial for good performance. To avoid calls to
	 * {@link #addAttribute(Class)} and {@link #getAttribute(Class)},
	 * references to all {@link AttributeImpl}s that this stream uses should be
	 * retrieved during instantiation.
	 * <p>
	 * To ensure that filters and consumers know which attributes are available,
	 * the attributes must be added during instantiation. Filters and consumers
	 * are not required to check for availability of attributes in
	 * {@link #incrementToken()}.
	 * 
	 * @return Next token of stream; null for end of stream
	 */
	public IToken nextToken() throws IOException;
	
	/**
	 * This method is called by the consumer after the last token has been
	 * consumed, after {@link #incrementToken()} returned <code>false</code>
	 * (using the new <code>TokenStream</code> API). Streams implementing the old API
	 * should upgrade to use this feature.
	 * <p/>
	 * This method can be used to perform any end-of-stream operations, such as
	 * setting the final offset of a stream. The final offset of a stream might
	 * differ from the offset of the last token eg in case one or more whitespaces
	 * followed after the last token, but a WhitespaceTokenizer was used.
	 * 
	 * @return the final token end offset
	 * @throws IOException
	 */
	public int end() throws IOException;
	
	/**
	 * This method is called by a consumer before it begins consumption using
	 * {@link #incrementToken()}.
	 * <p/>
	 * Resets this stream to the beginning.  As all TokenStreams must be reusable,
	 * any implementations which have state that needs to be reset between usages
	 * of the TokenStream, must implement this method. Note that if your TokenStream
	 * caches tokens and feeds them back again after a reset, it is imperative
	 * that you clone the tokens when you store them away (on the first pass) as
	 * well as when you return them (on future passes after {@link #reset()}).
	 */
	public void reset() throws IOException;
	
	/** Releases resources associated with this stream. */
	public void close() throws IOException;
	
}
