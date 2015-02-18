package org.javenstudio.panda.analysis;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

import org.javenstudio.common.indexdb.analysis.Analyzer;
import org.javenstudio.common.indexdb.util.IOUtils;
import org.javenstudio.panda.util.CharArraySet;
import org.javenstudio.panda.util.WordlistLoader;

/**
 * Base class for Analyzers that need to make use of stopword sets. 
 * 
 */
public abstract class StopwordAnalyzerBase extends Analyzer {

	/**
	 * An immutable stopword set
	 */
	protected final CharArraySet mStopwords;

	/**
	 * Returns the analyzer's stopword set or an empty set if the analyzer has no
	 * stopwords
	 * 
	 * @return the analyzer's stopword set or an empty set if the analyzer has no
	 *         stopwords
	 */
	public CharArraySet getStopwordSet() {
		return mStopwords;
	}

	/**
	 * Creates a new instance initialized with the given stopword set
	 * 
	 * @param version
	 *          the Lucene version for cross version compatibility
	 * @param stopwords
	 *          the analyzer's stopword set
	 */
	protected StopwordAnalyzerBase(final CharArraySet stopwords) {
		// analyzers should use char array set for stopwords!
		mStopwords = stopwords == null ? CharArraySet.EMPTY_SET : CharArraySet
				.unmodifiableSet(CharArraySet.copy(stopwords));
	}

	/**
	 * Creates a new Analyzer with an empty stopword set
	 * 
	 * @param version
	 *          the Lucene version for cross version compatibility
	 */
	protected StopwordAnalyzerBase() {
		this(null);
	}

	/**
	 * Creates a CharArraySet from a file resource associated with a class. (See
	 * {@link Class#getResourceAsStream(String)}).
	 * 
	 * @param ignoreCase
	 *          <code>true</code> if the set should ignore the case of the
	 *          stopwords, otherwise <code>false</code>
	 * @param aClass
	 *          a class that is associated with the given stopwordResource
	 * @param resource
	 *          name of the resource file associated with the given class
	 * @param comment
	 *          comment string to ignore in the stopword file
	 * @return a CharArraySet containing the distinct stopwords from the given
	 *         file
	 * @throws IOException
	 *           if loading the stopwords throws an {@link IOException}
	 */
	protected static CharArraySet loadStopwordSet(final boolean ignoreCase,
			final Class<? extends Analyzer> aClass, final String resource,
			final String comment) throws IOException {
		Reader reader = null;
		try {
			reader = IOUtils.getDecodingReader(aClass.getResourceAsStream(resource), IOUtils.CHARSET_UTF_8);
			return WordlistLoader.getWordSet(reader, comment, new CharArraySet(16, ignoreCase));
		} finally {
			IOUtils.close(reader);
		}
	}
  
	/**
	 * Creates a CharArraySet from a file.
	 * 
	 * @param stopwords
	 *          the stopwords file to load
	 * 
	 * @param matchVersion
	 *          the Lucene version for cross version compatibility
	 * @return a CharArraySet containing the distinct stopwords from the given
	 *         file
	 * @throws IOException
	 *           if loading the stopwords throws an {@link IOException}
	 */
	protected static CharArraySet loadStopwordSet(File stopwords) throws IOException {
		Reader reader = null;
		try {
			reader = IOUtils.getDecodingReader(stopwords, IOUtils.CHARSET_UTF_8);
			return WordlistLoader.getWordSet(reader);
		} finally {
			IOUtils.close(reader);
		}
	}
  
	/**
	 * Creates a CharArraySet from a file.
	 * 
	 * @param stopwords
	 *          the stopwords reader to load
	 * 
	 * @param matchVersion
	 *          the Lucene version for cross version compatibility
	 * @return a CharArraySet containing the distinct stopwords from the given
	 *         reader
	 * @throws IOException
	 *           if loading the stopwords throws an {@link IOException}
	 */
	protected static CharArraySet loadStopwordSet(Reader stopwords) throws IOException {
		try {
			return WordlistLoader.getWordSet(stopwords);
		} finally {
			IOUtils.close(stopwords);
		}
	}
	
}
