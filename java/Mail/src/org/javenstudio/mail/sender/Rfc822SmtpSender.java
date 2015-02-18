package org.javenstudio.mail.sender;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.javenstudio.common.util.Logger;
import org.javenstudio.mail.Address;
import org.javenstudio.mail.MessagingException;
import org.javenstudio.mail.content.MessageAttachmentData;
import org.javenstudio.mail.content.MessageBodyData;
import org.javenstudio.mail.content.MessageData;
import org.javenstudio.mail.internet.MimeUtility;
import org.javenstudio.mime.Base64;
import org.javenstudio.mime.Base64OutputStream;
import org.javenstudio.mail.util.IOUtils;

/**
 * Sender class to output RFC 822 messages from provider email messages
 */
public class Rfc822SmtpSender extends SmtpSender {
	private static Logger LOG = Logger.getLogger(Rfc822SmtpSender.class); 

	/**
     * Must implements static named constructor.
     */
    public static Sender newInstance(String uri) throws MessagingException {
    	try { 
    		return new Rfc822SmtpSender(uri); 
    	} catch (MessagingException e) { 
    		LOG.error("Rfc822SmtpSender.newInstance(\""+uri+"\") error", e); 
    		throw e; 
    	}
    }
	
    /**
     * Allowed formats for the Uri:
     * smtp://user:password@server:port
     * smtp+tls+://user:password@server:port
     * smtp+tls+trustallcerts://user:password@server:port
     * smtp+ssl+://user:password@server:port
     * smtp+ssl+trustallcerts://user:password@server:port
     *
     * @param uriString the Uri containing information to configure this sender
     */
    protected Rfc822SmtpSender(String uriString) throws MessagingException {
    	super(uriString); 
    }
    
    private static final Pattern PATTERN_START_OF_LINE = Pattern.compile("(?m)^");
    private static final Pattern PATTERN_ENDLINE_CRLF = Pattern.compile("\r\n");

    // In MIME, en_US-like date format should be used. In other words "MMM" should be encoded to
    // "Jan", not the other localized format like "Ene" (meaning January in locale es).
    static final SimpleDateFormat mDateFormat =
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
    
    /**
     * Write the entire message to an output stream.  This method provides buffering, so it is
     * not necessary to pass in a buffered output stream here.
     *
     * @param context system context for accessing the provider
     * @param messageId the message to write out
     * @param out the output stream to write the message to
     * @param appendQuotedText whether or not to append quoted text if this is a reply/forward
     *
     * TODO alternative parts (e.g. text+html) are not supported here.
     */
    @Override 
    protected void writeSmtpData(OutputStream out, MessageData message, 
            boolean appendQuotedText, boolean sendBcc) throws IOException, MessagingException { 
        if (message == null) {
            // throw something?
            return;
        }
    	
        OutputStream stream = new BufferedOutputStream(out, 1024);
        Writer writer = new OutputStreamWriter(stream);
        
        writeMimeHeader(writer, message, sendBcc); 
    	
        // Analyze message and determine if we have multiparts
        String text = buildBodyText(message, appendQuotedText);
        
        MessageAttachmentData[] atts = message.getMessageAttachments(); 
        
        int attachmentCount = atts != null ? atts.length : 0;
        boolean multipart = attachmentCount > 0;
        String multipartBoundary = null;
        String multipartType = "mixed";
        
        // Simplified case for no multipart - just emit text and be done.
        if (!multipart) {
            if (text != null) 
                writeTextWithHeaders(writer, stream, text);
            else 
                writer.write("\r\n");       // a truly empty message
            
        } else {
        	// continue with multipart headers, then into multipart body
            multipartBoundary = createMultipartBoundary();
        	
            int currentIdx = 0; 
            MessageAttachmentData currentAtt = atts != null && atts.length > 0 ? atts[currentIdx] : null; 
            if (attachmentCount == 1) {
                // If we've got one attachment and it's an ics "attachment", we want to send
                // this as multipart/alternative instead of multipart/mixed
                //int flags = currentAtt != null ? currentAtt.getFlags() : 0; 
                //if ((flags & AttachmentContent.FLAG_ICS_ALTERNATIVE_PART) != 0) 
            	if (currentAtt != null && currentAtt.isAlternativePart()) 
                    multipartType = "alternative";
            }
            
            writeHeader(writer, "Content-Type",
                    "multipart/" + multipartType + "; boundary=\"" + multipartBoundary + "\"");
            // Finish headers and prepare for body section(s)
            writer.write("\r\n");

            // first multipart element is the body
            if (text != null) {
                writeBoundary(writer, multipartBoundary, false);
                writeTextWithHeaders(writer, stream, text);
            }
            
            // Write out the attachments until we run out
            for (; atts != null && currentIdx < atts.length; currentIdx ++) { 
            	currentAtt = atts[currentIdx]; 
            	if (currentAtt == null) continue; 
            	
            	writeBoundary(writer, multipartBoundary, false);
            	writeOneAttachment(writer, stream, currentAtt);
                writer.write("\r\n");
            }
            
            // end of multipart section
            writeBoundary(writer, multipartBoundary, true);
        }
        
        writer.flush();
        out.flush();
    }
    
    protected void writeMimeHeader(Writer writer, MessageData message, boolean sendBcc) throws IOException { 
    	// Write the fixed headers.  Ordering is arbitrary (the legacy code iterated through a
        // hashmap here).

        String date = mDateFormat.format(new Date(message.getTimeStamp()));
        writeHeader(writer, "Date", date);

        writeEncodedHeader(writer, "Subject", message.getSubject());

        writeHeader(writer, "Message-ID", message.getMessageId());

        writeAddressHeader(writer, "From", message.getFrom());
        writeAddressHeader(writer, "To", message.getTo());
        writeAddressHeader(writer, "Cc", message.getCc());
        // Address fields.  Note that we skip bcc unless the sendBcc argument is true
        // SMTP should NOT send bcc headers, but EAS must send it!
        if (sendBcc) {
            writeAddressHeader(writer, "Bcc", message.getBcc());
        }
        writeAddressHeader(writer, "Reply-To", message.getReplyTo());
        writeHeader(writer, "MIME-Version", "1.0");
    }
    
    protected String createMultipartBoundary() { 
    	return "--_android.email_" + System.nanoTime();
    }
    
    protected static String buildBodyText(MessageData message, boolean appendQuotedText) {
        MessageBodyData body = message.getMessageBody();
        if (body == null) 
            return null;

        StringBuilder text = new StringBuilder();
        
        String content = body.getTextContent();
        if (content == null || content.length() == 0)
        	content = body.getHtmlContent();
        
        if (content != null)
        	text.append(content);
        
        //int flags = message.getFlags();
        boolean isReply = message.isReplyMessage(); //(flags & MessageContent.FLAG_TYPE_REPLY) != 0;
        boolean isForward = message.isForwardMessage(); //(flags & MessageContent.FLAG_TYPE_FORWARD) != 0;
        
        String intro = body.getIntroText();
        if (intro == null) 
        	intro = ""; 
        
        if (!appendQuotedText) {
            // appendQuotedText is set to false for use by SmartReply/SmartForward in EAS.
            // SmartReply doesn't appear to work properly, so we will still add the header into
            // the original message.
            // SmartForward doesn't put any kind of break between the original and the new text,
            // so we add a CRLF
            if (isReply) {
                text.append(intro);
            } else if (isForward) {
                text.append("\r\n");
            }
            
            return text.toString();
        }

        String quotedText = body.getTextReply();
        if (quotedText != null) {
            // fix CR-LF line endings to LF-only needed by EditText.
            Matcher matcher = PATTERN_ENDLINE_CRLF.matcher(quotedText);
            quotedText = matcher.replaceAll("\n");
        }
        
        if (isReply) {
            text.append(intro);
            if (quotedText != null) {
                Matcher matcher = PATTERN_START_OF_LINE.matcher(quotedText);
                text.append(matcher.replaceAll(">"));
            }
        } else if (isForward) {
            text.append(intro);
            if (quotedText != null) 
                text.append(quotedText);
        }
        
        return text.toString();
    }
    
    /**
     * Write a single attachment and its payload
     */
    protected void writeOneAttachment(Writer writer, OutputStream out, MessageAttachmentData attachment) 
    		throws IOException, MessagingException {
    	writeHeader(writer, "Content-Type",
                attachment.getMimeType() + ";\n name=\"" + attachment.getFileName() + "\"");
        writeHeader(writer, "Content-Transfer-Encoding", "base64");
        // Most attachments (real files) will send Content-Disposition.  The suppression option
        // is used when sending calendar invites.
        //if ((attachment.getFlags() & AttachmentContent.FLAG_ICS_ALTERNATIVE_PART) == 0) {
        if (attachment.isAlternativePart()) { 
            writeHeader(writer, "Content-Disposition",
                    "attachment;"
                    + "\n filename=\"" + attachment.getFileName() + "\";"
                    + "\n size=" + Long.toString(attachment.getSize()));
        }
        writeHeader(writer, "Content-ID", attachment.getContentId());
        writer.append("\r\n");
        
        // Set up input stream and write it out via base64
        InputStream inStream = null;
        try {
        	inStream = openAttachmentFile(attachment); 
        	if (inStream != null) { 
	            // switch to output stream for base64 text output
	            writer.flush();
	            Base64OutputStream base64Out = new Base64OutputStream(
	                out, Base64.CRLF | Base64.NO_CLOSE);
	            // copy base64 data and close up
	            IOUtils.copy(inStream, base64Out);
	            base64Out.close();
        	}

            // The old Base64OutputStream wrote an extra CRLF after
            // the output.  It's not required by the base-64 spec; not
            // sure if it's required by RFC 822 or not.
            out.write('\r');
            out.write('\n');
            out.flush();
            
        } catch (IOException ioe) {
            throw new MessagingException("Invalid attachment.", ioe);
            
        } finally { 
        	try { 
        		if (inStream != null) 
        			inStream.close(); 
        	} catch (IOException ex) { 
        		// ignore
        	}
        }
    }
    
    protected InputStream openAttachmentFile(MessageAttachmentData attachment) throws MessagingException { 
    	try {
    		// Use content, if provided; otherwise, use the contentUri
    		byte[] contentBytes = attachment.getContentBytes(); 
            if (contentBytes != null) {
                return new ByteArrayInputStream(contentBytes);
                
            } else {
                // try to open the file
                //Uri fileUri = Uri.parse(attachment.getContentUri());
                //return context.getContentResolver().openInputStream(fileUri);
            	//return Preferences.getPreferences().openInputStream(attachment.getContentUri()); 
            	
            	//AttachmentFile attachmentFile = MailContentProvider.getInstance().openAttachmentFile(attachment);
            	//if (attachmentFile != null) { 
            	//	StorageFile file = attachmentFile.getFile();
            	//	if (file != null) 
            	//		return file.openFile();
            	//}
            	
            	return attachment.openInputStream();
            }
    	//} catch (FileNotFoundException fnfe) {
        //    // Ignore this - empty file is OK
        } catch (IOException ex) { 
        	throw new MessagingException("Open attachment file error", ex); 
        }
    }
    
    /**
     * Write a single header with no wrapping or encoding
     *
     * @param writer the output writer
     * @param name the header name
     * @param value the header value
     */
    protected void writeHeader(Writer writer, String name, String value) throws IOException {
        if (value != null && value.length() > 0) {
            writer.append(name);
            writer.append(": ");
            writer.append(value);
            writer.append("\r\n");
        }
    }

    /**
     * Write a single header using appropriate folding & encoding
     *
     * @param writer the output writer
     * @param name the header name
     * @param value the header value
     */
    protected void writeEncodedHeader(Writer writer, String name, String value)
            throws IOException {
        if (value != null && value.length() > 0) {
            writer.append(name);
            writer.append(": ");
            writer.append(MimeUtility.foldAndEncode2(value, name.length() + 2));
            writer.append("\r\n");
        }
    }

    /**
     * Unpack, encode, and fold address(es) into a header
     *
     * @param writer the output writer
     * @param name the header name
     * @param value the header value (a packed list of addresses)
     */
    protected void writeAddressHeader(Writer writer, String name, String value)
            throws IOException {
        if (value != null && value.length() > 0) {
            writer.append(name);
            writer.append(": ");
            writer.append(MimeUtility.fold(Address.packedToHeader(value), name.length() + 2));
            writer.append("\r\n");
        }
    }

    /**
     * Write a multipart boundary
     *
     * @param writer the output writer
     * @param boundary the boundary string
     * @param end false if inner boundary, true if final boundary
     */
    protected void writeBoundary(Writer writer, String boundary, boolean end)
            throws IOException {
        writer.append("--");
        writer.append(boundary);
        if (end) {
            writer.append("--");
        }
        writer.append("\r\n");
    }

    /**
     * Write text (either as main body or inside a multipart), preceded by appropriate headers.
     *
     * Note this always uses base64, even when not required.  Slightly less efficient for
     * US-ASCII text, but handles all formats even when non-ascii chars are involved.  A small
     * optimization might be to prescan the string for safety and send raw if possible.
     *
     * @param writer the output writer
     * @param out the output stream inside the writer (used for byte[] access)
     * @param text The original text of the message
     */
    protected void writeTextWithHeaders(Writer writer, OutputStream out, String text)
            throws IOException {
        writeHeader(writer, "Content-Type", "text/plain; charset=utf-8");
        writeHeader(writer, "Content-Transfer-Encoding", "base64");
        writer.write("\r\n");
        byte[] bytes = text.getBytes("UTF-8");
        writer.flush();
        out.write(Base64.encode(bytes, Base64.CRLF));
    }
    
}
