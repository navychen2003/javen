package org.javenstudio.falcon.search.schema;

import java.util.Map;

import org.javenstudio.falcon.ErrorException;

/**
 * Abstract class defining shared behavior for primitive types
 * Intended to be used as base class for non-analyzed fields like
 * int, float, string, date etc, and set proper defaults for them 
 */
public abstract class PrimitiveFieldType extends SchemaFieldType {
	
	@Override
	public void init(IndexSchema schema, Map<String,String> args) throws ErrorException {
		super.init(schema, args);
		if (schema.getVersion() > 1.4) 
			mProperties |= OMIT_NORMS;
	}
	
}
