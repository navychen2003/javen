package org.javenstudio.android.mail.controller;

import java.util.List;

import org.javenstudio.mail.MessagingException;
import org.javenstudio.mail.Part;
import org.javenstudio.mail.internet.MimeHeader;
import org.javenstudio.mail.internet.MimeUtility;

import org.javenstudio.android.mail.content.BodyContent;
import org.javenstudio.android.mail.content.MessageContent;

public class MessagingSaveMessageBody {

	public static int updateBodyFields(final MessagingController controller, 
			MessageContent localMessage, MessageContent updateMessage, 
    		BodyContent body, List<Part> viewables) throws MessagingException {
    	int flags = localMessage.getFlags(); 
    	
    	StringBuilder sbHtml = null;
    	StringBuilder sbText = null;
    	StringBuilder sbHtmlReply = null;
    	StringBuilder sbTextReply = null;
    	StringBuilder sbIntroText = null;

        for (Part viewable : viewables) {
            String text = MimeUtility.getTextFromPart(viewable);
            String[] replyTags = viewable.getHeader(MimeHeader.HEADER_ANDROID_BODY_QUOTED_PART);
            String replyTag = null;
            if (replyTags != null && replyTags.length > 0) {
                replyTag = replyTags[0];
            }
            // Deploy text as marked by the various tags
            boolean isHtml = "text/html".equalsIgnoreCase(viewable.getMimeType());

            if (replyTag != null) {
                boolean isQuotedReply = MailController.BODY_QUOTED_PART_REPLY.equalsIgnoreCase(replyTag);
                boolean isQuotedForward = MailController.BODY_QUOTED_PART_FORWARD.equalsIgnoreCase(replyTag);
                boolean isQuotedIntro = MailController.BODY_QUOTED_PART_INTRO.equalsIgnoreCase(replyTag);

                if (isQuotedReply || isQuotedForward) {
                    if (isHtml) {
                        sbHtmlReply = controller.appendTextPart(sbHtmlReply, text);
                    } else {
                        sbTextReply = controller.appendTextPart(sbTextReply, text);
                    }
                    // Set message flags as well
                    flags &= ~MessageContent.FLAG_TYPE_MASK;
                    flags |= isQuotedReply
                            ? MessageContent.FLAG_TYPE_REPLY
                            : MessageContent.FLAG_TYPE_FORWARD;
                    continue;
                }
                if (isQuotedIntro) {
                    sbIntroText = controller.appendTextPart(sbIntroText, text);
                    continue;
                }
            }

            // Most of the time, just process regular body parts
            if (isHtml) {
                sbHtml = controller.appendTextPart(sbHtml, text);
            } else {
                sbText = controller.appendTextPart(sbText, text);
            }
        }

        int size = 0; 
        
        // write the combined data to the body part
        if (sbText != null && sbText.length() != 0) {
            body.setTextContent(sbText.toString());
            size += sbText.length(); 
        }
        if (sbHtml != null && sbHtml.length() != 0) {
            body.setHtmlContent(sbHtml.toString());
            size += sbHtml.length(); 
        }
        if (sbHtmlReply != null && sbHtmlReply.length() != 0) {
            body.setHtmlReply(sbHtmlReply.toString());
            size += sbHtmlReply.length(); 
        }
        if (sbTextReply != null && sbTextReply.length() != 0) {
            body.setTextReply(sbTextReply.toString());
            size += sbTextReply.length(); 
        }
        if (sbIntroText != null && sbIntroText.length() != 0) {
            body.setIntroText(sbIntroText.toString());
            size += sbIntroText.length(); 
        }
        
        updateMessage.setFlags(flags);
        
        controller.setOtherBodyFields(body, viewables); 
        
        // Commit the message & body to the local store immediately
        body.commitUpdates(); 
        
        return size; 
    }
	
}
