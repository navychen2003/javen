package org.javenstudio.common.indexdb;

import java.io.IOException;
import java.io.Reader;

public interface IAnalyzer {

	/** 
	 * Creates a TokenStream which tokenizes all the text in the provided
	 * Reader.  Must be able to handle null field name for
	 * backward compatibility.
	 */
	public ITokenStream tokenStream(String fieldName, Reader reader) 
			throws IOException;

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
	 * @return position increment gap, added to the next token emitted from 
	 *   {@link #tokenStream(String,Reader)}
	 */
	public int getPositionIncrementGap(String fieldName) throws IOException;
	
	/**
	 * Just like {@link #getPositionIncrementGap}, except for
	 * Token offsets instead.  By default this returns 1.
	 * This method is only called if the field
	 * produced at least one token for indexing.
	 *
	 * @param fieldName the field just indexed
	 * @return offset gap, added to the next token emitted from {@link #tokenStream(String,Reader)}.
	 *         This value must be {@code >= 0}.
	 */
	public int getOffsetGap(String fieldName) throws IOException;
	
}
