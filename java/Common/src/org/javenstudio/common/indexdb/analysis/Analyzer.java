package org.javenstudio.common.indexdb.analysis;

import java.io.IOException;
import java.io.Reader;

import org.javenstudio.common.indexdb.IAnalyzer;
import org.javenstudio.common.indexdb.ITokenStream;

/**
 * An Analyzer builds TokenStreams, which analyze text.  It thus represents a
 * policy for extracting index terms from text.
 * <p>
 * In order to define what analysis is done, subclasses must define their
 * {@link TokenComponents} in {@link #createComponents(String, Reader)}.
 * The components are then reused in each call to {@link #tokenStream(String, Reader)}.
 */
public abstract class Analyzer implements IAnalyzer {

	private final ReuseStrategy mReuseStrategy;

	public Analyzer() {
		this(new GlobalReuseStrategy());
	}

	public Analyzer(ReuseStrategy reuseStrategy) {
		mReuseStrategy = reuseStrategy;
	}

	/**
	 * Creates a new {@link TokenComponents} instance for this analyzer.
	 * 
	 * @param fieldName
	 *          the name of the fields content passed to the
	 *          {@link TokenComponents} sink as a reader
	 * @param reader
	 *          the reader passed to the {@link Tokenizer} constructor
	 * @return the {@link TokenComponents} for this analyzer.
	 */
	public abstract TokenComponents createComponents(String fieldName, 
			Reader reader) throws IOException;

	/**
	 * Creates a TokenStream that is allowed to be re-use from the previous time
	 * that the same thread called this method.  Callers that do not need to use
	 * more than one TokenStream at the same time from this analyzer should use
	 * this method for better performance.
	 * <p>
	 * This method uses {@link #createComponents(String, Reader)} to obtain an
	 * instance of {@link TokenComponents}. It returns the sink of the
	 * components and stores the components internally. Subsequent calls to this
	 * method will reuse the previously stored components after resetting them
	 * through {@link TokenComponents#reset(Reader)}.
	 * </p>
	 * 
	 * @param fieldName the name of the field the created TokenStream is used for
	 * @param reader the reader the streams source reads from
	 */
	@Override
	public final ITokenStream tokenStream(final String fieldName, 
			final Reader reader) throws IOException {
		TokenComponents components = mReuseStrategy.getReusableComponents(fieldName);
		final Reader r = initReader(fieldName, reader);
		if (components == null) {
			components = createComponents(fieldName, r);
			mReuseStrategy.setReusableComponents(fieldName, components);
		} else {
			components.reset(r);
		}
		return components.getTokenStream();
	}
  
	/**
	 * Override this if you want to add a CharFilter chain.
	 */
	public Reader initReader(String fieldName, Reader reader) 
			throws IOException {
		return reader;
	}

	/**
	 * Invoked before indexing a IField instance if
	 * terms have already been added to that field.  This allows custom
	 * analyzers to place an automatic position increment gap between
	 * IndexbleField instances using the same field name.  The default value
	 * position increment gap is 0.  With a 0 position increment gap and
	 * the typical default token position increment of 1, all terms in a field,
	 * including across IField instances, are in successive positions, allowing
	 * exact PhraseQuery matches, for instance, across IField instance boundaries.
	 *
	 * @param fieldName IField name being indexed.
	 * @return position increment gap, added to the next token emitted 
	 * from {@link #tokenStream(String,Reader)}
	 */
	@Override
	public int getPositionIncrementGap(String fieldName) throws IOException {
		return 0;
	}

	/**
	 * Just like {@link #getPositionIncrementGap}, except for
	 * Token offsets instead.  By default this returns 1.
	 * This method is only called if the field
	 * produced at least one token for indexing.
	 *
	 * @param fieldName the field just indexed
	 * @return offset gap, added to the next token emitted from 
	 * {@link #tokenStream(String,Reader)}.
	 *         This value must be {@code >= 0}.
	 */
	@Override
	public int getOffsetGap(String fieldName) throws IOException {
		return 1;
	}

	/** Frees persistent resources used by this Analyzer */
	public void close() throws IOException {
		mReuseStrategy.close();
	}

}
