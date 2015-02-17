package org.javenstudio.mail;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

@SuppressWarnings({"unused"})
public class Utility {
    public static final Charset UTF_8 = Charset.forName("UTF-8");

    // Added to replace Mailbox.TYPEs so I could have multiple or single args
    // in buildMailboxIdSelection()
    private static final String IN_ALL_MAILBOXES = "IN (0,1)";
    private static final String IN_INBOX = "IN (0)";
    private static final String IN_DRAFTS = "IN (3)";
    private static final String IN_OUTBOX = "IN (4)";

    public final static String readInputStream(InputStream in, String encoding) throws IOException {
        InputStreamReader reader = new InputStreamReader(in, encoding);
        StringBuffer sb = new StringBuffer();
        int count;
        char[] buf = new char[512];
        while ((count = reader.read(buf)) != -1) {
            sb.append(buf, 0, count);
        }
        return sb.toString();
    }

    public final static boolean arrayContains(Object[] a, Object o) {
        for (int i = 0, count = a.length; i < count; i++) {
            if (a[i].equals(o)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Combines the given array of Objects into a single string using the
     * seperator character and each Object's toString() method. between each
     * part.
     *
     * @param parts
     * @param seperator
     * @return
     */
    public static String combine(Object[] parts, char seperator) {
        if (parts == null) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < parts.length; i++) {
            sb.append(parts[i].toString());
            if (i < parts.length - 1) {
                sb.append(seperator);
            }
        }
        return sb.toString();
    }

    /**
     * Ensures that the given string starts and ends with the double quote character. The string is not modified in any way except to add the
     * double quote character to start and end if it's not already there.
     *
     * TODO: Rename this, because "quoteString()" can mean so many different things.
     *
     * sample -> "sample"
     * "sample" -> "sample"
     * ""sample"" -> "sample"
     * "sample"" -> "sample"
     * sa"mp"le -> "sa"mp"le"
     * "sa"mp"le" -> "sa"mp"le"
     * (empty string) -> ""
     * " -> ""
     * @param s
     * @return
     */
    public static String quoteString(String s) {
        if (s == null) {
            return null;
        }
        if (!s.matches("^\".*\"$")) {
            return "\"" + s + "\"";
        }
        else {
            return s;
        }
    }

    /**
     * Apply quoting rules per IMAP RFC,
     * quoted          = DQUOTE *QUOTED-CHAR DQUOTE
     * QUOTED-CHAR     = <any TEXT-CHAR except quoted-specials> / "\" quoted-specials
     * quoted-specials = DQUOTE / "\"
     *
     * This is used primarily for IMAP login, but might be useful elsewhere.
     *
     * NOTE:  Not very efficient - you may wish to preflight this, or perhaps it should check
     * for trouble chars before calling the replace functions.
     *
     * @param s The string to be quoted.
     * @return A copy of the string, having undergone quoting as described above
     */
    public static String imapQuoted(String s) {

        // First, quote any backslashes by replacing \ with \\
        // regex Pattern:  \\    (Java string const = \\\\)
        // Substitute:     \\\\  (Java string const = \\\\\\\\)
        String result = s.replaceAll("\\\\", "\\\\\\\\");

        // Then, quote any double-quotes by replacing " with \"
        // regex Pattern:  "    (Java string const = \")
        // Substitute:     \\"  (Java string const = \\\\\")
        result = result.replaceAll("\"", "\\\\\"");

        // return string with quotes around it
        return "\"" + result + "\"";
    }

    /**
     * A fast version of  URLDecoder.decode() that works only with UTF-8 and does only two
     * allocations. This version is around 3x as fast as the standard one and I'm using it
     * hundreds of times in places that slow down the UI, so it helps.
     */
    public static String fastUrlDecode(String s) {
        try {
            byte[] bytes = s.getBytes("UTF-8");
            byte ch;
            int length = 0;
            for (int i = 0, count = bytes.length; i < count; i++) {
                ch = bytes[i];
                if (ch == '%') {
                    int h = (bytes[i + 1] - '0');
                    int l = (bytes[i + 2] - '0');
                    if (h > 9) {
                        h -= 7;
                    }
                    if (l > 9) {
                        l -= 7;
                    }
                    bytes[length] = (byte) ((h << 4) | l);
                    i += 2;
                }
                else if (ch == '+') {
                    bytes[length] = ' ';
                }
                else {
                    bytes[length] = bytes[i];
                }
                length++;
            }
            return new String(bytes, 0, length, "UTF-8");
        }
        catch (UnsupportedEncodingException uee) {
            return null;
        }
    }

    /**
     * Returns true if the specified date is within today. Returns false otherwise.
     * @param date
     * @return
     */
    @SuppressWarnings("deprecation")
	public static boolean isDateToday(Date date) {
        // TODO But Calendar is so slowwwwwww....
        Date today = new Date();
        if (date.getYear() == today.getYear() &&
                date.getMonth() == today.getMonth() &&
                date.getDate() == today.getDate()) {
            return true;
        }
        return false;
    }

    /**
     * Generate a random message-id header for locally-generated messages.
     */
    public static String generateMessageId() {
        StringBuffer sb = new StringBuffer();
        sb.append("<");
        for (int i = 0; i < 24; i++) {
            sb.append(Integer.toString((int)(Math.random() * 35), 36));
        }
        sb.append(".");
        sb.append(Long.toString(System.currentTimeMillis()));
        sb.append("@email.android.com>");
        return sb.toString();
    }

    /**
     * Generate a time in milliseconds from a date string that represents a date/time in GMT
     * @param DateTime date string in format 20090211T180303Z (rfc2445, iCalendar).
     * @return the time in milliseconds (since Jan 1, 1970)
     */
    public static long parseDateTimeToMillis(String date) {
        GregorianCalendar cal = parseDateTimeToCalendar(date);
        return cal.getTimeInMillis();
    }

    /**
     * Generate a GregorianCalendar from a date string that represents a date/time in GMT
     * @param DateTime date string in format 20090211T180303Z (rfc2445, iCalendar).
     * @return the GregorianCalendar
     */
    public static GregorianCalendar parseDateTimeToCalendar(String date) {
        GregorianCalendar cal = new GregorianCalendar(Integer.parseInt(date.substring(0, 4)),
                Integer.parseInt(date.substring(4, 6)) - 1, Integer.parseInt(date.substring(6, 8)),
                Integer.parseInt(date.substring(9, 11)), Integer.parseInt(date.substring(11, 13)),
                Integer.parseInt(date.substring(13, 15)));
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        return cal;
    }

    /**
     * Generate a time in milliseconds from an email date string that represents a date/time in GMT
     * @param Email style DateTime string in format 2010-02-23T16:00:00.000Z (ISO 8601, rfc3339)
     * @return the time in milliseconds (since Jan 1, 1970)
     */
    public static long parseEmailDateTimeToMillis(String date) {
        GregorianCalendar cal = new GregorianCalendar(Integer.parseInt(date.substring(0, 4)),
                Integer.parseInt(date.substring(5, 7)) - 1, Integer.parseInt(date.substring(8, 10)),
                Integer.parseInt(date.substring(11, 13)), Integer.parseInt(date.substring(14, 16)),
                Integer.parseInt(date.substring(17, 19)));
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        return cal.getTimeInMillis();
    }

    /** Converts a String to UTF-8 */
    public static byte[] toUtf8(String s) {
        if (s == null) {
            return null;
        }
        final ByteBuffer buffer = UTF_8.encode(CharBuffer.wrap(s));
        final byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        return bytes;
    }

    /** Build a String from UTF-8 bytes */
    public static String fromUtf8(byte[] b) {
        if (b == null) {
            return null;
        }
        final CharBuffer cb = Utility.UTF_8.decode(ByteBuffer.wrap(b));
        return new String(cb.array(), 0, cb.length());
    }

    /**
     * @return true if the input is the first (or only) byte in a UTF-8 character
     */
    public static boolean isFirstUtf8Byte(byte b) {
        // If the top 2 bits is '10', it's not a first byte.
        return (b & 0xc0) != 0x80;
    }

    public static String byteToHex(int b) {
        return byteToHex(new StringBuilder(), b).toString();
    }

    public static StringBuilder byteToHex(StringBuilder sb, int b) {
        b &= 0xFF;
        sb.append("0123456789ABCDEF".charAt(b >> 4));
        sb.append("0123456789ABCDEF".charAt(b & 0xF));
        return sb;
    }

    public static String replaceBareLfWithCrlf(String str) {
        return str.replace("\r", "").replace("\n", "\r\n");
    }

    /**
     * @return a non-negative integer generated from 20 byte SHA-1 hash.
     */
    /* package for testing */ static int getSmallHashFromSha1(byte[] sha1) {
        final int offset = sha1[19] & 0xf; // SHA1 is 20 bytes.
        return ((sha1[offset]  & 0x7f) << 24)
                | ((sha1[offset + 1] & 0xff) << 16)
                | ((sha1[offset + 2] & 0xff) << 8)
                | ((sha1[offset + 3] & 0xff));
    }
}