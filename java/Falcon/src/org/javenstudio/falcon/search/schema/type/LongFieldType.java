package org.javenstudio.falcon.search.schema.type;

import java.io.IOException;
import java.util.Map;

import org.javenstudio.common.indexdb.document.Fieldable;
import org.javenstudio.common.indexdb.search.SortField;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.TextWriter;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.source.LongFieldSource;
import org.javenstudio.hornet.search.AdvancedSortField;
import org.javenstudio.falcon.search.query.QueryBuilder;
import org.javenstudio.falcon.search.schema.IndexSchema;
import org.javenstudio.falcon.search.schema.PrimitiveFieldType;
import org.javenstudio.falcon.search.schema.SchemaField;

/**
 * A legacy numeric field type that encodes "Long" values as simple Strings.
 * This class should not be used except by people with existing indexes that
 * contain numeric values indexed as Strings.  
 * New schemas should use {@link TrieLongField}.
 *
 * <p>
 * Field values will sort numerically, but Range Queries (and other features 
 * that rely on numeric ranges) will not work as expected: values will be 
 * evaluated in unicode String order, not numeric order.
 * </p>
 * 
 * @see TrieLongField
 */
public class LongFieldType extends PrimitiveFieldType {
	
	@Override
	public void init(IndexSchema schema, Map<String,String> args) 
			throws ErrorException {
		super.init(schema, args);
		restrictProps(SORT_MISSING_FIRST | SORT_MISSING_LAST);
	}

	@Override
	public SortField getSortField(SchemaField field,boolean reverse) 
			throws ErrorException {
		field.checkSortability();
		return new AdvancedSortField(field.getName(), SortField.Type.LONG, reverse);
	}

	@Override
	public ValueSource getValueSource(SchemaField field, QueryBuilder qparser) 
			throws ErrorException {
		field.checkFieldCacheSource(qparser);
		return new LongFieldSource(field.getName());
	}
	
	@Override
	public void write(TextWriter writer, String name, Fieldable f) 
			throws ErrorException {
		String s = f.getStringValue();

		// these values may be from a legacy index, which may
		// not be properly formatted in some output formats, or may
		// incorrectly have a zero length.
		
		try {
			if (s.length() == 0) {
				// zero length value means someone mistakenly indexed the value
				// instead of simply leaving it out.  Write a null value instead of a numeric.
				writer.writeNull(name);
				return;
			}
	
			try {
				long val = Long.parseLong(s);
				writer.writeLong(name, val);
				
			} catch (NumberFormatException e){
				// can't parse - write out the contents as a string so nothing is lost and
				// clients don't get a parse error.
				writer.writeString(name, s, true);
			}
			
		} catch (IOException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
	}

	@Override
	public Long toObject(Fieldable f) throws ErrorException {
		return Long.valueOf(toExternal(f));
	}
	
}
