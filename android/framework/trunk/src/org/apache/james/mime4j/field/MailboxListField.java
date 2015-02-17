package org.apache.james.mime4j.field;

import org.apache.james.mime4j.field.address.AddressList;
import org.apache.james.mime4j.field.address.MailboxList;
import org.apache.james.mime4j.field.address.parser.ParseException;
import org.apache.james.mime4j.util.MimeLogger;

public class MailboxListField extends Field {
    
    private MailboxList mailboxList;
    private ParseException parseException;

    protected MailboxListField(final String name, final String body, final String raw, final MailboxList mailboxList, final ParseException parseException) {
        super(name, body, raw);
        this.mailboxList = mailboxList;
        this.parseException = parseException;
    }

    public MailboxList getMailboxList() {
        return mailboxList;
    }

    public ParseException getParseException() {
        return parseException;
    }
    
    public static class Parser implements FieldParser {
    	private static MimeLogger log = MimeLogger.getLogger(Parser.class);

        public Field parse(final String name, final String body, final String raw) {
            MailboxList mailboxList = null;
            ParseException parseException = null;
            try {
                mailboxList = AddressList.parse(body).flatten();
            }
            catch (ParseException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Parsing value '" + body + "': "+ e.getMessage());
                }
                parseException = e;
            }
            return new MailboxListField(name, body, raw, mailboxList, parseException);
        }
    }
}