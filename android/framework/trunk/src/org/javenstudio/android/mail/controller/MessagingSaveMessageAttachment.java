package org.javenstudio.android.mail.controller;

import org.javenstudio.mail.Body;
import org.javenstudio.mail.MessagingException;
import org.javenstudio.mail.Part;
import org.javenstudio.mail.internet.MimeHeader;
import org.javenstudio.mail.internet.MimeUtility;

import org.javenstudio.android.mail.content.AttachmentContent;
import org.javenstudio.android.mail.content.AttachmentFile;
import org.javenstudio.android.mail.content.MailContentProvider;
import org.javenstudio.android.mail.content.MessageContent;

public class MessagingSaveMessageAttachment {

	public static long addOneAttachment(final MessagingController controller, 
			MessageContent localMessage, Part part) throws MessagingException {
    	final MailContentProvider provider = MailContentProvider.getInstance(); 
    	
    	AttachmentContent localAttachment = provider.newAttachmentContent(localMessage.getId()); 
    	localAttachment.setAccountKey(localMessage.getAccountKey()); 
    	localAttachment.setMessageKey(localMessage.getId()); 
    	
    	// Transfer fields from mime format to provider format
        String contentType = MimeUtility.unfoldAndDecode(part.getContentType());
        String name = MimeUtility.getHeaderParameter(contentType, "name");
        if (name == null) {
            String contentDisposition = MimeUtility.unfoldAndDecode(part.getDisposition());
            name = MimeUtility.getHeaderParameter(contentDisposition, "filename");
        }
        
        // Find size, if available, via a number of techniques:
        long size = 0;
        String contentUri = AttachmentContent.CONTENTURI_NULL; 
        
        // Incoming attachment: Try to pull size from disposition (if not downloaded yet)
        String disposition = part.getDisposition();
        if (disposition != null) {
            String s = MimeUtility.getHeaderParameter(disposition, "size");
            if (s != null && s.length() > 0) 
                size = Long.parseLong(s);
        }
    	
        String[] transferEncodings = part.getHeader(MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING); 
        String transferEncoding = transferEncodings != null ? transferEncodings[0] : null; 
        
        // Get partId for unloaded IMAP attachments (if any)
        // This is only provided (and used) when we have structure but not the actual attachment
        String[] partIds = part.getHeader(MimeHeader.HEADER_ANDROID_ATTACHMENT_STORE_DATA);
        String partId = partIds != null ? partIds[0] : null;
        
        // Save the body part of a single attachment, to a file in the attachments directory.
        Body body = part.getBody(); 
        if (body != null) {
        	AttachmentFile file = provider.saveAttachmentFile(localMessage, body); 
        	if (file == null) { 
        		throw new MessagingException("attachment: "+name+" cannot save for message: "+localMessage.getId()); 
        	}
        	
        	contentUri = file.getContentUri(); 
        	size = file.getContentLength(); 
        }
        
        localAttachment.setFileName(name); 
        localAttachment.setMimeType(part.getMimeType()); 
        localAttachment.setSize(size); 			// May be reset below if file handled
        localAttachment.setContentUri(contentUri); 
        localAttachment.setContentId(part.getContentId()); 
        localAttachment.setContentType(part.getContentType()); 
        localAttachment.setContentTransferEncoding(transferEncoding); 
        localAttachment.setLocation(partId); 
        localAttachment.setEncoding("B"); 		// TODO - convert other known encodings
        
        controller.setOtherAttachmentFields(localAttachment, part); 
        
        // Save the attachment (so far) in order to obtain an id
        localAttachment.commitUpdates(); 
        
    	return size; 
    }
	
}
