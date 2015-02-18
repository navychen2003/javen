package org.javenstudio.falcon.search.query.source;

import java.util.Date;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.source.DualFloatFunction;
import org.javenstudio.falcon.search.query.FunctionQueryBuilder;
import org.javenstudio.falcon.search.query.ValueSourceParser;
import org.javenstudio.falcon.search.schema.SchemaField;
import org.javenstudio.falcon.search.schema.type.DateFieldType;
import org.javenstudio.falcon.search.schema.type.TrieDateFieldType;

public class DateSourceParser extends ValueSourceParser {
	
	DateFieldType mFieldType = new TrieDateFieldType();

	@Override
	public void init(NamedList<?> args) {
		// do nothing
	}

	public Date getDate(FunctionQueryBuilder fp, String arg) throws ErrorException {
		if (arg == null) return null;
		
		if (arg.startsWith("NOW") || (arg.length() > 0 && Character.isDigit(arg.charAt(0)))) 
			return mFieldType.parseMathLenient(null, arg, fp.getRequest());
		
		return null;
	}

	public ValueSource getValueSource(FunctionQueryBuilder fp, 
			String arg) throws ErrorException {
		if (arg == null) return null;
		
		SchemaField f = getSearchCore().getSchema().getField(arg);
		if (f.getType().getClass() == DateFieldType.class) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Can't use ms() function on non-numeric legacy date field " + arg);
		}
		
		return f.getType().getValueSource(f, fp);
	}

	@Override
	public ValueSource parse(FunctionQueryBuilder fp) throws ErrorException {
		String first = fp.parseArg();
		String second = fp.parseArg();
		
		if (first == null) 
			first = "NOW";

		Date d1 = getDate(fp, first);
		ValueSource v1 = d1 == null ? getValueSource(fp, first) : null;

		Date d2 = getDate(fp, second);
		ValueSource v2 = d2 == null ? getValueSource(fp, second) : null;

		// d     constant
		// v     field
		// dd    constant
		// dv    subtract field from constant
		// vd    subtract constant from field
		// vv    subtract fields

		final long ms1 = (d1 == null) ? 0 : d1.getTime();
		final long ms2 = (d2 == null) ? 0 : d2.getTime();

		// "d,dd" handle both constant cases

		if (d1 != null && v2 == null) 
			return new LongConstSource(ms1 - ms2);
		
		// "v" just the date field
		if (v1 != null && v2 == null && d2 == null) 
			return v1;

		// "dv"
		if (d1 != null && v2 != null) {
			return new DualFloatFunction(new LongConstSource(ms1), v2) {
					@Override
					protected String getName() {
						return "ms";
					}
		
					@Override
					protected float callFunc(int doc, FunctionValues aVals, FunctionValues bVals) {
						return ms1 - bVals.longVal(doc);
					}
				};
		}

		// "vd"
		if (v1 != null && d2 != null) {
			return new DualFloatFunction(v1, new LongConstSource(ms2)) {
					@Override
					protected String getName() {
						return "ms";
					}
		
					@Override
					protected float callFunc(int doc, FunctionValues aVals, FunctionValues bVals) {
						return aVals.longVal(doc) - ms2;
					}
				};
		}

		// "vv"
		if (v1 != null && v2 != null) {
			return new DualFloatFunction(v1, v2) {
					@Override
					protected String getName() {
						return "ms";
					}
		
					@Override
					protected float callFunc(int doc, FunctionValues aVals, FunctionValues bVals) {
						return aVals.longVal(doc) - bVals.longVal(doc);
					}
				};
		}

		return null; // shouldn't happen
	}
	
}
