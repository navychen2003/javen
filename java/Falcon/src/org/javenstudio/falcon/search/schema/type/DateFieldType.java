package org.javenstudio.falcon.search.schema.type;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.common.indexdb.document.Fieldable;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.CharsRef;
import org.javenstudio.common.indexdb.util.UnicodeUtil;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.DateParser;
import org.javenstudio.falcon.util.DateUtils;
import org.javenstudio.falcon.util.ISO8601CanonicalDateFormat;
import org.javenstudio.falcon.util.TextWriter;
import org.javenstudio.falcon.util.ThreadLocalDateFormat;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.search.query.TermRangeQuery;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.query.QueryBuilder;
import org.javenstudio.falcon.search.schema.DateFieldSource;
import org.javenstudio.falcon.search.schema.PrimitiveFieldType;
import org.javenstudio.falcon.search.schema.SchemaField;

// TODO: make a FlexibleDateField that can accept dates in multiple
// formats, better for human entered dates.
// TODO: make a DayField that only stores the day?

/**
 * FieldType that can represent any Date/Time with millisecond precision.
 * <p>
 * Date Format for the XML, incoming and outgoing:
 * </p>
 * <blockquote>
 * A date field shall be of the form 1995-12-31T23:59:59Z
 * The trailing "Z" designates UTC time and is mandatory
 * (See below for an explanation of UTC).
 * Optional fractional seconds are allowed, as long as they do not end
 * in a trailing 0 (but any precision beyond milliseconds will be ignored).
 * All other parts are mandatory.
 * </blockquote>
 * <p>
 * This format was derived to be standards compliant (ISO 8601) and is a more
 * restricted form of the
 * <a href="http://www.w3.org/TR/xmlschema-2/#dateTime-canonical-representation">canonical
 * representation of dateTime</a> from XML schema part 2.  Examples...
 * </p>
 * <ul>
 *   <li>1995-12-31T23:59:59Z</li>
 *   <li>1995-12-31T23:59:59.9Z</li>
 *   <li>1995-12-31T23:59:59.99Z</li>
 *   <li>1995-12-31T23:59:59.999Z</li>
 * </ul>
 * <p>
 * Note that DateField is lenient with regards to parsing fractional
 * seconds that end in trailing zeros and will ensure that those values
 * are indexed in the correct canonical format.
 * </p>
 * <p>
 * This FieldType also supports incoming "Date Math" strings for computing
 * values by adding/rounding internals of time relative either an explicit
 * datetime (in the format specified above) or the literal string "NOW",
 * ie: "NOW+1YEAR", "NOW/DAY", "1995-12-31T23:59:59.999Z+5MINUTES", etc...
 * -- see {@link DateMathParser} for more examples.
 * </p>
 *
 * <p>
 * Explanation of "UTC"...
 * </p>
 * <blockquote>
 * "In 1970 the Coordinated Universal Time system was devised by an
 * international advisory group of technical experts within the International
 * Telecommunication Union (ITU).  The ITU felt it was best to designate a
 * single abbreviation for use in all languages in order to minimize
 * confusion.  Since unanimous agreement could not be achieved on using
 * either the English word order, CUT, or the French word order, TUC, the
 * acronym UTC was chosen as a compromise."
 * </blockquote>
 *
 *
 * @see <a href="http://www.w3.org/TR/xmlschema-2/#dateTime">XML schema part 2</a>
 *
 */
public class DateFieldType extends PrimitiveFieldType {
  
	// The XML (external) date format will sort correctly, except if
	// fractions of seconds are present (because '.' is lower than 'Z').
	// The easiest fix is to simply remove the 'Z' for the internal
	// format.
  
	protected static final String NOW = "NOW";
	protected static final char Z = 'Z';
	protected static final char[] Z_ARRAY = new char[] {Z};
  
	/**
	 * Thread safe DateFormat that can <b>format</b> in the canonical
	 * ISO8601 date format, not including the trailing "Z" (since it is
	 * left off in the internal indexed values)
	 */
	protected static final ThreadLocalDateFormat sFmtThreadLocal = 
			new ThreadLocalDateFormat(new ISO8601CanonicalDateFormat());
  
	@Override
	public String toInternal(String val) throws ErrorException {
		return toInternal(parseMath(null, val));
	}

	/**
	 * Parses a String which may be a date (in the standard format)
	 * followed by an optional math expression.
	 * @param now an optional fixed date to use as "NOW" in the DateMathParser
	 * @param val the string to parse
	 */
	public Date parseMath(Date now, String val) throws ErrorException {
		final DateParser p = new DateParser();
    
		if (now != null) 
			p.setNow(now);
    
		String math = null;
		
		if (val.startsWith(NOW)) {
			math = val.substring(NOW.length());
			
		} else {
			final int zz = val.indexOf(Z);
			if (zz > 0) {
				math = val.substring(zz+1);
				
				try {
					// p.setNow(toObject(val.substring(0,zz)));
					p.setNow(parseDate(val.substring(0,zz+1)));
				} catch (Exception e) {
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
							"Invalid Date in Date Math String:'" + val + '\'', e);
				}
			} else {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
						"Invalid Date String:'" + val + '\'');
			}
		}

		if (math == null || math.length() == 0) 
			return p.getNow();
    
		try {
			return p.parseMath(math);
		} catch (ParseException e) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
					"Invalid Date Math String:'" + val + '\'', e);
		}
	}

	@Override
	public Fieldable createField(SchemaField field, Object value, float boost) 
			throws ErrorException {
		// Convert to a string before indexing
		if (value instanceof Date) 
			value = toInternal((Date)value) + Z;
		
		return super.createField(field, value, boost);
	}
  
	public String toInternal(Date val) {
		return formatDate(val);
	}

	@Override
	public String indexedToReadable(String indexedForm) throws ErrorException {
		return indexedForm + Z;
	}

	@Override
	public CharsRef indexedToReadable(BytesRef input, CharsRef charsRef) 
			throws ErrorException {
		UnicodeUtil.UTF8toUTF16(input, charsRef);
		charsRef.append(Z_ARRAY, 0, 1);
		return charsRef;
	}

	@Override
	public String toExternal(Fieldable f) throws ErrorException {
		return indexedToReadable(f.getStringValue());
	}

	public Date toObject(String indexedForm) throws ErrorException {
		return parseDate(indexedToReadable(indexedForm));
	}

	@Override
	public Date toObject(Fieldable f) throws ErrorException {
		return parseDate( toExternal(f) );
	}

	@Override
	public ISortField getSortField(SchemaField field,boolean reverse) 
			throws ErrorException {
		return getStringSort(field,reverse);
	}

	@Override
	public ValueSource getValueSource(SchemaField field, QueryBuilder parser) 
			throws ErrorException {
		field.checkFieldCacheSource(parser);
		return new DateFieldSource(field.getName(), field.getType());
	}
	
	@Override
	public void write(TextWriter writer, String name, Fieldable f) 
			throws ErrorException {
		try {
			writer.writeDate(name, toExternal(f));
		} catch (IOException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
	}

	/**
	 * Returns a formatter that can be use by the current thread if needed to
	 * convert Date objects to the Internal representation.
	 *
	 * Only the <tt>format(Date)</tt> can be used safely.
	 * 
	 * @deprecated - use formatDate(Date) instead
	 */
	@Deprecated
	protected DateFormat getThreadLocalDateFormat() {
		return sFmtThreadLocal.get();
	}

	/**
	 * Thread safe method that can be used by subclasses to format a Date
	 * using the Internal representation.
	 */
	protected String formatDate(Date d) {
		return sFmtThreadLocal.get().format(d);
	}

	/**
	 * Return the standard human readable form of the date
	 */
	public static String formatExternal(Date d) {
		return sFmtThreadLocal.get().format(d) + 'Z';
	}

	/**
	 * @see #formatExternal
	 */
	public String toExternal(Date d) {
		return formatExternal(d);
	}

	/**
	 * Thread safe method that can be used by subclasses to parse a Date
	 * that is already in the internal representation
	 */
	public static Date parseDate(String s) throws ErrorException {
		try {
			return sFmtThreadLocal.get().parse(s);
		} catch (ParseException e) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}

	/** Parse a date string in the standard format, or any supported by DateUtil.parseDate */
	public Date parseDateLenient(String s, ISearchRequest req) throws ErrorException {
		// request could define timezone in the future
		try {
			return sFmtThreadLocal.get().parse(s);
		} catch (ParseException e) {
			try {
				return DateUtils.parseDate(s);
			} catch (ParseException ex) { 
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
			}
		}
	}

	/**
	 * Parses a String which may be a date
	 * followed by an optional math expression.
	 * @param now an optional fixed date to use as "NOW" in the DateMathParser
	 * @param val the string to parse
	 */
	public Date parseMathLenient(Date now, String val, ISearchRequest req) 
			throws ErrorException {
		final DateParser p = new DateParser();
		if (now != null) 
			p.setNow(now);

		String math = null;
		if (val.startsWith(NOW)) {
			math = val.substring(NOW.length());
			
		} else {
			final int zz = val.indexOf(Z);
			if (0 < zz) {
				math = val.substring(zz+1);
				try {
					// p.setNow(toObject(val.substring(0,zz)));
					p.setNow(parseDateLenient(val.substring(0,zz+1), req));
				} catch (ErrorException e) {
					throw e;
				}
			} else {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
						"Invalid Date String:'" + val + '\'');
			}
		}

		if (math == null || math.length() == 0) 
			return p.getNow();

		try {
			return p.parseMath(math);
		} catch (ParseException e) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
					"Invalid Date Math String:'" + val + '\'', e);
		}
	}
  
	/** DateField specific range query */
	public IQuery getRangeQuery(QueryBuilder parser, SchemaField sf, Date part1, Date part2, 
			boolean minInclusive, boolean maxInclusive) {
		return TermRangeQuery.newStringRange(sf.getName(),
				part1 == null ? null : toInternal(part1),
				part2 == null ? null : toInternal(part2),
				minInclusive, maxInclusive);
	}

}
