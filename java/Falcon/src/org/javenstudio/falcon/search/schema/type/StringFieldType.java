package org.javenstudio.falcon.search.schema.type;

import java.io.IOException;

import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.common.indexdb.document.Fieldable;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.TextWriter;
import org.javenstudio.falcon.search.query.QueryBuilder;
import org.javenstudio.falcon.search.schema.PrimitiveFieldType;
import org.javenstudio.falcon.search.schema.SchemaField;
import org.javenstudio.falcon.search.schema.StringFieldSource;
import org.javenstudio.hornet.query.ValueSource;

public class StringFieldType extends PrimitiveFieldType {
	
	@Override
	public ISortField getSortField(SchemaField field, boolean reverse) 
			throws ErrorException {
		return getStringSort(field,reverse);
	}

	@Override
	public ValueSource getValueSource(SchemaField field, QueryBuilder parser) 
			throws ErrorException {
		field.checkFieldCacheSource(parser);
		return new StringFieldSource(field.getName());
	}
	
	@Override
	public void write(TextWriter writer, String name, Fieldable f) throws ErrorException {
		try {
			writer.writeString(name, f.getStringValue(), true);
		} catch (IOException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
	}

	@Override
	public Object toObject(SchemaField sf, BytesRef term) {
		return term.utf8ToString();
	}
	
}

