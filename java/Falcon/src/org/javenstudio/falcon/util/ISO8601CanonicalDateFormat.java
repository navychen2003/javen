package org.javenstudio.falcon.util;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


public class ISO8601CanonicalDateFormat extends SimpleDateFormat {
	private static final long serialVersionUID = 1L;

	public static TimeZone UTC = TimeZone.getTimeZone("UTC");

	/** 
	 * No longer used
	 * @deprecated use DateParser.DEFAULT_MATH_TZ
	 * @see DateMathParser#DEFAULT_MATH_TZ
	 */
	public static final TimeZone MATH_TZ = DateParser.DEFAULT_MATH_TZ;
	
	/** 
	 * No longer used
	 * @deprecated use DateParser.DEFAULT_MATH_LOCALE
	 * @see DateMathParser#DEFAULT_MATH_LOCALE
	 */
	public static final Locale MATH_LOCALE = DateParser.DEFAULT_MATH_LOCALE;

	/** 
	 * Fixed TimeZone (UTC) needed for parsing/formating Dates in the 
	 * canonical representation.
	 */
	public static final TimeZone CANONICAL_TZ = UTC;
	
	/** 
	 * Fixed Locale needed for parsing/formating Milliseconds in the 
	 * canonical representation.
	 */
	public static final Locale CANONICAL_LOCALE = Locale.ROOT;
	
	protected NumberFormat mMillisParser = NumberFormat.getIntegerInstance(CANONICAL_LOCALE);

	protected NumberFormat mMillisFormat = new DecimalFormat(".###", 
			new DecimalFormatSymbols(CANONICAL_LOCALE));

	public ISO8601CanonicalDateFormat() {
		super("yyyy-MM-dd'T'HH:mm:ss", CANONICAL_LOCALE);
		setTimeZone(CANONICAL_TZ);
	}

	@Override
	public Date parse(String i, ParsePosition p) {
		/** delegate to SimpleDateFormat for easy stuff */
		Date d = super.parse(i, p);
		int milliIndex = p.getIndex();
		
		/** worry about the milliseconds ourselves */
		if (d != null && p.getErrorIndex() == -1 && milliIndex + 1 < i.length() && 
			i.charAt(milliIndex) == '.') {
			p.setIndex(++milliIndex); // NOTE: ++ to chomp '.'
			
			Number millis = mMillisParser.parse(i, p);
			
			if (p.getErrorIndex() == -1) {
				int endIndex = p.getIndex();
				d = new Date(d.getTime() + (long)(millis.doubleValue() *
						Math.pow(10, (3-endIndex+milliIndex))));
			}
		}
		
		return d;
	}

	@Override
	public StringBuffer format(Date d, StringBuffer toAppendTo, FieldPosition pos) {
		/** delegate to SimpleDateFormat for easy stuff */
		super.format(d, toAppendTo, pos);
		
		/** worry aboutthe milliseconds ourselves */
		long millis = d.getTime() % 1000l;
		if (millis == 0L) 
			return toAppendTo;
  
		if (millis < 0L) {
			// original date was prior to epoch
			millis += 1000L;
		}
		
		int posBegin = toAppendTo.length();
		toAppendTo.append(mMillisFormat.format(millis / 1000d));
		
		if (DateFormat.MILLISECOND_FIELD == pos.getField()) {
			pos.setBeginIndex(posBegin);
			pos.setEndIndex(toAppendTo.length());
		}
		
		return toAppendTo;
	}

	@Override
	public DateFormat clone() {
		ISO8601CanonicalDateFormat c = (ISO8601CanonicalDateFormat) super.clone();
		c.mMillisParser = NumberFormat.getIntegerInstance(CANONICAL_LOCALE);
		c.mMillisFormat = new DecimalFormat(".###", new DecimalFormatSymbols(CANONICAL_LOCALE));
		return c;
	}
	
}
