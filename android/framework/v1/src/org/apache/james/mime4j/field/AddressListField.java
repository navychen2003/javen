package org.apache.james.mime4j.field;

import org.apache.james.mime4j.field.address.AddressList;
import org.apache.james.mime4j.field.address.parser.ParseException;
import org.apache.james.mime4j.util.Logger;

public class AddressListField extends Field {
    private AddressList addressList;
    private ParseException parseException;

    protected AddressListField(String name, String body, String raw, AddressList addressList, ParseException parseException) {
        super(name, body, raw);
        this.addressList = addressList;
        this.parseException = parseException;
    }

    public AddressList getAddressList() {
        return addressList;
    }

    public ParseException getParseException() {
        return parseException;
    }

    public static class Parser implements FieldParser {
    	private static Logger log = Logger.getLogger(Parser.class);

        public Field parse(final String name, final String body, final String raw) {
            AddressList addressList = null;
            ParseException parseException = null;
            try {
                addressList = AddressList.parse(body);
            }
            catch (ParseException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Parsing value '" + body + "': "+ e.getMessage());
                }
                parseException = e;
            }
            return new AddressListField(name, body, raw, addressList, parseException);
        }
    }
}