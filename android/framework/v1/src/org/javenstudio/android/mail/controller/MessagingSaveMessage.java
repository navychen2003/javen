package org.javenstudio.android.mail.controller;

import java.util.ArrayList;
import java.util.Date;

import org.javenstudio.mail.Address;
import org.javenstudio.mail.Flag;
import org.javenstudio.mail.Message;
import org.javenstudio.mail.MessagingException;
import org.javenstudio.mail.Part;
import org.javenstudio.mail.internet.MimeMessage;
import org.javenstudio.mail.internet.MimeUtility;

import org.javenstudio.android.mail.content.AccountContent;
import org.javenstudio.android.mail.content.BodyContent;
import org.javenstudio.android.mail.content.MailContentProvider;
import org.javenstudio.android.mail.content.MailboxContent;
import org.javenstudio.android.mail.content.MessageContent;
import org.javenstudio.common.util.Logger;

public class MessagingSaveMessage {
	private static Logger LOG = Logger.getLogger(MessagingSaveMessage.class);

	public static void copyOneMessageToProvider(final MessagingController controller, 
			Message message, AccountContent account, MailboxContent folder, int loadStatus) 
    		throws MessagingException {
    	final MailContentProvider provider = MailContentProvider.getInstance(); 
    	if (message == null) return; 
    	
		MessageContent localMessage = provider.queryMessageWithUid(account.getId(), folder.getId(), message.getUid()); 
		if (localMessage == null) {
			if (LOG.isDebugEnabled()) { 
				LOG.debug("copyOneMessageToProvider: " + 
						"Could not retrieve message from db, UUID=" + message.getUid());
			}
            
            throw new MessagingException("Could not retrieve message with UUID: "+message.getUid()); 
        }
    	
		BodyContent body = provider.queryBodyWithMessageId(localMessage.getId()); 
		if (body == null) { 
			body = provider.newBodyContent(localMessage.getId()); 
			body.setMessageKey(localMessage.getId()); 
		} else 
			body = body.startUpdate(); 
		
		// Now process body parts & attachments
        ArrayList<Part> viewables = new ArrayList<Part>();
        ArrayList<Part> attachments = new ArrayList<Part>();
        MimeUtility.collectParts(message, viewables, attachments);
        
        // Copy the fields that are available into the message object
        MessageContent updateMessage = localMessage.startUpdate(); 
        
        // process (and save) attachments and body
        int bodySize = controller.updateBodyFields(localMessage, updateMessage, body, viewables); 
        long attachmentSize = controller.updateAttachments(localMessage, updateMessage, attachments); 
        
        // One last update of message with two updated flags
        updateMessage.setFlagLoaded(loadStatus);
        updateMessage.setFlagAttachment(attachmentSize > 0);
        //updateMessage.setFlags(flags);
        updateMessage.setSize(attachmentSize + bodySize);
        
     	controller.updateMessageFields(updateMessage, message); 
    }
	
	public static void updateMessageFields(final MessagingController controller, 
			MessageContent localMessage, Message message) throws MessagingException { 
    	
    	if (message != null) { 
    		Address[] from = message.getFrom();
            Address[] to = message.getRecipients(Message.RecipientType.TO);
            Address[] cc = message.getRecipients(Message.RecipientType.CC);
            Address[] bcc = message.getRecipients(Message.RecipientType.BCC);
            Address[] replyTo = message.getReplyTo();
            String subject = message.getSubject();
            Date sentDate = message.getSentDate();
            Date internalDate = message.getInternalDate();
            
            if (from != null && from.length > 0) {
                localMessage.setDisplayName(from[0].toFriendly());
            }
            if (sentDate != null) {
                localMessage.setTimeStamp(sentDate.getTime());
            }
            if (subject != null) {
                localMessage.setSubject(subject);
            }
            
            // Keep the message in the "unloaded" state until it has (at least) a display name.
            // This prevents early flickering of empty messages in POP download.
            if (localMessage.getFlagLoaded() != MessageContent.FLAG_LOADED_COMPLETE) {
                if (localMessage.getDisplayName() == null || "".equals(localMessage.getDisplayName())) 
                    localMessage.setFlagLoaded(MessageContent.FLAG_LOADED_UNLOADED);
                else 
                    localMessage.setFlagLoaded(MessageContent.FLAG_LOADED_PARTIAL);
            }
            
            localMessage.setFlagRead(message.isSet(Flag.SEEN));
            localMessage.setFlagFavorite(message.isSet(Flag.FLAGGED));
            localMessage.setFlagAnswered(message.isSet(Flag.ANSWERED));

            localMessage.setServerId(message.getUid());
            if (internalDate != null) {
                localMessage.setServerTimeStamp(internalDate.getTime());
            }

            // Only replace the local message-id if a new one was found.  This is seen in some ISP's
            // which may deliver messages w/o a message-id header.
            String messageId = ((MimeMessage)message).getMessageId();
            if (messageId != null) {
                localMessage.setMessageId(messageId);
            }

            if (from != null && from.length > 0) {
                localMessage.setFrom(Address.pack(from));
            }

            localMessage.setTo(Address.pack(to));
            localMessage.setCc(Address.pack(cc));
            localMessage.setBcc(Address.pack(bcc));
            localMessage.setReplyTo(Address.pack(replyTo));
            
            controller.setOtherMessageFields(localMessage, message); 
    	}
    	
    	// Commit the message to the local store
        localMessage.commitUpdates();
    }
	
}
