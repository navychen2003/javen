package org.javenstudio.android.mail.controller;

import java.util.Date;

import org.javenstudio.mail.Address;
import org.javenstudio.mail.Flag;
import org.javenstudio.mail.Message;
import org.javenstudio.mail.MessagingException;
import org.javenstudio.mail.internet.MimeBodyPart;
import org.javenstudio.mail.internet.MimeHeader;
import org.javenstudio.mail.internet.MimeMessage;
import org.javenstudio.mail.internet.MimeMultipart;
import org.javenstudio.mail.internet.TextBody;

import org.javenstudio.android.mail.content.BodyContent;
import org.javenstudio.android.mail.content.MailContentProvider;
import org.javenstudio.android.mail.content.MessageContent;

public class MessagingMakeMessage {

    /**
     * Values for HEADER_ANDROID_BODY_QUOTED_PART to tag body parts
     */
    /* package */ static final String BODY_QUOTED_PART_REPLY = "quoted-reply";
    /* package */ static final String BODY_QUOTED_PART_FORWARD = "quoted-forward";
    /* package */ static final String BODY_QUOTED_PART_INTRO = "quoted-intro";
	
	/**
     * Read a complete Provider message into a legacy message (for IMAP upload).  This
     * is basically the equivalent of LocalFolder.getMessages() + LocalFolder.fetch().
     */
    public static Message makeMessage(final MessagingController controller, 
    		MessageContent localMessage) throws MessagingException {
    	final MailContentProvider provider = MailContentProvider.getInstance(); 
    	
    	MimeMessage message = new MimeMessage();
    	if (localMessage == null) 
    		return message;

        // LocalFolder.getMessages() equivalent:  Copy message fields
    	String subject = localMessage.getSubject();
        message.setSubject(subject == null ? "" : subject);
        Address[] from = Address.unpack(localMessage.getFrom());
        if (from.length > 0) 
            message.setFrom(from[0]);
        
        message.setSentDate(new Date(localMessage.getTimeStamp()));
        message.setUid(localMessage.getServerId());
        message.setFlag(Flag.DELETED,
                localMessage.getFlagLoaded() == MessageContent.FLAG_LOADED_DELETED);
        message.setFlag(Flag.SEEN, localMessage.getFlagRead());
        message.setFlag(Flag.FLAGGED, localMessage.getFlagFavorite());
//      message.setFlag(Flag.DRAFT, localMessage.mMailboxKey == draftMailboxKey);
        message.setRecipients(Message.RecipientType.TO, Address.unpack(localMessage.getTo()));
        message.setRecipients(Message.RecipientType.CC, Address.unpack(localMessage.getCc()));
        message.setRecipients(Message.RecipientType.BCC, Address.unpack(localMessage.getBcc()));
        message.setReplyTo(Address.unpack(localMessage.getReplyTo()));
        message.setInternalDate(new Date(localMessage.getServerTimeStamp()));
        message.setMessageId(localMessage.getMessageId());

        // LocalFolder.fetch() equivalent: build body parts
        message.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "multipart/mixed");
        MimeMultipart mp = new MimeMultipart();
        mp.setSubType("mixed");
        message.setBody(mp);

        boolean isReply = (localMessage.getFlags() & MessageContent.FLAG_TYPE_REPLY) != 0;
        boolean isForward = (localMessage.getFlags() & MessageContent.FLAG_TYPE_FORWARD) != 0;
        
        BodyContent localBody = provider.queryBodyWithMessageId(localMessage.getId());
        if (localBody != null) { 
        	addTextBodyPart(mp, "text/html", null, localBody.getHtmlContent());
        	addTextBodyPart(mp, "text/plain", null, localBody.getTextContent());
        	
        	// If there is a quoted part (forwarding or reply), add the intro first, and then the
            // rest of it.  If it is opened in some other viewer, it will (hopefully) be displayed in
            // the same order as we've just set up the blocks:  composed text, intro, replied text
            if (isReply || isForward) {
            	addTextBodyPart(mp, "text/plain", BODY_QUOTED_PART_INTRO, localBody.getIntroText());
            	
            	String replyTag = isReply ? BODY_QUOTED_PART_REPLY : BODY_QUOTED_PART_FORWARD;
            	addTextBodyPart(mp, "text/html", replyTag, localBody.getHtmlReply());
            	addTextBodyPart(mp, "text/plain", replyTag, localBody.getTextReply());
            }
        }

        // Attachments
        // TODO: Make sure we deal with these as structures and don't accidentally upload files

        return message;
    }
	
    /**
     * Helper method to add a body part for a given type of text, if found
     *
     * @param mp The text body part will be added to this multipart
     * @param contentType The content-type of the text being added
     * @param quotedPartTag If non-null, HEADER_ANDROID_BODY_QUOTED_PART will be set to this value
     * @param partText The text to add.  If null, nothing happens
     */
    protected static void addTextBodyPart(MimeMultipart mp, 
    		String contentType, String quotedPartTag, String partText) 
    		throws MessagingException {
        if (partText == null || partText.length() == 0) 
            return;
        
        TextBody body = new TextBody(partText);
        MimeBodyPart bp = new MimeBodyPart(body, contentType);
        if (quotedPartTag != null) 
            bp.addHeader(MimeHeader.HEADER_ANDROID_BODY_QUOTED_PART, quotedPartTag);
        
        mp.addBodyPart(bp);
    }
    
}
