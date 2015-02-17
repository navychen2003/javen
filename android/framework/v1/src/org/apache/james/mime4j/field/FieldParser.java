package org.apache.james.mime4j.field;

public interface FieldParser {
    
    Field parse(final String name, final String body, final String raw);
    
}