package org.javenstudio.falcon.search.schema;

import java.util.Map;

import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.common.indexdb.document.Fieldable;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.query.QueryBuilder;

/**
 * Encapsulates all information about a Field in a Schema
 *
 */
public final class SchemaField extends FieldProperties {
	
	private final String mName;
	private final SchemaFieldType mType;
	private final int mProperties;
	private final String mDefaultValue;
	
	// this can't be final since it may be changed dynamically
	private boolean mRequired = false;

	/** 
	 * Create a new SchemaField with the given name and type,
	 *  using all the default properties from the type.
	 */
	public SchemaField(String name, SchemaFieldType type) throws ErrorException {
		this(name, type, type.getFieldProps(), null);
	}

	/** 
	 * Create a new SchemaField from an existing one by using all
	 * of the properties of the prototype except the field name.
	 */
	public SchemaField(SchemaField prototype, String name) throws ErrorException {
		this(name, prototype.mType, prototype.mProperties, prototype.mDefaultValue );
	}

	/** 
	 * Create a new SchemaField with the given name and type,
	 * and with the specified properties.  Properties are *not*
	 * inherited from the type in this case, so users of this
	 * constructor should derive the properties from type.getProperties()
	 *  using all the default properties from the type.
	 */
	public SchemaField(String name, SchemaFieldType type, int properties, 
			String defaultValue) throws ErrorException{
		mName = name;
		mType = type;
		mProperties = properties;
		mDefaultValue = defaultValue;
    
		// initalize with the required property flag
		mRequired = (properties & REQUIRED) !=0;

		type.checkSchemaField(this);
	}

	public String getName() { return mName; }
	public SchemaFieldType getType() { return mType; }
	public int getFieldProps() { return mProperties; }

	public boolean isIndexed() { return (mProperties & INDEXED) != 0; }
	public boolean isStored() { return (mProperties & STORED) != 0; }
	public boolean isStoreTermVector() { return (mProperties & STORE_TERMVECTORS) != 0; }
	public boolean isStoreTermPositions() { return (mProperties & STORE_TERMPOSITIONS) != 0; }
	public boolean isStoreTermOffsets() { return (mProperties & STORE_TERMOFFSETS) != 0; }
	public boolean isOmitNorms() { return (mProperties & OMIT_NORMS) != 0; }

	public boolean isOmitTermFreqAndPositions() { return (mProperties & OMIT_TF_POSITIONS) != 0; }
	public boolean isOmitPositions() { return (mProperties & OMIT_POSITIONS) != 0; }

	public boolean isMultiValued() { return (mProperties & MULTIVALUED) != 0; }
	public boolean isSortMissingFirst() { return (mProperties & SORT_MISSING_FIRST) != 0; }
	public boolean isSortMissingLast() { return (mProperties & SORT_MISSING_LAST) != 0; }
	public boolean isRequired() { return mRequired; } 

	// things that should be determined by field type, not set as options
	public boolean isTokenized() { return (mProperties & TOKENIZED) != 0; }
	public boolean isBinary() { return (mProperties & BINARY) != 0; }

	public void setRequired(boolean required) { mRequired = required; }
	
	public Fieldable createField(Object val, float boost) throws ErrorException {
		return mType.createField(this, val, boost);
	}
  
	public Fieldable[] createFields(Object val, float boost) throws ErrorException {
		return mType.createFields(this,val,boost);
	}

	/**
	 * If true, then use {@link #createFields(Object, float)}, else use {@link #createField} 
	 * to save an extra allocation
	 * @return true if this field is a poly field
	 */
	public boolean isPolyField(){
		return mType.isPolyField();
	}

	/**
	 * Delegates to the FieldType for this field
	 * @see SchemaFieldType#getSortField
	 */
	public ISortField getSortField(boolean top) throws ErrorException {
		return mType.getSortField(this, top);
	}

	/** 
	 * Sanity checks that the properties of this field type are plausible 
	 * for a field that may be used in sorting, throwing an appropriate 
	 * exception (including the field name) if it is not.  FieldType subclasses 
	 * can choose to call this method in their getSortField implementation
	 * @see SchemaFieldType#getSortField
	 */
	public void checkSortability() throws ErrorException {
		if (!isIndexed() ) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"can not sort on unindexed field: " + getName());
		}
		if (isMultiValued()) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"can not sort on multivalued field: " + getName());
		}
	}

	/** 
	 * Sanity checks that the properties of this field type are plausible 
	 * for a field that may be used to get a FieldCacheSource, throwing
	 * an appropriate exception (including the field name) if it is not.  
	 * FieldType subclasses can choose to call this method in their 
	 * getValueSource implementation 
	 * @see SchemaFieldType#getValueSource
	 */
	public void checkFieldCacheSource(QueryBuilder parser) throws ErrorException {
		if (!isIndexed()) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"can not use FieldCache on unindexed field: " + getName());
		}
		if (isMultiValued()) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"can not use FieldCache on multivalued field: " + getName());
		}
	}

	public static SchemaField create(String name, SchemaFieldType ft, 
			Map<String,String> props) throws ErrorException {
		String defaultValue = null;
		if (props.containsKey("default")) 
			defaultValue = props.get("default");
		
		return new SchemaField(name, ft, calcProps(name, ft, props), defaultValue);
	}

	/**
	 * Create a SchemaField w/ the props specified.  Does not support a default value.
	 * @param name The name of the SchemaField
	 * @param ft The {@link SchemaFieldType} of the field
	 * @param props The props.  See {@link #calcProps(String, SchemaFieldType, java.util.Map)}
	 * @param defValue The default Value for the field
	 * @return The SchemaField
	 *
	 * @see #create(String, SchemaFieldType, java.util.Map)
	 */
	public static SchemaField create(String name, SchemaFieldType ft, 
			int props, String defValue) throws ErrorException {
		return new SchemaField(name, ft, props, defValue);
	}

	static int calcProps(String name, SchemaFieldType ft, Map<String, String> props) 
			throws ErrorException {
		int trueProps = parseProperties(props, true);
		int falseProps = parseProperties(props, false);
		int p = ft.getFieldProps();

		//
		// If any properties were explicitly turned off, then turn off other properties
		// that depend on that.
		//
		if (propertyOn(falseProps, STORED)) {
			int pp = STORED | BINARY;
			
			if (propertyOn(pp, trueProps)) {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR,
						"SchemaField: " + name + " conflicting stored field options:" + props);
			}
			
			p &= ~pp;
		}

		if (propertyOn(falseProps, INDEXED)) {
			int pp = (INDEXED | STORE_TERMVECTORS | STORE_TERMPOSITIONS | STORE_TERMOFFSETS | 
					SORT_MISSING_FIRST | SORT_MISSING_LAST);
			
			if (propertyOn(pp, trueProps)) {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR,
						"SchemaField: " + name + " conflicting 'true' field options for non-indexed field: " 
						+ props);
			}
			
			p &= ~pp;
		}
		
		if (propertyOn(falseProps, INDEXED)) {
			int pp = (OMIT_NORMS | OMIT_TF_POSITIONS | OMIT_POSITIONS);
			
			if (propertyOn(pp, falseProps)) {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR,
						"SchemaField: " + name + " conflicting 'false' field options for non-indexed field: " 
						+ props);
			}
			
			p &= ~pp;
		}

		if (propertyOn(trueProps, OMIT_TF_POSITIONS)) {
			int pp = (OMIT_POSITIONS | OMIT_TF_POSITIONS);
			
			if (propertyOn(pp, falseProps)) {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR,
						"SchemaField: " + name + " conflicting tf and position field options: " + props);
			}
			
			p &= ~pp;
		}

		if (propertyOn(falseProps, STORE_TERMVECTORS)) {
			int pp = (STORE_TERMVECTORS | STORE_TERMPOSITIONS | STORE_TERMOFFSETS);
			
			if (propertyOn(pp, trueProps)) {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR,
						"SchemaField: " + name + " conflicting termvector field options: " + props);
			}
			
			p &= ~pp;
		}

		// override sort flags
		if (propertyOn(trueProps, SORT_MISSING_FIRST)) 
			p &= ~SORT_MISSING_LAST;

		if (propertyOn(trueProps, SORT_MISSING_LAST)) 
			p &= ~SORT_MISSING_FIRST;

		p &= ~falseProps;
		p |= trueProps;
		
		return p;
	}

	public String getDefaultValue() {
		return mDefaultValue;
	}

	@Override
	public int hashCode() {
		return mName.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return(obj instanceof SchemaField) && mName.equals(((SchemaField)obj).mName);
	}
	
	@Override
	public String toString() {
		return mName + "{type=" + mType.getTypeName()
				+ ((mDefaultValue == null) ? "" : (",default=" + mDefaultValue))
				+ ",mProperties=" + propertiesToString(mProperties)
				+ (mRequired ? ", required=true" : "")
				+ "}";
	}
	
}
