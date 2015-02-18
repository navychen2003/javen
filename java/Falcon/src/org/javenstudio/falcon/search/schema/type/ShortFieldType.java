package org.javenstudio.falcon.search.schema.type;

import java.io.IOException;
import java.util.Map;

import org.javenstudio.common.indexdb.document.Fieldable;
import org.javenstudio.common.indexdb.search.SortField;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.TextWriter;
import org.javenstudio.hornet.search.AdvancedSortField;
import org.javenstudio.falcon.search.schema.IndexSchema;
import org.javenstudio.falcon.search.schema.PrimitiveFieldType;
import org.javenstudio.falcon.search.schema.SchemaField;

/**
 * A numeric field that can contain 16-bit signed two's complement integer
 * values, encoded as simple Strings.
 *
 * <p>
 * Field values will sort numerically, but Range Queries (and other features 
 * that rely on numeric ranges) will not work as expected: values will be 
 * evaluated in unicode String order, not numeric order.
 * </p>
 *
 * <ul>
 *  <li>Min Value Allowed: -32768</li>
 *  <li>Max Value Allowed: 32767</li>
 * </ul>
 *
 * @see Short
 **/
public class ShortFieldType extends PrimitiveFieldType {
	
	@Override
	public void init(IndexSchema schema, Map<String, String> args) throws ErrorException {
		super.init(schema, args);
		restrictProps(SORT_MISSING_FIRST | SORT_MISSING_LAST);
	}

	@Override
	public SortField getSortField(SchemaField field, boolean reverse) throws ErrorException {
		field.checkSortability();
		return new AdvancedSortField(field.getName(), SortField.Type.SHORT, reverse);
	}

	@Override
	public void write(TextWriter writer, String name, Fieldable f) throws ErrorException {
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
				short val = Short.parseShort(s);
				writer.writeInt(name, val);
				
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
	public Short toObject(Fieldable f) throws ErrorException {
		return Short.valueOf(toExternal(f));
	}

}
