package org.javenstudio.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder; 
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Collection;
import java.util.HashMap;
import java.nio.charset.Charset;

/**
 * General string utils
 * @author Owen O'Malley
 */
public class StringUtils {
 
  private static final DecimalFormat decimalFormat;
  static {
    NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.ENGLISH);
    decimalFormat = (DecimalFormat) numberFormat;
    decimalFormat.applyPattern("#.##");
  }
 
  /**
   * Make a string representation of the exception.
   * @param e The exception to stringify
   * @return A string with exception name and call stack.
   */
  public static String stringifyException(Throwable e) {
    StringWriter stm = new StringWriter();
    PrintWriter wrt = new PrintWriter(stm);
    e.printStackTrace(wrt);
    wrt.close();
    return stm.toString();
  }
  
  /**
   * Given a full hostname, return the word upto the first dot.
   * @param fullHostname the full hostname
   * @return the hostname to the first dot
   */
  public static String simpleHostname(String fullHostname) {
    int offset = fullHostname.indexOf('.');
    if (offset != -1) {
      return fullHostname.substring(0, offset);
    }
    return fullHostname;
  }
 
  private static DecimalFormat oneDecimal = new DecimalFormat("0.0");
  
  /**
   * Given an integer, return a string that is in an approximate, but human 
   * readable format. 
   * It uses the bases 'k', 'm', and 'g' for 1024, 1024**2, and 1024**3.
   * @param number the number to format
   * @return a human readable form of the integer
   */
  public static String humanReadableInt(long number) {
    long absNumber = Math.abs(number);
    double result = number;
    String suffix = "";
    if (absNumber < 1024) {
      // nothing
    } else if (absNumber < 1024 * 1024) {
      result = number / 1024.0;
      suffix = "k";
    } else if (absNumber < 1024 * 1024 * 1024) {
      result = number / (1024.0 * 1024);
      suffix = "m";
    } else {
      result = number / (1024.0 * 1024 * 1024);
      suffix = "g";
    }
    return oneDecimal.format(result) + suffix;
  }
  
  /**
   * Format a percentage for presentation to the user.
   * @param done the percentage to format (0.0 to 1.0)
   * @param digits the number of digits past the decimal point
   * @return a string representation of the percentage
   */
  public static String formatPercent(double done, int digits) {
    DecimalFormat percentFormat = new DecimalFormat("0.00%");
    double scale = Math.pow(10.0, digits+2);
    double rounded = Math.floor(done * scale);
    percentFormat.setDecimalSeparatorAlwaysShown(false);
    percentFormat.setMinimumFractionDigits(digits);
    percentFormat.setMaximumFractionDigits(digits);
    return percentFormat.format(rounded / scale);
  }
  
  /**
   * Given an array of strings, return a comma-separated list of its elements.
   * @param strs Array of strings
   * @return Empty string if strs.length is 0, comma separated list of strings
   * otherwise
   */
  
  public static String arrayToString(String[] strs) {
    if (strs.length == 0) { return ""; }
    StringBuilder sbuf = new StringBuilder();
    sbuf.append(strs[0]);
    for (int idx = 1; idx < strs.length; idx++) {
      sbuf.append(",");
      sbuf.append(strs[idx]);
    }
    return sbuf.toString();
  }
 
  /**
   * Given an array of bytes it will convert the bytes to a hex string
   * representation of the bytes
   * @param bytes
   * @param start start index, inclusively
   * @param end end index, exclusively
   * @return hex string representation of the byte array
   */
  public static String byteToHexString(byte[] bytes, int start, int end) {
    if (bytes == null) {
      throw new IllegalArgumentException("bytes == null");
    }
    StringBuilder s = new StringBuilder(); 
    for(int i = start; i < end; i++) {
      s.append(String.format("%02x", bytes[i]));
    }
    return s.toString();
  }
 
  /** Same as byteToHexString(bytes, 0, bytes.length). */
  public static String byteToHexString(byte bytes[]) {
    return byteToHexString(bytes, 0, bytes.length);
  }
 
  /**
   * Given a hexstring this will return the byte array corresponding to the
   * string
   * @param hex the hex String array
   * @return a byte array that is a hex string representation of the given
   *         string. The size of the byte array is therefore hex.length/2
   */
  public static byte[] hexStringToByte(String hex) {
    byte[] bts = new byte[hex.length() / 2];
    for (int i = 0; i < bts.length; i++) {
      bts[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
    }
    return bts;
  }
 
  /**
   * 
   * @param uris
   */
  public static String uriToString(URI[] uris){
    String ret = null;
    ret = uris[0].toString();
    for(int i = 1; i < uris.length;i++){
      ret = ret + "," + uris[i].toString();
    }
    return ret;
  }
  
  /**
   * 
   * @param str
   */
  public static URI[] stringToURI(String[] str){
    if (str == null) 
      return null;
    URI[] uris = new URI[str.length];
    for (int i = 0; i < str.length;i++){
      try{
        uris[i] = new URI(str[i]);
      }catch(URISyntaxException ur){
        System.out.println("Exception in specified URI's " + StringUtils.stringifyException(ur));
        //making sure its asssigned to null in case of an error
        uris[i] = null;
      }
    }
    return uris;
  }
  
  /**
   * 
   * Given a finish and start time in long milliseconds, returns a 
   * String in the format Xhrs, Ymins, Z sec, for the time difference between two times. 
   * If finish time comes before start time then negative valeus of X, Y and Z wil return. 
   * 
   * @param finishTime finish time
   * @param startTime start time
   */
  public static String formatTimeDiff(long finishTime, long startTime){
	StringBuilder buf = new StringBuilder();
    
    long timeDiff = finishTime - startTime; 
    long hours = timeDiff / (60*60*1000);
    long rem = (timeDiff % (60*60*1000));
    long minutes =  rem / (60*1000);
    rem = rem % (60*1000);
    long seconds = rem / 1000;
    
    if (hours != 0){
      buf.append(hours);
      buf.append("hrs, ");
    }
    if (minutes != 0){
      buf.append(minutes);
      buf.append("mins, ");
    }
    // return "0sec if no difference
    buf.append(seconds);
    buf.append("sec");
    return buf.toString(); 
  }
 
  /**
   * Formats time in ms and appends difference (finishTime - startTime) 
   * as returned by formatTimeDiff().
   * If finish time is 0, empty string is returned, if start time is 0 
   * then difference is not appended to return value. 
   * @param dateFormat date format to use
   * @param finishTime fnish time
   * @param startTime start time
   * @return formatted value. 
   */
  public static String getFormattedTimeWithDiff(DateFormat dateFormat, 
                                                long finishTime, long startTime){
	StringBuilder buf = new StringBuilder();
    if (0 != finishTime) {
      buf.append(dateFormat.format(new Date(finishTime)));
      if (0 != startTime){
        buf.append(" (" + formatTimeDiff(finishTime , startTime) + ")");
      }
    }
    return buf.toString();
  }
  
  /**
   * returns an arraylist of strings  
   * @param str the comma seperated string values
   * @return the arraylist of the comma seperated string values
   */
  public static String[] getStrings2(String str){
    if (str == null)
      return null;
    StringTokenizer tokenizer = new StringTokenizer (str,",");
    List<String> values = new ArrayList<String>();
    while (tokenizer.hasMoreTokens()) {
      values.add(tokenizer.nextToken());
    }
    return (String[])values.toArray(new String[values.size()]);
  }
 
  /**
   * Returns an arraylist of strings.
   * @param str the comma seperated string values
   * @return the arraylist of the comma seperated string values
   */
  public static String[] getStrings(String str){
    Collection<String> values = getStringCollection(str);
    if(values.size() == 0) {
      return null;
    }
    return values.toArray(new String[values.size()]);
  }
 
  /**
   * Returns a collection of strings.
   * @param str comma seperated string values
   * @return an <code>ArrayList</code> of string values
   */
  public static Collection<String> getStringCollection(String str){
    List<String> values = new ArrayList<String>();
    if (str == null)
      return values;
    StringTokenizer tokenizer = new StringTokenizer (str,",");
    values = new ArrayList<String>();
    while (tokenizer.hasMoreTokens()) {
      values.add(tokenizer.nextToken());
    }
    return values;
  }
 
  final public static char COMMA = ',';
  final public static String COMMA_STR = ",";
  final public static char ESCAPE_CHAR = '\\';
  
  /**
   * Split a string using the default separator
   * @param str a string that may have escaped separator
   * @return an array of strings
   */
  public static String[] split(String str) {
    return split(str, ESCAPE_CHAR, COMMA);
  }
  
  /**
   * Split a string using the given separator
   * @param str a string that may have escaped separator
   * @param escapeChar a char that be used to escape the separator
   * @param separator a separator char
   * @return an array of strings
   */
  public static String[] split(
      String str, char escapeChar, char separator) {
    if (str==null) {
      return null;
    }
    ArrayList<String> strList = new ArrayList<String>();
    StringBuilder split = new StringBuilder();
    int index = 0;
    while ((index = findNext(str, separator, escapeChar, index, split)) >= 0) {
      ++index; // move over the separator for next search
      strList.add(split.toString());
      split.setLength(0); // reset the buffer 
    }
    strList.add(split.toString());
    // remove trailing empty split(s)
    int last = strList.size(); // last split
    while (--last>=0 && "".equals(strList.get(last))) {
      strList.remove(last);
    }
    return strList.toArray(new String[strList.size()]);
  }
  
  /**
   * Finds the first occurrence of the separator character ignoring the escaped
   * separators starting from the index. Note the substring between the index
   * and the position of the separator is passed.
   * @param str the source string
   * @param separator the character to find
   * @param escapeChar character used to escape
   * @param start from where to search
   * @param split used to pass back the extracted string
   */
  public static int findNext(String str, char separator, char escapeChar, 
                             int start, StringBuilder split) {
    int numPreEscapes = 0;
    for (int i = start; i < str.length(); i++) {
      char curChar = str.charAt(i);
      if (numPreEscapes == 0 && curChar == separator) { // separator 
        return i;
      } else {
        split.append(curChar);
        numPreEscapes = (curChar == escapeChar)
                        ? (++numPreEscapes) % 2
                        : 0;
      }
    }
    return -1;
  }
  
  /**
   * Escape commas in the string using the default escape char
   * @param str a string
   * @return an escaped string
   */
  public static String escapeString(String str) {
    return escapeString(str, ESCAPE_CHAR, COMMA);
  }
  
  /**
   * Escape <code>charToEscape</code> in the string 
   * with the escape char <code>escapeChar</code>
   * 
   * @param str string
   * @param escapeChar escape char
   * @param charToEscape the char to be escaped
   * @return an escaped string
   */
  public static String escapeString(
      String str, char escapeChar, char charToEscape) {
    return escapeString(str, escapeChar, new char[] {charToEscape});
  }
  
  // check if the character array has the character 
  private static boolean hasChar(char[] chars, char character) {
    for (char target : chars) {
      if (character == target) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * @param charsToEscape array of characters to be escaped
   */
  public static String escapeString(String str, char escapeChar, 
                                    char[] charsToEscape) {
    if (str == null) {
      return null;
    }
    StringBuilder result = new StringBuilder();
    for (int i=0; i<str.length(); i++) {
      char curChar = str.charAt(i);
      if (curChar == escapeChar || hasChar(charsToEscape, curChar)) {
        // special char
        result.append(escapeChar);
      }
      result.append(curChar);
    }
    return result.toString();
  }
  
  /**
   * Unescape commas in the string using the default escape char
   * @param str a string
   * @return an unescaped string
   */
  public static String unEscapeString(String str) {
    return unEscapeString(str, ESCAPE_CHAR, COMMA);
  }
  
  /**
   * Unescape <code>charToEscape</code> in the string 
   * with the escape char <code>escapeChar</code>
   * 
   * @param str string
   * @param escapeChar escape char
   * @param charToEscape the escaped char
   * @return an unescaped string
   */
  public static String unEscapeString(
      String str, char escapeChar, char charToEscape) {
    return unEscapeString(str, escapeChar, new char[] {charToEscape});
  }
  
  /**
   * @param charsToEscape array of characters to unescape
   */
  public static String unEscapeString(String str, char escapeChar, 
                                      char[] charsToEscape) {
    if (str == null) {
      return null;
    }
    StringBuilder result = new StringBuilder(str.length());
    boolean hasPreEscape = false;
    for (int i=0; i<str.length(); i++) {
      char curChar = str.charAt(i);
      if (hasPreEscape) {
        if (curChar != escapeChar && !hasChar(charsToEscape, curChar)) {
          // no special char
          throw new IllegalArgumentException("Illegal escaped string " + str + 
              " unescaped " + escapeChar + " at " + (i-1));
        } 
        // otherwise discard the escape char
        result.append(curChar);
        hasPreEscape = false;
      } else {
        if (hasChar(charsToEscape, curChar)) {
          throw new IllegalArgumentException("Illegal escaped string " + str + 
              " unescaped " + curChar + " at " + i);
        } else if (curChar == escapeChar) {
          hasPreEscape = true;
        } else {
          result.append(curChar);
        }
      }
    }
    if (hasPreEscape ) {
      throw new IllegalArgumentException("Illegal escaped string " + str + 
          ", not expecting " + escapeChar + " in the end." );
    }
    return result.toString();
  }
  
  /**
   * Return hostname without throwing exception.
   * @return hostname
   */
  public static String getHostname() {
    try {return "" + InetAddress.getLocalHost();}
    catch(UnknownHostException uhe) {return "" + uhe;}
  }
 
  /**
   * The traditional binary prefixes, kilo, mega, ..., exa,
   * which can be represented by a 64-bit integer.
   * TraditionalBinaryPrefix symbol are case insensitive. 
   */
  public static enum TraditionalBinaryPrefix {
    KILO(1024),
    MEGA(KILO.value << 10),
    GIGA(MEGA.value << 10),
    TERA(GIGA.value << 10),
    PETA(TERA.value << 10),
    EXA(PETA.value << 10);
 
    public final long value;
    public final char symbol;
 
    TraditionalBinaryPrefix(long value) {
      this.value = value;
      this.symbol = toString().charAt(0);
    }
 
    /**
     * @return The TraditionalBinaryPrefix object corresponding to the symbol.
     */
    public static TraditionalBinaryPrefix valueOf(char symbol) {
      symbol = Character.toUpperCase(symbol);
      for(TraditionalBinaryPrefix prefix : TraditionalBinaryPrefix.values()) {
        if (symbol == prefix.symbol) {
          return prefix;
        }
      }
      throw new IllegalArgumentException("Unknown symbol '" + symbol + "'");
    }
 
    /**
     * Convert a string to long.
     * The input string is first be trimmed
     * and then it is parsed with traditional binary prefix.
     *
     * For example,
     * "-1230k" will be converted to -1230 * 1024 = -1259520;
     * "891g" will be converted to 891 * 1024^3 = 956703965184;
     *
     * @param s input string
     * @return a long value represented by the input string.
     */
    public static long string2long(String s) {
      s = s.trim();
      final int lastpos = s.length() - 1;
      final char lastchar = s.charAt(lastpos);
      if (Character.isDigit(lastchar))
        return Long.parseLong(s);
      else {
        long prefix = TraditionalBinaryPrefix.valueOf(lastchar).value;
        long num = Long.parseLong(s.substring(0, lastpos));
        if (num > (Long.MAX_VALUE/prefix) || num < (Long.MIN_VALUE/prefix)) {
          throw new IllegalArgumentException(s + " does not fit in a Long");
        }
        return num * prefix;
      }
    }
  }
  
    /**
     * Escapes HTML Special characters present in the string.
     * @param string
     * @return HTML Escaped String representation
     */
    public static String escapeHTML(String string) {
      if(string == null) {
        return null;
      }
      StringBuilder sb = new StringBuilder();
      boolean lastCharacterWasSpace = false;
      char[] chars = string.toCharArray();
      for(char c : chars) {
        if(c == ' ') {
          if(lastCharacterWasSpace){
            lastCharacterWasSpace = false;
            sb.append("&nbsp;");
          }else {
            lastCharacterWasSpace=true;
            sb.append(" ");
          }
        }else {
          lastCharacterWasSpace = false;
          switch(c) {
          case '<': sb.append("&lt;"); break;
          case '>': sb.append("&gt;"); break;
          case '&': sb.append("&amp;"); break;
          case '"': sb.append("&quot;"); break;
          default : sb.append(c);break;
          }
        }
      }
      
      return sb.toString();
    }
 
  /**
   * Return an abbreviated English-language desc of the byte length
   */
  public static String byteDesc(long len) {
    double val = 0.0;
    String ending = "";
    if (len < 1024 * 1024) {
      val = (1.0 * len) / 1024;
      ending = " KB";
    } else if (len < 1024 * 1024 * 1024) {
      val = (1.0 * len) / (1024 * 1024);
      ending = " MB";
    } else if (len < 1024L * 1024 * 1024 * 1024) {
      val = (1.0 * len) / (1024 * 1024 * 1024);
      ending = " GB";
    } else if (len < 1024L * 1024 * 1024 * 1024 * 1024) {
      val = (1.0 * len) / (1024L * 1024 * 1024 * 1024);
      ending = " TB";
    } else {
      val = (1.0 * len) / (1024L * 1024 * 1024 * 1024 * 1024);
      ending = " PB";
    }
    return limitDecimalTo2(val) + ending;
  }
 
  public static synchronized String limitDecimalTo2(double d) {
    return decimalFormat.format(d);
  }

  public static String toString(Object obj) {
    if (obj == null) return null; 
    if (obj instanceof Date) 
      return formatDate(((Date)obj).getTime()); 
    return obj.toString(); 
  }
 
  static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  public static String formatDate(long tm) {
    return dateFormat.format(tm); 
  }
 
  public static String URLEncode(String str) {
    return URLEncode(str, getCharacterEncoding()); 
  }
 
  public static String URLEncode(String str, String encoding) {
    try {
      return URLEncoder.encode(str, encoding);
    } catch (Exception e) {
      return str; 
    }
  }
 
  public static String HTMLEncode(String str) {
    if (str == null || str.length() == 0) 
        return str; 

      StringBuilder sbuf = new StringBuilder(); 
      for( int i = 0; i < str.length(); i ++ ) {
        char chr = str.charAt(i); 
        switch (chr) {
        case '>': 
          sbuf.append("&gt;"); 
          break; 
        case '<': 
          sbuf.append("&lt;"); 
          break; 
        case '&': 
          sbuf.append("&amp;"); 
          break; 
        case '\'': 
          sbuf.append("&apos;"); 
          break; 
        case '\"': 
          sbuf.append("&quot;"); 
          break; 
        default: 
          sbuf.append(chr); 
          break; 
        }
      }
      return sbuf.toString(); 
  }
 
  public static String CDATAEncode(String str) {
    if (str == null || str.length() == 0) 
        return str; 

    StringBuilder sbuf = new StringBuilder(); 
      char ppc = 0, pc = 0; 
      for( int i = 0; i < str.length(); i ++ ) {
        char chr = str.charAt(i); 
        if (chr == '>') {
          if (ppc == ']' && pc == ']') {
            sbuf.append("]]><![CDATA["); 
          }
        }
        sbuf.append(chr); 
        ppc = pc; pc = chr; 
      }

      return sbuf.toString(); 
  }
 
  public static boolean stringEquals(String a, String b) {
    if (a == null || b == null) {
      return a==null && b==null ? true : false;
    } else {
      return a.equals(b);
    }
  }
 
  public static boolean stringsEquals(String[] a, String b[]) {
    if (a == null || b == null) {
      return a==null && b==null ? true : false;
    } else if (a.length != b.length) {
      return false; 
    } else {
      for (int i=0; i<a.length; i++) {
        if (stringEquals(a[i], b[i]) == false)
          return false; 
      }
    }
    return true; 
  }
 
  public static boolean objectEquals(Object a, Object b) {
    if (a == null || b == null) {
      return a==null && b==null ? true : false;
    } else {
      if (a.getClass() != b.getClass()) 
        return false; 
      return a.equals(b);
    }
  }
 
  public static String[] parseNames(String names) {
    ArrayList<String> items = new ArrayList<String>();
    if (names != null && names.length() > 0) {
      StringTokenizer st = new StringTokenizer(names, ",;|/\\ \t\r\n");
      while (st.hasMoreTokens()) {
        String token = st.nextToken();
        items.add(token);
      }
    }
    return items.toArray(new String[items.size()]);
  }
 
  /**
   * Returns a copy of <code>s</code> padded with trailing spaces so
   * that it's length is <code>length</code>.  Strings already
   * <code>length</code> characters long or longer are not altered.
   */
  public static String rightPad(String s, int length) {
	  StringBuilder sb= new StringBuilder(s);
    for (int i= length - s.length(); i > 0; i--) 
      sb.append(" ");
    return sb.toString();
  }
 
  /**
   * Returns a copy of <code>s</code> padded with leading spaces so
   * that it's length is <code>length</code>.  Strings already
   * <code>length</code> characters long or longer are not altered.
   */
  public static String leftPad(String s, int length) {
	StringBuilder sb= new StringBuilder();
    for (int i= length - s.length(); i > 0; i--) 
      sb.append(" ");
    sb.append(s);
    return sb.toString();
  }
 
 
  private static final char[] HEX_DIGITS =
  {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
 
  /**
   * Convenience call for {@link #toHexString(byte[], String, int)}, where
   * <code>sep = null; lineLen = Integer.MAX_VALUE</code>.
   * @param buf
   */
  public static String toHexString(byte[] buf) {
    return toHexString(buf, null, Integer.MAX_VALUE);
  }
 
  /**
   * Get a text representation of a byte[] as hexadecimal String, where each
   * pair of hexadecimal digits corresponds to consecutive bytes in the array.
   * @param buf input data
   * @param sep separate every pair of hexadecimal digits with this separator, or
   * null if no separation is needed.
   * @param lineLen break the output String into lines containing output for lineLen
   * bytes.
   */
  public static String toHexString(byte[] buf, String sep, int lineLen) {
    if (buf == null) return null;
    if (lineLen <= 0) lineLen = Integer.MAX_VALUE;
    StringBuilder res = new StringBuilder(buf.length * 2);
    for (int i = 0; i < buf.length; i++) {
      int b = buf[i];
      res.append(HEX_DIGITS[(b >> 4) & 0xf]);
      res.append(HEX_DIGITS[b & 0xf]);
      if (i > 0 && (i % lineLen) == 0) res.append('\n');
      else if (sep != null && i < lineLen - 1) res.append(sep); 
    }
    return res.toString();
  }
  
  /**
   * Convert a String containing consecutive (no inside whitespace) hexadecimal
   * digits into a corresponding byte array. If the number of digits is not even,
   * a '0' will be appended in the front of the String prior to conversion.
   * Leading and trailing whitespace is ignored.
   * @param text input text
   * @return converted byte array, or null if unable to convert
   */
  public static byte[] fromHexString(String text) {
    text = text.trim();
    if (text.length() % 2 != 0) text = "0" + text;
    int resLen = text.length() / 2;
    int loNibble, hiNibble;
    byte[] res = new byte[resLen];
    for (int i = 0; i < resLen; i++) {
      int j = i << 1;
      hiNibble = charToNibble(text.charAt(j));
      loNibble = charToNibble(text.charAt(j + 1));
      if (loNibble == -1 || hiNibble == -1) return null;
      res[i] = (byte)(hiNibble << 4 | loNibble);
    }
    return res;
  }
  
  private static final int charToNibble(char c) {
    if (c >= '0' && c <= '9') {
      return c - '0';
    } else if (c >= 'a' && c <= 'f') {
      return 0xa + (c - 'a');
    } else if (c >= 'A' && c <= 'F') {
      return 0xA + (c - 'A');
    } else {
      return -1;
    }
  }
 
  /**
   * Parse the character encoding from the specified content type header.
   * If the content type is null, or there is no explicit character encoding,
   * <code>null</code> is returned.
   * <br />
   * This method was copy from org.apache.catalina.util.RequestUtil 
   * is licensed under the Apache License, Version 2.0 (the "License").
   *
   * @param contentType a content type header
   */
  public static String parseCharacterEncoding(String contentType) {
    if (contentType == null)
      return (null);
    int start = contentType.indexOf("charset=");
    if (start < 0)
      return (null);
    String encoding = contentType.substring(start + 8);
    int end = encoding.indexOf(';');
    if (end >= 0)
      encoding = encoding.substring(0, end);
    encoding = encoding.trim();
    if ((encoding.length() > 2) && (encoding.startsWith("\""))
      && (encoding.endsWith("\"")))
      encoding = encoding.substring(1, encoding.length() - 1);
    return (encoding.trim());
 
  }
 
  /**
   * Checks if a string is empty (ie is null or empty).
   */
  public static boolean isEmpty(String str) {
    return (str == null) || (str.length() == 0);
  }
  
  public static String getSystemCharacterEncoding() {
    String encoding = System.getProperty("file.encoding"); 
    if (encoding == null || encoding.length() == 0) 
      encoding = System.getProperty("sun.jnu.encoding"); 
    if (encoding == null || encoding.length() == 0) 
      encoding = "UTF-8"; 
    return encoding; 
  }
  
  public static String getCharacterEncoding() {
    if (encodingDefault == null) {
      synchronized (encodingAliases) {
        encodingDefault = getCharacterEncoding(getSystemCharacterEncoding());
      }
    }
    return encodingDefault; 
  }
 
  public static String getCharacterEncoding(String encoding) {
    try {
      Charset.forName(encoding); 
      return encoding; 
    } catch (Exception e) {
      return "UTF-8"; 
    }
  }
 
  public static Locale getLocale() {
    if (localeDefault == null) {
      synchronized (encodingAliases) {
        localeDefault = createLocale(getLocaleString());
      }
    }
    return localeDefault;
  }
  
  public static String getLocaleString() {
    String language = System.getProperty("user.language"); 
    String country = System.getProperty("user.country"); 
    if (language == null || language.length() == 0) 
      language = "en"; 
    if (country == null || country.length() == 0) 
      country = "US"; 
    return language + "_" + country; 
  }
 
  public static Locale createLocale(String localeKey) {
      if (localeKey == null || localeKey.length() == 0) 
          return DEFAULT_LOCALE; 

      String language = null; 
      String country = null; 
      StringTokenizer st = new StringTokenizer(localeKey, "_."); 
      while (st.hasMoreTokens()) {
          if (language == null) 
              language = st.nextToken(); 
          else if (country == null) 
              country = st.nextToken(); 
          else
              break; 
      }

      Locale locale = null; 

      if (language != null && country != null) 
          locale = new Locale(language, country); 
      else if (language != null) 
          locale = new Locale(language); 

      if (locale == null) 
          locale = DEFAULT_LOCALE; 

      return locale; 
  }
 
  private static Locale DEFAULT_LOCALE = Locale.CHINESE; //Locale.getDefault();
  
  private static String encodingDefault = null; 
  private static Locale localeDefault = null; 
  private static HashMap<String,String> encodingAliases = new HashMap<String,String>();
 
  /** 
   * the following map is not an alias mapping table, but
   * maps character encodings which are often used in mislabelled
   * documents to their correct encodings. For instance,
   * there are a lot of documents labelled 'ISO-8859-1' which contain
   * characters not covered by ISO-8859-1 but covered by windows-1252. 
   * Because windows-1252 is a superset of ISO-8859-1 (sharing code points
   * for the common part), it's better to treat ISO-8859-1 as
   * synonymous with windows-1252 than to reject, as invalid, documents
   * labelled as ISO-8859-1 that have characters outside ISO-8859-1.
   */
  static {
    encodingAliases.put("ISO-8859-1", "windows-1252"); 
    encodingAliases.put("EUC-KR", "x-windows-949"); 
    encodingAliases.put("x-EUC-CN", "GB18030"); 
    encodingAliases.put("GBK", "GB18030"); 
 // encodingAliases.put("Big5", "Big5HKSCS"); 
 // encodingAliases.put("TIS620", "Cp874"); 
 // encodingAliases.put("ISO-8859-11", "Cp874"); 
  }
 
  public static String resolveEncodingAlias(String encoding) {
    if (!Charset.isSupported(encoding))
      return null;
    String canonicalName = new String(Charset.forName(encoding).name());
    return encodingAliases.containsKey(canonicalName) ? 
           (String) encodingAliases.get(canonicalName) : canonicalName; 
  }
 
  public static String stringToView(String str) {
    if (str == null || str.length() < 100) 
      return str; 
    return str.substring(0,100) + "...(" + str.length() + " size)"; 
  }
 
  /**
   * replace substrings within string.
   */
  public static String replace(String s, String sub, String with) {
    int c = 0;
    int i = s.indexOf(sub,c);
    if (i == -1)
      return s;
    
    StringBuilder buf = new StringBuilder(s.length()+with.length());
 
    synchronized(buf) {
      do {
        buf.append(s.substring(c,i));
        buf.append(with);
        c = i+sub.length();
      } while ((i=s.indexOf(sub,c))!=-1);
            
      if (c<s.length())
        buf.append(s.substring(c,s.length()));
            
      return buf.toString();
    }
  }
 
  public static String trim(String str) {
    if (str != null && str.length() > 0) {
      int pos1 = 0;
	  int pos2 = str.length();
		 
	  while (pos1 < str.length()) {
		char chr = str.charAt(pos1++);
		if (chr == ' ' || chr == '\"' || chr == '\t' || chr == '\r' || chr == '\n') 
		  continue;
		pos1 --;
		break;
	  }
		 
	  while (pos2 > 0) {
		char chr = str.charAt(--pos2);
		if (chr == ' ' || chr == '\"' || chr == '\t' || chr == '\r' || chr == '\n')
		  continue;
		pos2 ++;
		break;
	  }
		 
	  if (pos1 < 0 || pos2 < 0)
		str = "";
	  else if (pos1 > pos2)
		str = "";
	  else if ((pos1 != 0 || pos2 != str.length()))
		str = str.substring(pos1, pos2);
	}

	return str;
  }
  
  public static String trimChars(String str, String chars) {
    if (str != null && str.length() > 0 && chars != null && chars.length() > 0) {
      int pos1 = 0;
	  int pos2 = str.length();
		 
	  while (pos1 < str.length()) {
		char chr = str.charAt(pos1++);
		if (chars.indexOf(chr) >= 0) 
		  continue;
		pos1 --;
		break;
	  }
		 
	  while (pos2 > 0) {
		char chr = str.charAt(--pos2);
		if (chars.indexOf(chr) >= 0)
		  continue;
		pos2 ++;
		break;
	  }
		 
	  if (pos1 < 0 || pos2 < 0)
		str = "";
	  else if (pos1 > pos2)
		str = "";
	  else if ((pos1 != 0 || pos2 != str.length()))
		str = str.substring(pos1, pos2);
	}

	return str;
  }
  
  public static String[] split(String str, char sep) {
	if (str == null || str.length() == 0) return null;
	
	ArrayList<String> list = new ArrayList<String>();
	
	int pos1 = 0;
	int pos2 = str.indexOf(sep, pos1);
	
	while (pos2 > pos1 && pos1 >= 0) {
	  String token = str.substring(pos1, pos2);
	  if (token == null) token = "";
	  list.add(token);
	  
	  pos1 = pos2 + 1;
	  pos2 = str.indexOf(sep, pos1);
	}
	
	if (pos1 >= 0 && pos1 < str.length() && pos2 < 0)
		list.add(str.substring(pos1));
	
	return list.toArray(new String[list.size()]);
  }
  
  public static String[] splitToken(String str, String delim) {
	if (str == null || str.length() == 0) return null;
	if (delim == null || delim.length() == 0)
	  return new String[]{ str };
	  
	StringTokenizer st = new StringTokenizer(str, delim);
	ArrayList<String> list = new ArrayList<String>();
	  
	while (st.hasMoreTokens()) {
	  String token = st.nextToken();
	  if (token != null) list.add(token);
	}
	  
	return list.toArray(new String[list.size()]);
  }
  
  public static String join(String[] strs, String sep) {
	if (strs == null || strs.length == 0) 
	  return null;
	  
	StringBuilder sbuf = new StringBuilder();
	for (String str : strs) {
	  if (str == null) continue;
	  if (sbuf.length() > 0 && sep != null) 
		  sbuf.append(sep);
	  sbuf.append(str);
	}
	  
	return sbuf.toString();
  }
  
  public static String replaceChar(String str, char oldChr, char newChr) {
	if (str == null || str.length() == 0) return str;
	  
	StringBuilder sbuf = new StringBuilder(str.length());
	  
	for (int i=0; i < str.length(); i++) {
	  char chr = str.charAt(i);
	  sbuf.append(chr == oldChr ? newChr : chr);
	}
	  
	return sbuf.toString();
  }
  
}