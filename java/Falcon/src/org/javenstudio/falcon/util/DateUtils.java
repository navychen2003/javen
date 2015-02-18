package org.javenstudio.falcon.util;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtils {
	
  public static TimeZone UTC = TimeZone.getTimeZone("UTC");

  /** 
   * No longer used
   * @deprecated use DateMathParser.DEFAULT_MATH_TZ
   * @see DateMathParser#DEFAULT_MATH_TZ
   */
  public static final TimeZone MATH_TZ = DateParser.DEFAULT_MATH_TZ;
  
  /** 
   * No longer used
   * @deprecated use DateMathParser.DEFAULT_MATH_LOCALE
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
	
  /**
   * Date format pattern used to parse HTTP date headers in RFC 1123 format.
   */
  public static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";

  /**
   * Date format pattern used to parse HTTP date headers in RFC 1036 format.
   */
  public static final String PATTERN_RFC1036 = "EEEE, dd-MMM-yy HH:mm:ss zzz";

  /**
   * Date format pattern used to parse HTTP date headers in ANSI C
   * <code>asctime()</code> format.
   */
  public static final String PATTERN_ASCTIME = "EEE MMM d HH:mm:ss yyyy";
  //These are included for back compat
  private static final Collection<String> DEFAULT_HTTP_CLIENT_PATTERNS = Arrays.asList(
          PATTERN_ASCTIME, PATTERN_RFC1036, PATTERN_RFC1123);

  private static final Date DEFAULT_TWO_DIGIT_YEAR_START;

  static {
    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.ROOT);
    calendar.set(2000, Calendar.JANUARY, 1, 0, 0);
    DEFAULT_TWO_DIGIT_YEAR_START = calendar.getTime();
  }

  private static final TimeZone GMT = TimeZone.getTimeZone("GMT");
  
  /**
   * A suite of default date formats that can be parsed, and thus transformed to the Solr specific format
   */
  public static final Collection<String> DEFAULT_DATE_FORMATS = new ArrayList<String>();

  static {
    DEFAULT_DATE_FORMATS.add("yyyy-MM-dd'T'HH:mm:ss'Z'");
    DEFAULT_DATE_FORMATS.add("yyyy-MM-dd'T'HH:mm:ss");
    DEFAULT_DATE_FORMATS.add("yyyy-MM-dd");
    DEFAULT_DATE_FORMATS.add("yyyy-MM-dd hh:mm:ss");
    DEFAULT_DATE_FORMATS.add("yyyy-MM-dd HH:mm:ss");
    DEFAULT_DATE_FORMATS.add("EEE MMM d hh:mm:ss z yyyy");
    DEFAULT_DATE_FORMATS.addAll(DEFAULT_HTTP_CLIENT_PATTERNS);
  }

  /**
   * Returns a formatter that can be use by the current thread if needed to
   * convert Date objects to the Internal representation.
   *
   * @param d The input date to parse
   * @return The parsed {@link java.util.Date}
   * @throws java.text.ParseException If the input can't be parsed
   */
  public static Date parseDate(String d) throws ParseException {
    return parseDate(d, DEFAULT_DATE_FORMATS);
  }

  public static Date parseDate(String d, Collection<String> fmts) throws ParseException {
    // 2007-04-26T08:05:04Z
    if (d.endsWith("Z") && d.length() > 20) {
      return getThreadLocalDateFormat().parse(d);
    }
    return parseDate(d, fmts, null);
  }
  
  /**
   * Slightly modified from org.apache.commons.httpclient.util.DateUtil.parseDate
   * <p/>
   * Parses the date value using the given date formats.
   *
   * @param dateValue   the date value to parse
   * @param dateFormats the date formats to use
   * @param startDate   During parsing, two digit years will be placed in the range
   *                    <code>startDate</code> to <code>startDate + 100 years</code>. This value may
   *                    be <code>null</code>. When <code>null</code> is given as a parameter, year
   *                    <code>2000</code> will be used.
   * @return the parsed date
   * @throws ParseException if none of the dataFormats could parse the dateValue
   */
  public static Date parseDate(
          String dateValue,
          Collection<String> dateFormats,
          Date startDate
  ) throws ParseException {

    if (dateValue == null) {
      throw new IllegalArgumentException("dateValue is null");
    }
    if (dateFormats == null) {
      dateFormats = DEFAULT_HTTP_CLIENT_PATTERNS;
    }
    if (startDate == null) {
      startDate = DEFAULT_TWO_DIGIT_YEAR_START;
    }
    // trim single quotes around date if present
    // see issue #5279
    if (dateValue.length() > 1
            && dateValue.startsWith("'")
            && dateValue.endsWith("'")
            ) {
      dateValue = dateValue.substring(1, dateValue.length() - 1);
    }

    SimpleDateFormat dateParser = null;
    Iterator<String> formatIter = dateFormats.iterator();

    while (formatIter.hasNext()) {
      String format = (String) formatIter.next();
      if (dateParser == null) {
        dateParser = new SimpleDateFormat(format, Locale.ROOT);
        dateParser.setTimeZone(GMT);
        dateParser.set2DigitYearStart(startDate);
      } else {
        dateParser.applyPattern(format);
      }
      try {
        return dateParser.parse(dateValue);
      } catch (ParseException pe) {
        // ignore this exception, we will try the next format
      }
    }

    // we were unable to parse the date
    throw new ParseException("Unable to parse the date " + dateValue, 0);
  }
	
  /**
   * Return the standard human readable form of the date
   */
  public static String formatExternal(Date d) {
    return fmtThreadLocal.get().format(d) + 'Z';
  }
  
   /**
    * Returns a formatter that can be use by the current thread if needed to
    * convert Date objects to the Internal representation.
    *
    * @return The {@link java.text.DateFormat} for the current thread
    */
   public static DateFormat getThreadLocalDateFormat() {
     return fmtThreadLocal.get();
   }
   
  /**
   * Thread safe DateFormat that can <b>format</b> in the canonical
   * ISO8601 date format, not including the trailing "Z" (since it is
   * left off in the internal indexed values)
   */
  private final static ThreadLocalDateFormat fmtThreadLocal
    = new ThreadLocalDateFormat(new ISO8601CanonicalDateFormat());
  
  private static class ISO8601CanonicalDateFormat extends SimpleDateFormat {
	private static final long serialVersionUID = 1L;

	protected NumberFormat millisParser
      = NumberFormat.getIntegerInstance(CANONICAL_LOCALE);

    protected NumberFormat millisFormat = new DecimalFormat(".###", 
      new DecimalFormatSymbols(CANONICAL_LOCALE));

    public ISO8601CanonicalDateFormat() {
      super("yyyy-MM-dd'T'HH:mm:ss", CANONICAL_LOCALE);
      this.setTimeZone(CANONICAL_TZ);
    }

    @Override
    public Date parse(String i, ParsePosition p) {
      /* delegate to SimpleDateFormat for easy stuff */
      Date d = super.parse(i, p);
      int milliIndex = p.getIndex();
      /* worry about the milliseconds ourselves */
      if (null != d &&
          -1 == p.getErrorIndex() &&
          milliIndex + 1 < i.length() &&
          '.' == i.charAt(milliIndex)) {
        p.setIndex( ++milliIndex ); // NOTE: ++ to chomp '.'
        Number millis = millisParser.parse(i, p);
        if (-1 == p.getErrorIndex()) {
          int endIndex = p.getIndex();
            d = new Date(d.getTime()
                         + (long)(millis.doubleValue() *
                                  Math.pow(10, (3-endIndex+milliIndex))));
        }
      }
      return d;
    }

    @Override
    public StringBuffer format(Date d, StringBuffer toAppendTo,
                               FieldPosition pos) {
      /* delegate to SimpleDateFormat for easy stuff */
      super.format(d, toAppendTo, pos);
      /* worry aboutthe milliseconds ourselves */
      long millis = d.getTime() % 1000l;
      if (0L == millis) {
        return toAppendTo;
      }
      if (millis < 0L) {
        // original date was prior to epoch
        millis += 1000L;
      }
      int posBegin = toAppendTo.length();
      toAppendTo.append(millisFormat.format(millis / 1000d));
      if (DateFormat.MILLISECOND_FIELD == pos.getField()) {
        pos.setBeginIndex(posBegin);
        pos.setEndIndex(toAppendTo.length());
      }
      return toAppendTo;
    }

    @Override
    public DateFormat clone() {
      ISO8601CanonicalDateFormat c
        = (ISO8601CanonicalDateFormat) super.clone();
      c.millisParser = NumberFormat.getIntegerInstance(CANONICAL_LOCALE);
      c.millisFormat = new DecimalFormat(".###", 
        new DecimalFormatSymbols(CANONICAL_LOCALE));
      return c;
    }
  }
  
  private static class ThreadLocalDateFormat extends ThreadLocal<DateFormat> {
    DateFormat proto;
    public ThreadLocalDateFormat(DateFormat d) {
      super();
      proto = d;
    }
    @Override
    protected DateFormat initialValue() {
      return (DateFormat) proto.clone();
    }
  }
	
}
