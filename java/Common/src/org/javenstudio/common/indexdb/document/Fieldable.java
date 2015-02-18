package org.javenstudio.common.indexdb.document;

import java.io.IOException;
import java.io.Reader;

import org.javenstudio.common.indexdb.IAnalyzer;
import org.javenstudio.common.indexdb.IField;
import org.javenstudio.common.indexdb.ITokenStream;

/**
 * Synonymous with {@link Field}.
 *
 * <p><bold>WARNING</bold>: This interface may change within minor versions, 
 * despite Indexdb's backward compatibility requirements.
 * This means new methods may be added from version to version.  This change 
 * only affects the Fieldable API; other backwards
 * compatibility promises remain intact. For example, Indexdb can still
 * read and write indices created within the same major version.
 * </p>
 *
 */
public interface Fieldable extends IField {

	/** 
	 * Returns the name of the field as an interned string.
	 * For example "date", "title", "body", ...
	 */
	public String getName();
	
	/** 
	 * Sets the boost factor hits on this field.  This value will be
	 * multiplied into the score of all hits on this this field of this
	 * document.
	 *
	 * <p>The boost is multiplied by {@link Document#getBoost()} 
	 * of the document containing this field.  If a document has multiple fields with the same
	 * name, all such values are multiplied together.  This product is then
	 * used to compute the norm factor for the field.  By
	 * default, in the {@link
	 * Similarity#computeNorm(String,
	 * FieldInvertState)} method, the boost value is multiplied
	 * by the {@link Similarity#lengthNorm(String,
	 * int)} and then rounded by {@link Similarity#encodeNormValue(float)} 
	 * before it is stored in the
	 * index.  One should attempt to ensure that this product does not overflow
	 * the range of that encoding.
	 *
	 * @see Document#setBoost(float)
	 * @see Similarity#computeNorm(String, FieldInvertState)
	 * @see Similarity#encodeNormValue(float)
	 */
	public void setBoost(float boost);
	
	/** 
	 * Returns the boost factor for hits for this field.
	 *
	 * <p>The default value is 1.0.
	 *
	 * <p>Note: this value is not stored directly with the document in the index.
	 * Documents returned from {@link IndexReader#document(int)} and
	 * {@link Searcher#doc(int)} may thus not have the same value 
	 * present as when this field was indexed.
	 *
	 * @see #setBoost(float)
	 */
	public float getBoost();
	
	/** 
	 * The value of the field as a String, or null.
	 * <p>
	 * For indexing, if isStored()==true, the stringValue() will be used as the stored field value
	 * unless isBinary()==true, in which case getBinaryValue() will be used.
	 *
	 * If isIndexed()==true and isTokenized()==false, this String value will be indexed as a single token.
	 * If isIndexed()==true and isTokenized()==true, then tokenStreamValue() will be used 
	 * to generate indexed tokens if not null,
	 * else readerValue() will be used to generate indexed tokens if not null, 
	 * else stringValue() will be used to generate tokens.
	 */
	public String getStringValue();
	
	/** 
	 * The value of the field as a Reader, which can be used at index time to generate indexed tokens.
	 * @see #stringValue()
	 */
	public Reader getReaderValue();
	
	/**
	 * Creates the TokenStream used for indexing this field.  If appropriate,
	 * implementations should use the given Analyzer to create the TokenStreams.
	 *
	 * @param analyzer Analyzer that should be used to create the TokenStreams from
	 * @return TokenStream value for indexing the document.  Should always return
	 *         a non-null value if the field is to be indexed
	 * @throws IOException Can be thrown while creating the TokenStream
	 */
	public ITokenStream tokenStream(IAnalyzer analyzer) throws IOException;
	
}
