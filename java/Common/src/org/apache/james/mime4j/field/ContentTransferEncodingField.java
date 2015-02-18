package org.apache.james.mime4j.field;

/**
 * Represents a <code>Content-Transfer-Encoding</code> field.
 *
 */
public class ContentTransferEncodingField extends Field {
    /**
     * The <code>7bit</code> encoding.
     */
    public static final String ENC_7BIT = "7bit";
    /**
     * The <code>8bit</code> encoding.
     */
    public static final String ENC_8BIT = "8bit";
    /**
     * The <code>binary</code> encoding.
     */
    public static final String ENC_BINARY = "binary";
    /**
     * The <code>quoted-printable</code> encoding.
     */
    public static final String ENC_QUOTED_PRINTABLE = "quoted-printable";
    /**
     * The <code>base64</code> encoding.
     */
    public static final String ENC_BASE64 = "base64";
    
    private String encoding;
    
    protected ContentTransferEncodingField(String name, String body, String raw, String encoding) {
        super(name, body, raw);
        this.encoding = encoding;
    }

    /**
     * Gets the encoding defined in this field.
     * 
     * @return the encoding or an empty string if not set.
     */
    public String getEncoding() {
        return encoding;
    }
    
    /**
     * Gets the encoding of the given field if. Returns the default 
     * <code>7bit</code> if not set or if
     * <code>f</code> is <code>null</code>.
     * 
     * @return the encoding.
     */
    public static String getEncoding(ContentTransferEncodingField f) {
        if (f != null && f.getEncoding().length() != 0) {
            return f.getEncoding();
        }
        return ENC_7BIT;
    }
    
    public static class Parser implements FieldParser {
        public Field parse(final String name, final String body, final String raw) {
            final String encoding = body.trim().toLowerCase();
            return new ContentTransferEncodingField(name, body, raw, encoding);
        }
    }
}