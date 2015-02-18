package org.javenstudio.common.indexdb.document;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.javenstudio.common.indexdb.IAnalyzer;
import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.common.indexdb.analysis.TokenStreamField;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.NumericType;

abstract class AbstractField extends TokenStreamField implements Fieldable {

	protected final FieldType mType;
	protected final String mName;
	protected float mBoost = 1.0f;

	// Field's value:
	protected Object mFieldsData;

	// Pre-analyzed tokenStream for indexed fields; this is
	// separate from fieldsData because you are allowed to
	// have both; eg maybe field has a String value but you
	// customize how it's tokenized:
	protected ITokenStream mTokenStream;
	
	protected AbstractField(String name, FieldType type) {
		this(name, type, (Object)null);
	}
	
	protected AbstractField(String name, FieldType type, Object data) {
		if (name == null) 
			throw new IllegalArgumentException("name cannot be null");
		
	    if (type == null) 
	    	throw new IllegalArgumentException("type cannot be null");
	    
	    mName = name;
	    mType = type;
	    mFieldsData = data;
	}

	/**
	 * Create field with Reader value.
	 */
	public AbstractField(String name, Reader reader, FieldType type) {
		this(name, type, reader);
		
	    if (reader == null) 
	    	throw new NullPointerException("reader cannot be null");
	    
	    if (type.isStored()) 
	    	throw new IllegalArgumentException("fields with a Reader value cannot be stored");
	    
	    if (type.isIndexed() && !type.isTokenized()) 
	    	throw new IllegalArgumentException("non-tokenized fields must use String values");
	}

	/**
	 * Create field with TokenStream value.
	 */
	public AbstractField(String name, ITokenStream tokenStream, FieldType type) {
		this(name, type, (Object)null);
	    
	    if (tokenStream == null) 
	    	throw new NullPointerException("tokenStream cannot be null");
	    
	    if (!type.isIndexed() || !type.isTokenized()) 
	    	throw new IllegalArgumentException("TokenStream fields must be indexed and tokenized");
	    
	    if (type.isStored()) 
	    	throw new IllegalArgumentException("TokenStream fields cannot be stored");
	    
	    mTokenStream = tokenStream;
	}
	  
	/**
	 * Create field with binary value.
	 */
	public AbstractField(String name, byte[] value, FieldType type) {
	    this(name, value, 0, value.length, type);
	}

	/**
	 * Create field with binary value.
	 */
	public AbstractField(String name, byte[] value, int offset, int length, FieldType type) {
	    this(name, new BytesRef(value, offset, length), type);
	}

	/**
	 * Create field with binary value.
	 *
	 * <p>NOTE: the provided BytesRef is not copied so be sure
	 * not to change it until you're done with this field.
	 */
	public AbstractField(String name, BytesRef bytes, FieldType type) {
	    this(name, type, bytes);
	    
	    if (bytes == null) 
	    	throw new NullPointerException("bytes cannot be null");
	    
	    if (type.isIndexed()) 
	    	throw new IllegalArgumentException("Fields with BytesRef values cannot be indexed");
	}

	// TODO: allow direct construction of int, long, float, double value too..?

	/**
	 * Create field with String value.
	 */
	public AbstractField(String name, String value, FieldType type) {
	    this(name, type, value); 
	    
	    if (value == null) 
	    	throw new NullPointerException("value cannot be null");
	    
	    if (!type.isStored() && !type.isIndexed()) {
	    	throw new IllegalArgumentException("it doesn't make sense to have a field that "
	    			+ "is neither indexed nor stored");
	    }
	    
	    if (!type.isIndexed() && (type.isStoreTermVectors())) {
	    	throw new IllegalArgumentException("cannot store term vector information "
	    			+ "for a field that is not indexed");
	    }
	}

	/**
	 * The value of the field as a String, or null. If null, the Reader value or
	 * binary value is used. Exactly one of stringValue(), readerValue(), and
	 * getBinaryValue() must be set.
	 */
	@Override
	public String getStringValue() {
	    return mFieldsData instanceof String ? (String) mFieldsData : null;
	}
	  
	/**
	 * The value of the field as a Reader, or null. If null, the String value or
	 * binary value is used. Exactly one of stringValue(), readerValue(), and
	 * getBinaryValue() must be set.
	 */
	@Override
	public Reader getReaderValue() {
		return mFieldsData instanceof Reader ? (Reader) mFieldsData : null;
	}
  
	/**
	 * The TokesStream for this field to be used when indexing, or null. If null,
	 * the Reader value or String value is analyzed to produce the indexed tokens.
	 */
	public ITokenStream getTokenStream() {
		return mTokenStream;
	}
	  
	/**
	 * <p>
	 * Expert: change the value of this field. This can be used during indexing to
	 * re-use a single Field instance to improve indexing speed by avoiding GC
	 * cost of new'ing and reclaiming Field instances. Typically a single
	 * {@link Document} instance is re-used as well. This helps most on small
	 * documents.
	 * </p>
	 * <p>
	 * Each Field instance should only be used once within a single
	 * {@link Document} instance. See <a
	 * href="http://wiki.apache.org/lucene-java/ImproveIndexingSpeed"
	 * >ImproveIndexingSpeed</a> for details.
	 * </p>
	 */
	public void setStringValue(String value) {
		if (!(mFieldsData instanceof String)) {
			throw new IllegalArgumentException("cannot change value type from " 
					+ mFieldsData.getClass().getSimpleName() + " to String");
		}
		
		mFieldsData = value;
	}

	/**
	 * Expert: change the value of this field. See 
	 * {@link #setStringValue(String)}.
	 */
	public void setReaderValue(Reader value) {
		if (!(mFieldsData instanceof Reader)) {
			throw new IllegalArgumentException("cannot change value type from " 
					+ mFieldsData.getClass().getSimpleName() + " to Reader");
		}
		
		mFieldsData = value;
	}
	  
	/**
	 * Expert: change the value of this field. See 
	 * {@link #setStringValue(String)}.
	 */
	public void setBytesValue(byte[] value) {
		setBytesValue(new BytesRef(value));
	}

	/**
	 * Expert: change the value of this field. See 
	 * {@link #setStringValue(String)}.
	 *
	 * <p>NOTE: the provided BytesRef is not copied so be sure
	 * not to change it until you're done with this field.
	 */
	public void setBytesValue(BytesRef value) {
		if (!(mFieldsData instanceof BytesRef)) {
			throw new IllegalArgumentException("cannot change value type from " 
					+ mFieldsData.getClass().getSimpleName() + " to BytesRef");
		}
		
		if (mType.isIndexed()) 
			throw new IllegalArgumentException("cannot set a Reader value on an indexed field");
		
		mFieldsData = value;
	}

	public void setByteValue(byte value) {
		if (!(mFieldsData instanceof Byte)) {
			throw new IllegalArgumentException("cannot change value type from " 
					+ mFieldsData.getClass().getSimpleName() + " to Byte");
		}
		
		setNumericValue((int)value);
		
		mFieldsData = Byte.valueOf(value);
	}

	public void setShortValue(short value) {
		if (!(mFieldsData instanceof Short)) {
			throw new IllegalArgumentException("cannot change value type from " 
					+ mFieldsData.getClass().getSimpleName() + " to Short");
		}
		
		setNumericValue((int)value);
		
		mFieldsData = Short.valueOf(value);
	}

	public void setIntValue(int value) {
		if (!(mFieldsData instanceof Integer)) {
			throw new IllegalArgumentException("cannot change value type from " 
					+ mFieldsData.getClass().getSimpleName() + " to Integer");
		}
		
		setNumericValue(value);
		
		mFieldsData = Integer.valueOf(value);
	}

	public void setLongValue(long value) {
		if (!(mFieldsData instanceof Long)) {
			throw new IllegalArgumentException("cannot change value type from " 
					+ mFieldsData.getClass().getSimpleName() + " to Long");
		}
		
		setNumericValue(value);
		
		mFieldsData = Long.valueOf(value);
	}

	public void setFloatValue(float value) {
		if (!(mFieldsData instanceof Float)) {
			throw new IllegalArgumentException("cannot change value type from " 
					+ mFieldsData.getClass().getSimpleName() + " to Float");
		}
		
		setNumericValue(value);
		
		mFieldsData = Float.valueOf(value);
	}

	public void setDoubleValue(double value) {
		if (!(mFieldsData instanceof Double)) {
			throw new IllegalArgumentException("cannot change value type from " 
					+ mFieldsData.getClass().getSimpleName() + " to Double");
		}
		
		setNumericValue(value);
		
		mFieldsData = Double.valueOf(value);
	}

	/**
	 * Expert: sets the token stream to be used for indexing and causes
	 * isIndexed() and isTokenized() to return true. May be combined with stored
	 * values from stringValue() or getBinaryValue()
	 */
	public void setTokenStream(ITokenStream tokenStream) {
		if (!mType.isIndexed() || !mType.isTokenized()) 
			throw new IllegalArgumentException("TokenStream fields must be indexed and tokenized");
		
		if (mType.getNumericType() != null) 
			throw new IllegalArgumentException("cannot set private TokenStream on numeric fields");
		
		mTokenStream = tokenStream;
	}
  
	@Override
	public String getName() {
		return mName;
	}
  
	@Override
	public float getBoost() {
		return mBoost;
	}

	/** 
	 * Sets the boost factor hits on this field.  This value will be
	 * multiplied into the score of all hits on this this field of this
	 * document.
	 *
	 * <p>The boost is used to compute the norm factor for the field.  By
	 * default, in the {@link Similarity#computeNorm(FieldInvertState, Norm)} method, 
	 * the boost value is multiplied by the length normalization factor and then
	 * rounded by {@link DefaultSimilarity#encodeNormValue(float)} before it is stored in the
	 * index.  One should attempt to ensure that this product does not overflow
	 * the range of that encoding.
	 *
	 * @see Similarity#computeNorm(FieldInvertState, Norm)
	 * @see DefaultSimilarity#encodeNormValue(float)
	 */
	@Override
	public void setBoost(float boost) {
		mBoost = boost;
	}

	@Override
	public Number getNumericValue() {
		if (mFieldsData instanceof Number) 
			return (Number) mFieldsData;
		else 
			return null;
	}

	@Override
	public BytesRef getBinaryValue() {
		if (mFieldsData instanceof BytesRef) 
			return (BytesRef) mFieldsData;
		else 
			return null;
	}
  
	/** Returns the {@link FieldType} for this field. */
	@Override
	public FieldType getFieldType() {
		return mType;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ITokenStream tokenStream(IAnalyzer analyzer) throws IOException {
		if (!getFieldType().isIndexed()) 
			return null;

		final NumericType numericType = getFieldType().getNumericType();
		if (numericType != null) {
			return getNumericTokenStream(numericType, 
					(Number) mFieldsData, getFieldType().getNumericPrecisionStep());
		}

		if (!getFieldType().isTokenized()) {
			final String stringValue = getStringValue();
			if (stringValue == null) 
				throw new IllegalArgumentException("Non-Tokenized Fields must have a String value");

			return getNotTokenizedTokenStream(stringValue);
		}

		if (mTokenStream != null) {
			return mTokenStream;
		} else if (getReaderValue() != null) {
			return analyzer.tokenStream(getName(), getReaderValue());
		} else if (getStringValue() != null) {
			return analyzer.tokenStream(getName(), new StringReader(getStringValue()));
		}

		throw new IllegalArgumentException("Field must have either TokenStream, String, Reader or Number value");
	}
	
	/** Prints a Field for human consumption. */
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append(getClass().getSimpleName());
		result.append("{");
		result.append(mType.toString());
		result.append(",field:");
		result.append(mName);
		result.append('=');

		if (mFieldsData != null) 
			result.append(mFieldsData);

		result.append("}");
		return result.toString();
	}
	
}
