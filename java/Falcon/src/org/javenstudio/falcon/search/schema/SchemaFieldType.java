package org.javenstudio.falcon.search.schema;

import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.indexdb.IAnalyzer;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.ISimilarity;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.common.indexdb.IndexOptions;
import org.javenstudio.common.indexdb.document.FieldType;
import org.javenstudio.common.indexdb.document.Fieldable;
import org.javenstudio.common.indexdb.document.TextField;
import org.javenstudio.common.indexdb.index.term.Term;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.CharsRef;
import org.javenstudio.common.indexdb.util.UnicodeUtil;
import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.TextWriter;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.search.query.TermQuery;
import org.javenstudio.hornet.search.query.TermRangeQuery;
import org.javenstudio.falcon.search.analysis.BaseAnalyzer;
import org.javenstudio.falcon.search.analysis.DefaultAnalyzer;
import org.javenstudio.falcon.search.hits.Sorting;
import org.javenstudio.falcon.search.query.QueryBuilder;

/**
 * Base class for all field types used by an index schema.
 *
 */
public abstract class SchemaFieldType extends FieldProperties {
	public static final Logger LOG = Logger.getLogger(SchemaFieldType.class);

	/**
	 * The default poly field separator.
	 *
	 * @see #createFields(SchemaField, Object, float)
	 * @see #isPolyField()
	 */
	public static final String POLY_FIELD_SEPARATOR = "___";

	/** The name of the type (not the name of the field) */
	protected String mTypeName;
	
	/** additional arguments specified in the field type declaration */
	protected Map<String,String> mArgs;
	
	/**
	 * Analyzer set by schema for text types to use when indexing fields
	 * of this type, subclasses can set analyzer themselves or override
	 * getAnalyzer()
	 * @see #getAnalyzer
	 * @see #setAnalyzer
	 */
	protected IAnalyzer mAnalyzer = new DefaultAnalyzer(this, 256);

	/**
	 * Analyzer set by schema for text types to use when searching fields
	 * of this type, subclasses can set analyzer themselves or override
	 * getAnalyzer()
	 * @see #getQueryAnalyzer
	 * @see #setQueryAnalyzer
	 */
	protected IAnalyzer mQueryAnalyzer = mAnalyzer;
	
	protected ISimilarity mSimilarity;
	
	/** properties explicitly set to true */
	protected int mPropertiesTrue;
	
	/** properties explicitly set to false */
	protected int mPropertiesFalse;
	
	/** The postings format used for this field type */
	protected String mPostingsFormat;
	
	protected int mProperties;

	public int getFieldProps() { 
		return mProperties;
	}
	
	/** Returns true if fields of this type should be tokenized */
	public boolean isTokenized() {
		return (mProperties & TOKENIZED) != 0;
	}

	/** Returns true if fields can have multiple values */
	public boolean isMultiValued() {
		return (mProperties & MULTIVALUED) != 0;
	}
  
	/** Check if a property is set */
	protected boolean hasProperty(int p) {
		return (mProperties & p) != 0;
	}

	/**
	 * A "polyField" is a FieldType that can produce more than one 
	 * IndexableField instance for a single value, via the 
	 * {@link #createFields(SchemaField, Object, float)} method.  This is useful
	 * when hiding the implementation details of a field from the end user.
	 * For instance, a spatial point may be represented by multiple different fields.
	 * @return true if the {@link #createFields(SchemaField, Object, float)} 
	 * method may return more than one field
	 */
	public boolean isPolyField(){
		return false;
	}

	/** 
	 * Returns true if a single field value of this type has multiple logical values
	 *  for the purposes of faceting, sorting, etc.  Text fields normally return
	 *  true since each token/word is a logical value.
	 */
	public boolean isMultiValuedFieldCache() {
		return isTokenized();
	}

	/** 
	 * subclasses should initialize themselves with the args provided
	 * and remove valid arguments.  leftover arguments will cause an exception.
	 * Common boolean properties have already been handled.
	 */
	public void init(IndexSchema schema, Map<String, String> args) throws ErrorException {
		// do nothing
	}

	protected String getArg(String n, Map<String,String> args) throws ErrorException {
		String s = args.remove(n);
		if (s == null) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Missing parameter '"+n+"' for FieldType=" + mTypeName + " " + args);
		}
		return s;
	}

	// Handle additional arguments...
	protected void setArgs(IndexSchema schema, Map<String,String> args) throws ErrorException {
		// default to STORED, INDEXED, OMIT_TF_POSITIONS and MULTIVALUED depending on schema version
		mProperties = (STORED | INDEXED | STORE_TERMVECTORS | STORE_TERMPOSITIONS | STORE_TERMOFFSETS);
		
		float schemaVersion = schema.getVersion();
		if (schemaVersion < 1.1f) mProperties |= MULTIVALUED;
		if (schemaVersion > 1.1f) mProperties |= OMIT_TF_POSITIONS;
		if (schemaVersion < 1.3) 
			args.remove("compressThreshold");

		mArgs = args;
		Map<String,String> initArgs = new HashMap<String,String>(args);

		mPropertiesTrue = FieldProperties.parseProperties(initArgs,true);
		mPropertiesFalse = FieldProperties.parseProperties(initArgs,false);

		mProperties &= ~mPropertiesFalse;
		mProperties |= mPropertiesTrue;

		for (String prop : FieldProperties.sPropertyNames) { 
			initArgs.remove(prop); 
		}

		init(schema, initArgs);

		String positionInc = initArgs.get("positionIncrementGap");
		
		if (positionInc != null) {
			IAnalyzer analyzer = getAnalyzer();
			if (analyzer instanceof BaseAnalyzer) {
				((BaseAnalyzer)analyzer).setPositionIncrementGap(Integer.parseInt(positionInc));
			} else {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
						"Can't set positionIncrementGap on custom analyzer " + analyzer.getClass());
			}
			
			analyzer = getQueryAnalyzer();
			if (analyzer instanceof BaseAnalyzer) {
				((BaseAnalyzer)analyzer).setPositionIncrementGap(Integer.parseInt(positionInc));
			} else {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
						"Can't set positionIncrementGap on custom analyzer " + analyzer.getClass());
			}
			
			initArgs.remove("positionIncrementGap");
		}

		final String postingsFormat = initArgs.get("postingsFormat");
		if (postingsFormat != null) {
			mPostingsFormat = postingsFormat;
			initArgs.remove("postingsFormat");
		}

		if (initArgs.size() > 0) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"schema fieldtype " + mTypeName + "(" + getClass().getName() + ")"
					+ " invalid arguments: " + initArgs);
		}
	}

	/** :TODO: document this method */
	protected void restrictProps(int props) throws ErrorException {
		if ((mProperties & props) != 0) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"schema fieldtype " + mTypeName + "(" + this.getClass().getName() + ")"
					+ " invalid properties:" + propertiesToString(mProperties & props));
		}
	}

	/** The Name of this FieldType as specified in the schema file */
	public String getTypeName() {
		return mTypeName;
	}

	public void setTypeName(String typeName) {
		mTypeName = typeName;
	}

	/**
	 * Used for adding a document when a field needs to be created from a
	 * type and a string.
	 *
	 * <p>
	 * By default, the indexed value is the same as the stored value
	 * (taken from toInternal()).   Having a different representation for
	 * external, internal, and indexed would present quite a few problems
	 * given the current indexdb architecture.  An analyzer for adding docs
	 * would need to translate internal->indexed while an analyzer for
	 * querying would need to translate external-&gt;indexed.
	 * </p>
	 * <p>
	 * The only other alternative to having internal==indexed would be to have
	 * internal==external.   In this case, toInternal should convert to
	 * the indexed representation, toExternal() should do nothing, and
	 * createField() should *not* call toInternal, but use the external
	 * value and set tokenized=true to get indexdb to convert to the
	 * internal(indexed) form.
	 * </p>
	 *
	 * :TODO: clean up and clarify this explanation.
	 *
	 * @see #toInternal
	 */
	public Fieldable createField(SchemaField field, Object value, float boost) 
			throws ErrorException {
		if (!field.isIndexed() && !field.isStored()) {
			if (LOG.isWarnEnabled())
				LOG.warn("Ignoring unindexed/unstored field: " + field);
			
			return null;
		}
    
		final String val;
		try {
			val = toInternal(value.toString());
		} catch (RuntimeException e) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Error while creating field '" + field + "' from value '" + value + "'", e);
		}
		
		if (val == null) 
			return null;

		FieldType newType = new FieldType();
		
		newType.setIndexed(field.isIndexed());
		newType.setTokenized(field.isTokenized());
		newType.setStored(field.isStored());
		newType.setOmitNorms(field.isOmitNorms());
		newType.setIndexOptions(getIndexOptions(field, val));
		newType.setStoreTermVectors(field.isStoreTermVector());
		newType.setStoreTermVectorOffsets(field.isStoreTermOffsets());
		newType.setStoreTermVectorPositions(field.isStoreTermPositions());
    
		return createField(field.getName(), val, newType, boost);
	}

	/**
	 * Create the field from native indexdb parts.  Mostly intended for use by 
	 * FieldTypes outputing multiple Fields per SchemaField
	 * @param name The name of the field
	 * @param val The _internal_ value to index
	 * @param type {@link FieldType}
	 * @param boost The boost value
	 * @return the {@link Fieldable}.
	 */
	protected Fieldable createField(String name, String val, FieldType type, float boost){
		Fieldable f = new TextField(name, val, type);
		f.setBoost(boost);
		return f;
	}

	/**
	 * Given a {@link SchemaField}, create one or more {@link IndexableField} instances
	 * @param field the {@link SchemaField}
	 * @param value The value to add to the field
	 * @param boost The boost to apply
	 * @return An array of {@link IndexableField}
	 *
	 * @see #createField(SchemaField, Object, float)
	 * @see #isPolyField()
	 */
	public Fieldable[] createFields(SchemaField field, Object value, float boost) 
			throws ErrorException {
		Fieldable f = createField( field, value, boost);
		return f == null ? new Fieldable[]{} : new Fieldable[]{f};
	}
  
	protected IndexOptions getIndexOptions(SchemaField field, String internalVal) {
		IndexOptions options = IndexOptions.DOCS_AND_FREQS_AND_POSITIONS;
		if (field.isOmitTermFreqAndPositions()) 
			options = IndexOptions.DOCS_ONLY;
		else if (field.isOmitPositions()) 
			options = IndexOptions.DOCS_AND_FREQS;
		
		return options;
	}

	/**
	 * Convert an external value (from XML update command or from query string)
	 * into the internal format for both storing and indexing (which can be modified by any analyzers).
	 * @see #toExternal
	 */
	public String toInternal(String val) throws ErrorException {
		// - used in delete when a Term needs to be created.
		// - used by the default getTokenizer() and createField()
		return val;
	}

	/**
	 * Convert the stored-field format to an external (string, human readable)
	 * value
	 * @see #toInternal
	 */
	public String toExternal(Fieldable f) throws ErrorException {
		// currently used in writing XML of the search result (but perhaps
		// a more efficient toXML(IndexableField f, Writer w) should be used
		// in the future.
		return f.getStringValue();
	}

	/**
	 * Convert the stored-field format to an external object.
	 * @see #toInternal
	 * @since 1.3
	 */
	public Object toObject(Fieldable f) throws ErrorException {
		return toExternal(f); // by default use the string
	}

	public Object toObject(SchemaField sf, BytesRef term) throws ErrorException {
		final CharsRef ref = new CharsRef(term.getLength());
		indexedToReadable(term, ref);
		final Fieldable f = createField(sf, ref.toString(), 1.0f);
		return toObject(f);
	}

	/** Given an indexed term, return the human readable representation */
	public String indexedToReadable(String indexedForm) throws ErrorException {
		return indexedForm;
	}

	/** Given an indexed term, append the human readable representation*/
	public CharsRef indexedToReadable(BytesRef input, CharsRef output) throws ErrorException {
		UnicodeUtil.UTF8toUTF16(input, output);
		return output;
	}

	/** Given the stored field, return the human readable representation */
	public String storedToReadable(Fieldable f) throws ErrorException {
		return toExternal(f);
	}

	/** Given the stored field, return the indexed form */
	public String storedToIndexed(Fieldable f) throws ErrorException {
		// right now, the transformation of single valued fields like SortableInt
		// is done when the Field is created, not at analysis time... this means
		// that the indexed form is the same as the stored field form.
		return f.getStringValue();
	}

	/** Given the readable value, return the term value that will match it. */
	public String readableToIndexed(String val) throws ErrorException {
		return toInternal(val);
	}

	/** Given the readable value, return the term value that will match it. */
	public void readableToIndexed(CharSequence val, BytesRef result) throws ErrorException {
		final String internal = readableToIndexed(val.toString());
		UnicodeUtil.UTF16toUTF8(internal, 0, internal.length(), result);
	}

	/**
	 * Returns the Analyzer to be used when indexing fields of this type.
	 * <p>
	 * This method may be called many times, at any time.
	 * </p>
	 * @see #getQueryAnalyzer
	 */
	public IAnalyzer getAnalyzer() {
		return mAnalyzer;
	}

	/**
	 * Returns the Analyzer to be used when searching fields of this type.
	 * <p>
	 * This method may be called many times, at any time.
	 * </p>
	 * @see #getAnalyzer
	 */
	public IAnalyzer getQueryAnalyzer() {
		return mQueryAnalyzer;
	}

	/**
	 * Sets the Analyzer to be used when indexing fields of this type.
	 *
	 * <p>
	 * The default implementation throws a ErrorException.  
	 * Subclasses that override this method need to ensure the behavior 
	 * of the analyzer is consistent with the implementation of toInternal.
	 * </p>
	 * 
	 * @see #toInternal
	 * @see #setQueryAnalyzer
	 * @see #getAnalyzer
	 */
	public void setAnalyzer(IAnalyzer analyzer) throws ErrorException {
		throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR,
				"FieldType: " + getClass().getSimpleName() + " (" + mTypeName 
				+ ") does not support specifying an analyzer");
	}

	/**
	 * Sets the Analyzer to be used when querying fields of this type.
	 *
	 * <p>
	 * The default implementation throws a ErrorException.  
	 * Subclasses that override this method need to ensure the behavior 
	 * of the analyzer is consistent with the implementation of toInternal.
	 * </p>
	 * 
	 * @see #toInternal
	 * @see #setAnalyzer
	 * @see #getQueryAnalyzer
	 */
	public void setQueryAnalyzer(IAnalyzer analyzer) throws ErrorException {
		throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR,
				"FieldType: " + this.getClass().getSimpleName() + " (" + mTypeName 
				+ ") does not support specifying an analyzer");
	}

	/**
	 * Gets the Similarity used when scoring fields of this type
	 * 
	 * <p>
	 * The default implementation returns null, which means this type
	 * has no custom similarity associated with it.
	 * </p>
	 * 
	 */
	public ISimilarity getSimilarity() {
		return mSimilarity;
	}
  
	/**
	 * Sets the Similarity used when scoring fields of this type
	 * 
	 */
	public void setSimilarity(ISimilarity similarity) {
		mSimilarity = similarity;
	}
  
	public String getPostingsFormat() {
		return mPostingsFormat;
	}
  
	/** calls back to TextResponseWriter to write the field value */
	public abstract void write(TextWriter writer, String name, Fieldable f) 
			throws ErrorException;

	/**
	 * Returns the SortField instance that should be used to sort fields
	 * of this type.
	 * @see SchemaField#checkSortability
	 */
	public abstract ISortField getSortField(SchemaField field, boolean top) 
			throws ErrorException;

	/**
	 * Utility usable by subclasses when they want to get basic String sorting
	 * using common checks.
	 * @see SchemaField#checkSortability
	 */
	protected ISortField getStringSort(SchemaField field, boolean reverse) 
			throws ErrorException {
		field.checkSortability();
		return Sorting.getStringSortField(field.getName(), reverse, 
				field.isSortMissingLast(), field.isSortMissingFirst());
	}

	/** 
	 * called to get the default value source (normally, from the FieldCache.)
	 */
	public ValueSource getValueSource(SchemaField field, QueryBuilder parser) 
			throws ErrorException {
		field.checkFieldCacheSource(parser);
	    return new StringFieldSource(field.getName());
	}
	
	/**
	 * Returns a Query instance for doing range searches on this field type. {@link QueryParser}
	 * currently passes part1 and part2 as null if they are '*' respectively. minInclusive 
	 * and maxInclusive are both true
	 * currently by QueryParser but that may change in the future. 
	 * Also, other QueryParser implementations may have different semantics.
	 * <p/>
	 * Sub-classes should override this method to provide their own range query implementation. 
	 * They should strive to handle nulls in part1 and/or part2 as well as unequal minInclusive 
	 * and maxInclusive parameters gracefully.
	 *
	 * @param field        the schema field
	 * @param part1        the lower boundary of the range, nulls are allowed.
	 * @param part2        the upper boundary of the range, nulls are allowed
	 * @param minInclusive whether the minimum of the range is inclusive or not
	 * @param maxInclusive whether the maximum of the range is inclusive or not
	 * @return a Query instance to perform range search according to given parameters
	 */
	public IQuery getRangeQuery(QueryBuilder parser, SchemaField field, String part1, String part2, 
			boolean minInclusive, boolean maxInclusive) throws ErrorException {
		// constant score mode is now enabled per default
		return TermRangeQuery.newStringRange(
				field.getName(),
				part1 == null ? null : toInternal(part1),
				part2 == null ? null : toInternal(part2),
				minInclusive, maxInclusive);
	}

	/**
	 * Returns a Query instance for doing searches against a field.
	 * @param parser The {@link QueryBuilder} calling the method
	 * @param field The {@link SchemaField} of the field to search
	 * @param externalVal The String representation of the value to search
	 * @return The {@link Query} instance.  This implementation returns a {@link TermQuery} 
	 * but overriding queries may not
	 */
	public IQuery getFieldQuery(QueryBuilder parser, SchemaField field, 
			String externalVal) throws ErrorException {
		BytesRef br = new BytesRef();
		readableToIndexed(externalVal, br);
		return new TermQuery(new Term(field.getName(), br));
	}

	/**
	 * Check's {@link SchemaField} instances constructed 
	 * using this field type to ensure that they are valid.
	 *
	 * <p>
	 * This method is called by the <code>SchemaField</code> constructor to 
	 * check that it's initialization does not violate any fundemental 
	 * requirements of the <code>FieldType</code>.  The default implementation 
	 * does nothing, but subclasses may chose to throw a {@link ErrorException}  
	 * if invariants are violated by the <code>SchemaField.</code>
	 * </p>
	 * 
	 */
	public void checkSchemaField(final SchemaField field) throws ErrorException {
		// :NOOP:
	}
	
	@Override
	public String toString() {
		return mTypeName + "{class=" + getClass().getName() + ","
				+ propertiesToString(mProperties)
				+ (mAnalyzer != null ? ",analyzer=" + mAnalyzer.getClass().getName() : "")
				+ ",args=" + mArgs + "}";
	}
	
}
