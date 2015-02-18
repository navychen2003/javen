package org.apache.james.mime4j.field;

import org.apache.james.mime4j.util.DecodeUtil;

/**
 * Simple unstructured field such as <code>Subject</code>.
 *
 */
public class UnstructuredField extends Field {
    private String value;
    
    protected UnstructuredField(String name, String body, String raw, String value) {
        super(name, body, raw);
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static class Parser implements FieldParser {
        public Field parse(final String name, final String body, final String raw) {
            final String value = DecodeUtil.decodeEncodedWords(body);
            return new UnstructuredField(name, body, raw, value);
        }
    }
}