package com.beetstra.jutf7;

/**
 * <p>
 * The character set specified in RFC 3501 to use for IMAP4rev1 mailbox name
 * encoding.
 * </p>
 * 
 * @see <a href="http://tools.ietf.org/html/rfc3501">RFC 3501< /a>
 * @author Jaap Beetstra
 */
class ModifiedUTF7Charset extends UTF7StyleCharset {
    private static final String MODIFIED_BASE64_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "abcdefghijklmnopqrstuvwxyz" + "0123456789+,";

    ModifiedUTF7Charset(String name, String[] aliases) {
        super(name, aliases, MODIFIED_BASE64_ALPHABET, true);
    }

    boolean canEncodeDirectly(char ch) {
        if (ch == shift())
            return false;
        return ch >= 0x20 && ch <= 0x7E;
    }

    byte shift() {
        return '&';
    }

    byte unshift() {
        return '-';
    }
}