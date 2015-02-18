package org.javenstudio.common.indexdb;

import java.io.IOException;
import java.io.Reader;

import org.javenstudio.common.indexdb.util.BytesRef;

public interface IField {

	public static interface Type { 
		/** True if this field should be indexed (inverted) */
		public boolean isIndexed();

		/** True if the field's value should be stored */
		public boolean isStored();

		/** True if this field's value should be analyzed */
		public boolean isTokenized();

		/** True if term vectors should be indexed */
		public boolean isStoreTermVectors();

		/** True if term vector offsets should be indexed */
		public boolean isStoreTermVectorOffsets();

		/** True if term vector positions should be indexed */
		public boolean isStoreTermVectorPositions();

		/** True if norms should not be indexed */
		public boolean isOmitNorms();
		
		/** {@link IndexOptions}, describing what should be
		 * recorded into the inverted index */
		public IndexOptions getIndexOptions();
	}
	
	/** Field name */
	public String getName();

	/** {@link IndexableFieldType} describing the properties
	 * of this field. */
	public IField.Type getFieldType();
	  
	/** Field boost (you must pre-multiply in any doc boost). */
	public float getBoost();

	/** Non-null if this field has a binary value */
	public BytesRef getBinaryValue();

	/** Non-null if this field has a string value */
	public String getStringValue();

	/** Non-null if this field has a Reader value */
	public Reader getReaderValue();

	/** Non-null if this field has a numeric value */
	public Number getNumericValue();

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
