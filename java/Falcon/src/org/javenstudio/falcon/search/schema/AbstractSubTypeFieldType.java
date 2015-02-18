package org.javenstudio.falcon.search.schema;

import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.MapParams;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.falcon.search.query.QueryBuilder;

/**
 * An abstract base class for FieldTypes that delegate work to another {@link SchemaFieldType}.
 * The sub type can be obtained by either specifying the subFieldType attribute or the subFieldSuffix.  
 * In the former case, a new dynamic field will be injected into the schema automatically 
 * with the name of {@link #POLY_FIELD_SEPARATOR}.
 * In the latter case, it will use an existing dynamic field definition to get the type.  
 * See the example schema and the
 * use of the {@link PointType} for more details.
 */
public abstract class AbstractSubTypeFieldType extends SchemaFieldType implements SchemaAware {
	
	public static final String SUB_FIELD_SUFFIX = "subFieldSuffix";
	public static final String SUB_FIELD_TYPE = "subFieldType";
  
	protected SchemaFieldType mSubType;
	protected String mSuffix;
	protected int mDynFieldProps;
	protected String[] mSuffixes;
	protected IndexSchema mSchema; // needed for retrieving SchemaFields

	public SchemaFieldType getSubType() {
		return mSubType;
	}

	@Override
	public void init(IndexSchema schema, Map<String, String> args) throws ErrorException {
		mSchema = schema;
		
		//it's not a first class citizen for the IndexSchema
		Params p = new MapParams(args);
		String subFT = p.get(SUB_FIELD_TYPE);
		String subSuffix = p.get(SUB_FIELD_SUFFIX);
		
		if (subFT != null) {
			args.remove(SUB_FIELD_TYPE);
			mSubType = schema.getFieldTypeByName(subFT.trim());
			mSuffix = POLY_FIELD_SEPARATOR + mSubType.getTypeName();
			
		} else if (subSuffix != null) {
			args.remove(SUB_FIELD_SUFFIX);
			mSuffix = subSuffix;
			
		} else {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"The field type: " + mTypeName + " must specify the " +
			        SUB_FIELD_TYPE + " attribute or the " + SUB_FIELD_SUFFIX + " attribute.");
		}
	}

	/**
	 * Helper method for creating a dynamic field SchemaField prototype.  
	 * Returns a {@link SchemaField} with
	 * the {@link FieldType} given and a name of "*" + 
	 * {@link FieldType#POLY_FIELD_SEPARATOR} + {@link FieldType#typeName}
	 * and props of indexed=true, stored=false.
	 *
	 * @param schema the IndexSchema
	 * @param type   The {@link FieldType} of the prototype.
	 * @return The {@link SchemaField}
	 */
	public static SchemaField registerPolyFieldDynamicPrototype(
			IndexSchema schema, SchemaFieldType type) throws ErrorException {
		String name = "*" + SchemaFieldType.POLY_FIELD_SEPARATOR + type.getTypeName();
		
		Map<String, String> props = new HashMap<String, String>();
		//Just set these, delegate everything else to the field type
		props.put("indexed", "true");
		props.put("stored", "false");
		props.put("multiValued", "false");
		
		int p = SchemaField.calcProps(name, type, props);
		SchemaField proto = SchemaField.create(name, type, p, null);
		schema.registerDynamicField(proto);
		
		return proto;
	}

	@Override
	public void inform(IndexSchema schema) throws ErrorException {
		//Can't do this until here b/c the Dynamic Fields are not initialized until here.
		if (mSubType != null) {
			SchemaField proto = registerPolyFieldDynamicPrototype(schema, mSubType);
			mDynFieldProps = proto.getFieldProps();
		}
	}

	/**
	 * Throws UnsupportedOperationException()
	 */
	@Override
	public IQuery getFieldQuery(QueryBuilder parser, SchemaField field, 
			String externalVal) throws ErrorException {
		throw new UnsupportedOperationException();
	}

	protected void createSuffixCache(int size) {
		mSuffixes = new String[size];
		for (int i=0; i < size; i++) {
			mSuffixes[i] = "_" + i + mSuffix;
		}
	}

	protected SchemaField subField(SchemaField base, int i) throws ErrorException {
		return mSchema.getField(base.getName() + mSuffixes[i]);
	}
	
}
