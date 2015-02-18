package org.apache.james.mime4j.field;

import java.util.HashMap;
import java.util.Map;

public class DelegatingFieldParser implements FieldParser {
    
    private Map<String, FieldParser> parsers = new HashMap<String, FieldParser>();
    private FieldParser defaultParser = new UnstructuredField.Parser();
    
    /**
     * Sets the parser used for the field named <code>name</code>.
     * @param name the name of the field
     * @param parser the parser for fields named <code>name</code>
     */
    public void setFieldParser(final String name, final FieldParser parser) {
        parsers.put(name.toLowerCase(), parser);
    }
    
    public FieldParser getParser(final String name) {
        final FieldParser field = (FieldParser) parsers.get(name.toLowerCase());
        if(field==null) {
            return defaultParser;
        }
        return field;
    }
    
    public Field parse(final String name, final String body, final String raw) {
        final FieldParser parser = getParser(name);
        return parser.parse(name, body, raw);
    }
}