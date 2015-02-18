package org.javenstudio.falcon.search.schema;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.indexdb.IDocument;
import org.javenstudio.common.indexdb.IField;
import org.javenstudio.common.indexdb.analysis.Analyzer;
import org.javenstudio.common.indexdb.document.Fieldable;
import org.javenstudio.common.indexdb.search.Similarity;
import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.Constants;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContextLoader;
import org.javenstudio.falcon.search.analysis.IndexAnalyzer;
import org.javenstudio.falcon.search.analysis.QueryAnalyzer;

public final class IndexSchema {
	static final Logger LOG = Logger.getLogger(IndexSchema.class);
	
	public static final String DEFAULT_SCHEMA_FILE = Constants.SCHEMA_XML_FILENAME;

	protected final Map<String, SchemaField> mFields = 
			new HashMap<String, SchemaField>();
	protected final Map<String, SchemaFieldType> mFieldTypes = 
			new HashMap<String, SchemaFieldType>();

	protected final List<SchemaField> mFieldsWithDefaultValue = 
			new ArrayList<SchemaField>();
	protected final Collection<SchemaField> mRequiredFields = 
			new HashSet<SchemaField>();
	
	protected final Map<String, List<CopyField>> mCopyFieldsMap = 
			new HashMap<String, List<CopyField>>();
	
	/**
	 * keys are all fields copied to, count is num of copyField
	 * directives that target them.
	 */
	protected Map<SchemaField, Integer> mCopyFieldTargetCounts = 
			new HashMap<SchemaField, Integer>();
	
	protected final ContextLoader mLoader;
	protected final String mResourceName;
	
	protected String mName;
	protected float mVersion;
	
	protected DynamicField[] mDynamicFields;
	protected DynamicCopy[] mDynamicCopyFields;
	
	protected Analyzer mAnalyzer;
	protected Analyzer mQueryAnalyzer;
	protected Similarity mSimilarity;

	protected String mDefaultSearchFieldName = null;
	protected String mQueryParserDefaultOperator = "OR";
	
	protected SchemaField mUniqueKeyField;
	
	protected String mUniqueKeyFieldName;
	protected SchemaFieldType mUniqueKeyFieldType;

	public IndexSchema(ContextLoader loader, String name) 
			throws ErrorException {
		this(loader, name, (InputStream)null);
	}
	
	/**
	 * Constructs a schema using the specified resource name and stream.
	 * If the is stream is null, the resource loader will load the schema resource by name.
	 * @see ContextLoader#openSchema
	 * By default, this follows the normal config path directory searching rules.
	 * @see ContextLoader#openResource
	 */
	public IndexSchema(ContextLoader loader, String name, InputStream is) 
			throws ErrorException {
		if (name == null)
			name = DEFAULT_SCHEMA_FILE;
		
		mResourceName = name;
		mLoader = loader;
		
		try {
			if (is == null) 
				is = mLoader.openResourceAsStream(name);
			
			IndexSchemaHelper.readSchema(this, is);
			//loader.inform(loader);
			
		} catch (Exception e) {
			if (e instanceof ErrorException)
				throw (ErrorException)e;
			else
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}
  
	public ContextLoader getContextLoader() {
		return mLoader;
	}
  
	/** Gets the name of the resource used to instantiate this schema. */
	public String getResourceName() {
		return mResourceName;
	}
  
	/** Gets the name of the schema as specified in the schema resource. */
	public String getSchemaName() {
		return mName;
	}
  
	public float getVersion() {
		return mVersion;
	}

	/**
	 * Provides direct access to the Map containing all explicit
	 * (ie: non-dynamic) fields in the index, keyed on field name.
	 *
	 * <p>
	 * Modifying this Map (or any item in it) will affect the real schema
	 * </p>
	 * 
	 * <p>
	 * NOTE: this function is not thread safe.  However, it is safe to use within the standard
	 * <code>inform( Core core )</code> function for <code>CoreAware</code> classes.
	 * Outside <code>inform</code>, this could potentially throw a ConcurrentModificationException
	 * </p>
	 */
	public Map<String,SchemaField> getFields() { 
		return mFields; 
	}

	/**
	 * Provides direct access to the Map containing all Field Types
	 * in the index, keyed on field type name.
	 *
	 * <p>
	 * Modifying this Map (or any item in it) will affect the real schema.  However if you 
	 * make any modifications, be sure to call {@link IndexSchema#refreshAnalyzers()} to
	 * update the Analyzers for the registered fields.
	 * </p>
	 * 
	 * <p>
	 * NOTE: this function is not thread safe.  However, it is safe to use within the standard
	 * <code>inform( Core core )</code> function for <code>CoreAware</code> classes.
	 * Outside <code>inform</code>, this could potentially throw a ConcurrentModificationException
	 * </p>
	 */
	public Map<String,SchemaFieldType> getFieldTypes() { 
		return mFieldTypes; 
	}

	/**
	 * Provides direct access to the List containing all fields with a default value
	 */
	public List<SchemaField> getFieldsWithDefaultValue() { 
		return mFieldsWithDefaultValue; 
	}

	/**
	 * Provides direct access to the List containing all required fields.  This
	 * list contains all fields with default values.
	 */
	public Collection<SchemaField> getRequiredFields() { 
		return mRequiredFields; 
	}

	/**
	 * Returns the Similarity used for this index
	 */
	public Similarity getSimilarity() { 
		return mSimilarity; 
	}

	/**
	 * Returns the Analyzer used when indexing documents for this index
	 *
	 * <p>
	 * This Analyzer is field (and dynamic field) name aware, and delegates to
	 * a field specific Analyzer based on the field type.
	 * </p>
	 */
	public Analyzer getAnalyzer() { 
		return mAnalyzer; 
	}

	/**
	 * Returns the Analyzer used when searching this index
	 *
	 * <p>
	 * This Analyzer is field (and dynamic field) name aware, and delegates to
	 * a field specific Analyzer based on the field type.
	 * </p>
	 */
	public Analyzer getQueryAnalyzer() { 
		return mQueryAnalyzer; 
	}

	/**
	 * Name of the default search field specified in the schema file.
	 * <br/><b>Note:</b>Avoid calling this, try to use this method so that the 'df' param 
	 * is consulted as an override:
	 * {@link QueryParsing#getDefaultField(IndexSchema, String)}
	 */
	public String getDefaultSearchFieldName() {
		return mDefaultSearchFieldName;
	}

	/**
	 * default operator ("AND" or "OR") for QueryParser
	 */
	public String getQueryParserDefaultOperator() {
		return mQueryParserDefaultOperator;
	}

	/**
	 * Unique Key field specified in the schema file
	 * @return null if this schema has no unique key field
	 */
	public SchemaField getUniqueKeyField() { 
		return mUniqueKeyField; 
	}

	/**
	 * The raw (field type encoded) value of the Unique Key field for
	 * the specified Document
	 * @return null if this schema has no unique key field
	 * @see #printableUniqueKey
	 */
	public IField getUniqueKeyField(IDocument doc) {
		return doc.getField(mUniqueKeyFieldName);  // this should return null if name is null
	}

	/**
	 * The printable value of the Unique Key field for
	 * the specified Document
	 * @return null if this schema has no unique key field
	 */
	public String getPrintableUniqueKey(IDocument doc) throws ErrorException {
		IField f = doc.getField(mUniqueKeyFieldName);
		return (f == null) ? null : mUniqueKeyFieldType.toExternal((Fieldable)f);
	}

	protected SchemaField getIndexedField(String fname) throws ErrorException {
		SchemaField f = getFields().get(fname);
		if (f == null) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"unknown field '" + fname + "'");
		}
		if (!f.isIndexed()) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"'" + fname + "' is not an indexed field:" + f);
		}
		return f;
	}
  
	/**
	 * This will re-create the Analyzers.  If you make any modifications to
	 * the Field map ({@link IndexSchema#getFields()}, this function is required
	 * to synch the internally cached field analyzers.
	 * 
	 */
	public void refreshAnalyzers() {
		mAnalyzer = new IndexAnalyzer(this);
		mQueryAnalyzer = new QueryAnalyzer(this);
	}

	protected void addDynamicField(List<DynamicField> dFields, 
			SchemaField f) throws ErrorException {
		boolean dup = isDuplicateDynField(dFields, f);
		if( !dup ) {
			addDynamicFieldNoDupCheck(dFields, f);
			
		} else {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"[schema.xml] Duplicate DynamicField definition for '" + f.getName() + "'");
		}
	}

	/**
	 * Register one or more new Dynamic Field with the Schema.
	 * @param f The {@link SchemaField}
	 */
	public void registerDynamicField(SchemaField... f) throws ErrorException {
		List<DynamicField> dynFields = new ArrayList<DynamicField>(Arrays.asList(mDynamicFields));
		for (SchemaField field : f) {
			if (isDuplicateDynField(dynFields, field) == false) {
				if (LOG.isDebugEnabled())
					LOG.debug("dynamic field creation for schema field: " + field.getName());
				
				addDynamicFieldNoDupCheck(dynFields, field);
			} else {
				if (LOG.isDebugEnabled())
					LOG.debug("dynamic field already exists: dynamic field: [" + field.getName() + "]");
			}
		}
		
		Collections.sort(dynFields);
		mDynamicFields = dynFields.toArray(new DynamicField[dynFields.size()]);
	}

	protected void addDynamicFieldNoDupCheck(List<DynamicField> dFields, 
			SchemaField f) throws ErrorException {
		dFields.add(new DynamicField(f));
		
		if (LOG.isDebugEnabled())
			LOG.debug("dynamic field defined: " + f);
	}

	private boolean isDuplicateDynField(List<DynamicField> dFields, SchemaField f) {
		for (DynamicField df : dFields) {
			if (df.getRegex().equals(f.getName())) 
				return true;
		}
		return false;
	}

	public void registerCopyField(String source, String dest) throws ErrorException {
		registerCopyField(source, dest, CopyField.UNLIMITED);
	}

	/**
	 * <p>
	 * NOTE: this function is not thread safe.  However, it is safe to use within the standard
	 * <code>inform( Core core )</code> function for <code>CoreAware</code> classes.
	 * Outside <code>inform</code>, this could potentially throw a ConcurrentModificationException
	 * </p>
	 * 
	 * @see CoreAware
	 */
	public void registerCopyField(String source, String dest, int maxChars) 
			throws ErrorException {
		boolean sourceIsPattern = isWildCard(source);
		boolean destIsPattern   = isWildCard(dest);

		if (LOG.isDebugEnabled())
			LOG.debug("copyField source='"+source+"' dest='"+dest+"' maxChars='"+maxChars);
		
		SchemaField d = getFieldOrNull(dest);
		if (d == null) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"copyField destination :'" + dest + "' does not exist" );
		}

		if (sourceIsPattern) {
			if (destIsPattern) {
				DynamicField df = null;
				for (DynamicField dd : mDynamicFields) {
					if (dd.getRegex().equals(dest)) {
						df = dd;
						break;
					}
				}
				
				if (df == null) {
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
							"copyField dynamic destination must match a dynamicField.");
				}
				
				registerDynamicCopyField(new DynamicDestCopy(source, df, maxChars));
				
			} else {
				registerDynamicCopyField(new DynamicCopy(source, d, maxChars));
			}
			
		} else if (destIsPattern) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"copyField only supports a dynamic destination if the source is also dynamic");
			
		} else {
			// retrieve the field to force an exception if it doesn't exist
			SchemaField f = getField(source);

			List<CopyField> copyFieldList = mCopyFieldsMap.get(source);
			if (copyFieldList == null) {
				copyFieldList = new ArrayList<CopyField>();
				mCopyFieldsMap.put(source, copyFieldList);
			}
			
			copyFieldList.add(new CopyField(f, d, maxChars));

			mCopyFieldTargetCounts.put(d, 
					(mCopyFieldTargetCounts.containsKey(d) ? mCopyFieldTargetCounts.get(d) + 1 : 1));
		}
	}
  
	private void registerDynamicCopyField(DynamicCopy dcopy) {
		if (mDynamicCopyFields == null) {
			mDynamicCopyFields = new DynamicCopy[] {dcopy};
			
		} else {
			DynamicCopy[] temp = new DynamicCopy[mDynamicCopyFields.length+1];
			System.arraycopy(mDynamicCopyFields, 0, temp, 0, mDynamicCopyFields.length);
			temp[temp.length -1] = dcopy;
			
			mDynamicCopyFields = temp;
		}
		
		if (LOG.isDebugEnabled())
			LOG.debug("Dynamic Copy Field: " + dcopy);
	}

	protected static Object[] append(Object[] orig, Object item) {
		Object[] newArr = (Object[])java.lang.reflect.Array.newInstance(
				orig.getClass().getComponentType(), orig.length+1);
		System.arraycopy(orig, 0, newArr, 0, orig.length);
		newArr[orig.length] = item;
		return newArr;
	}

	public SchemaField[] getDynamicFieldPrototypes() {
		SchemaField[] df = new SchemaField[mDynamicFields.length];
		for (int i=0; i < mDynamicFields.length; i++) {
			df[i] = mDynamicFields[i].getPrototype();
		}
		return df;
	}

	public String getDynamicPattern(String fieldName) {
		for (DynamicField df : mDynamicFields) {
			if (df.matches(fieldName)) 
				return df.getRegex();
		}
		return  null; 
	}
  
	/**
	 * Does the schema have the specified field defined explicitly, i.e.
	 * not as a result of a copyField declaration with a wildcard?  We
	 * consider it explicitly defined if it matches a field or dynamicField
	 * declaration.
	 * @return true if explicitly declared in the schema.
	 */
	public boolean hasExplicitField(String fieldName) {
		if (mFields.containsKey(fieldName)) 
			return true;

		for (DynamicField df : mDynamicFields) {
			if (df.matches(fieldName)) 
				return true;
		}

		return false;
	}

	/**
	 * Is the specified field dynamic or not.
	 * @return true if the specified field is dynamic
	 */
	public boolean isDynamicField(String fieldName) {
		if (mFields.containsKey(fieldName)) 
			return false;

		for (DynamicField df : mDynamicFields) {
			if (df.matches(fieldName)) 
				return true;
		}

		return false;
	}

	/**
	 * Returns the SchemaField that should be used for the specified field name, or
	 * null if none exists.
	 *
	 * @param fieldName may be an explicitly defined field or a name that
	 * matches a dynamic field.
	 * @see #getFieldType
	 * @see #getField(String)
	 * @return The {@link SchemaField}
	 */
	public SchemaField getFieldOrNull(String fieldName) throws ErrorException {
		SchemaField f = mFields.get(fieldName);
		if (f != null) return f;

		for (DynamicField df : mDynamicFields) {
			if (df.matches(fieldName)) 
				return df.makeSchemaField(fieldName);
		}

		return f;
	}

	/**
	 * Returns the SchemaField that should be used for the specified field name
	 *
	 * @param fieldName may be an explicitly defined field or a name that
	 * matches a dynamic field.
	 * @throws ErrorException if no such field exists
	 * @see #getFieldType
	 * @see #getFieldOrNull(String)
	 * @return The {@link SchemaField}
	 */
	public SchemaField getField(String fieldName) throws ErrorException {
		SchemaField f = getFieldOrNull(fieldName);
		if (f != null) return f;


		// Hmmm, default field could also be implemented with a dynamic field of "*".
		// It would have to be special-cased and only used if nothing else matched.
		/***  REMOVED -YCS
    	if (defaultFieldType != null) return new SchemaField(fieldName,defaultFieldType);
		 ***/
		
		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
				"undefined field: \"" + fieldName + "\"");
	}

	/**
	 * Returns the FieldType for the specified field name.
	 *
	 * <p>
	 * This method exists because it can be more efficient then
	 * {@link #getField} for dynamic fields if a full SchemaField isn't needed.
	 * </p>
	 *
	 * @param fieldName may be an explicitly created field, or a name that
	 * excercies a dynamic field.
	 * @throws ErrorException if no such field exists
	 * @see #getField(String)
	 * @see #getFieldTypeNoEx
	 */
	public SchemaFieldType getFieldType(String fieldName) throws ErrorException {
		SchemaField f = mFields.get(fieldName);
		if (f != null) return f.getType();

		return getDynamicFieldType(fieldName);
	}

	/**
	 * Given the name of a {@link SchemaFieldType} (not to be confused with {@link #getFieldType(String)} which
	 * takes in the name of a field), return the {@link SchemaFieldType}.
	 * @param fieldTypeName The name of the {@link SchemaFieldType}
	 * @return The {@link SchemaFieldType} or null.
	 */
	public SchemaFieldType getFieldTypeByName(String fieldTypeName){
		return mFieldTypes.get(fieldTypeName);
	}

	/**
	 * Returns the FieldType for the specified field name.
	 *
	 * <p>
	 * This method exists because it can be more efficient then
	 * {@link #getField} for dynamic fields if a full SchemaField isn't needed.
	 * </p>
	 *
	 * @param fieldName may be an explicitly created field, or a name that
	 * excercies a dynamic field.
	 * @return null if field is not defined.
	 * @see #getField(String)
	 * @see #getFieldTypeNoEx
	 */
	public SchemaFieldType getFieldTypeNoEx(String fieldName) {
		SchemaField f = mFields.get(fieldName);
		if (f != null) 
			return f.getType();
		
		return dynFieldType(fieldName);
	}

	/**
	 * Returns the FieldType of the best matching dynamic field for
	 * the specified field name
	 *
	 * @param fieldName may be an explicitly created field, or a name that
	 * excercies a dynamic field.
	 * @throws ErrorException if no such field exists
	 * @see #getField(String)
	 * @see #getFieldTypeNoEx
	 */
	public SchemaFieldType getDynamicFieldType(String fieldName) throws ErrorException {
		for (DynamicField df : mDynamicFields) {
			if (df.matches(fieldName)) 
				return df.getPrototype().getType();
		}
		
		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
				"undefined field " + fieldName);
	}

	private SchemaFieldType dynFieldType(String fieldName) {
		for (DynamicField df : mDynamicFields) {
			if (df.matches(fieldName)) 
				return df.getPrototype().getType();
		}
		return null;
	}

	/**
	 * Get all copy fields, both the static and the dynamic ones.
	 * @return Array of fields copied into this field
	 */
	public SchemaField[] getCopySources(String destField) throws ErrorException {
		SchemaField f = getField(destField);
		if (!isCopyFieldTarget(f)) 
			return new SchemaField[0];
		
		List<SchemaField> sf = new ArrayList<SchemaField>();
		for (Map.Entry<String, List<CopyField>> cfs : mCopyFieldsMap.entrySet()) {
			for (CopyField copyField : cfs.getValue()) {
				if (copyField.getDestination().getName().equals(destField)) 
					sf.add(copyField.getSource());
			}
		}
		
		return sf.toArray(new SchemaField[sf.size()]);
	}

	/**
	 * Get all copy fields for a specified source field, both static
	 * and dynamic ones.
	 * @return List of CopyFields to copy to.
	 */
	// This is useful when we need the maxSize param of each CopyField
	public List<CopyField> getCopyFieldsList(final String sourceField) throws ErrorException {
		final List<CopyField> result = new ArrayList<CopyField>();
		for (DynamicCopy dynamicCopy : mDynamicCopyFields) {
			if (dynamicCopy.matches(sourceField)) {
				result.add(new CopyField(getField(sourceField), 
						dynamicCopy.getTargetField(sourceField), dynamicCopy.getMaxChars()));
			}
		}
		
		List<CopyField> fixedCopyFields = mCopyFieldsMap.get(sourceField);
		if (fixedCopyFields != null) 
			result.addAll(fixedCopyFields);

		return result;
	}
  
	/**
	 * Check if a field is used as the destination of a copyField operation 
	 * 
	 */
	public boolean isCopyFieldTarget(SchemaField f) {
		return mCopyFieldTargetCounts.containsKey(f);
	}

	/**
	 * Is the given field name a wildcard?  I.e. does it begin or end with *?
	 * @return true/false
	 */
	public static boolean isWildCard(String name) {
		return name.startsWith("*") || name.endsWith("*");
	}
	
}
