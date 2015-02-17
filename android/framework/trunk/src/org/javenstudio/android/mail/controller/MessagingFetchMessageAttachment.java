package org.javenstudio.android.mail.controller;

import org.javenstudio.mail.FetchProfile;
import org.javenstudio.mail.Folder;
import org.javenstudio.mail.Message;
import org.javenstudio.mail.MessagingException;
import org.javenstudio.mail.OutputBody;
import org.javenstudio.mail.store.Store;

import org.javenstudio.android.mail.content.AccountContent;
import org.javenstudio.android.mail.content.AttachmentContent;
import org.javenstudio.android.mail.content.AttachmentFile;
import org.javenstudio.android.mail.content.MailContentProvider;
import org.javenstudio.android.mail.content.MailboxContent;
import org.javenstudio.android.mail.content.MessageContent;
import org.javenstudio.common.util.Logger;

public class MessagingFetchMessageAttachment {
	private static Logger LOG = Logger.getLogger(MessagingFetchMessageAttachment.class);

	public static void fetchMessageAttachment(final MessagingController controller, 
			final AccountContent account, final long messageId, final long attachmentId, 
    		final CallbackMessagingListener listener) throws MessagingException { 
    	final MailContentProvider provider = MailContentProvider.getInstance(); 
		
    	// 1. Resample the message, in case it disappeared or synced while
        // this command was in queue
    	final MessageContent message = provider.queryMessage(messageId); 
    	if (message == null) { 
    		if (LOG.isDebugEnabled()) { 
    			LOG.debug("fetchMessageAttachment:" + 
    					" message: "+messageId+" not found"); 
    		}
    		return; 
    	}
    	
    	final AttachmentContent attachment = provider.queryAttachment(attachmentId); 
    	if (attachment == null) { 
    		if (LOG.isDebugEnabled()) { 
    			LOG.debug("fetchMessageAttachment:" + 
    					" attachment: "+attachmentId+" not found"); 
    		}
    		return; 
    	}
    	
    	listener.callbackFetchMessageAttachmentStarted(messageId, attachmentId, attachment); 
    	
    	//if (message.getFlagLoaded() == MessageContent.FLAG_LOADED_COMPLETE) { 
    	//	listener.callbackFetchMessageAttachmentFinished(
    	//			messageId, attachmentId, MessagingEvent.RESULT_COMPLETE); 
    	//	return; 
    	//}
    	
    	// 2. Open the remote folder.
        // TODO all of these could be narrower projections
        // TODO combine with common code in loadAttachment
    	final MailboxContent mailbox = provider.queryMailbox(message.getMailboxKey()); 
    	if (mailbox == null) { 
    		if (LOG.isDebugEnabled()) { 
    			LOG.debug("fetchMessageAttachment:" + 
    					" mailbox: "+message.getMailboxKey()+" not found"); 
    		}
    		
    		throw new MessagingException("mailbox: "+message.getMailboxKey()+" not found"); 
    	}
	    
    	// 3. Open remote folder
    	Store remoteStore = Store.getInstance(account.getStoreHostAuth().toUri()); 
    	Folder remoteFolder = remoteStore.getFolder(mailbox.getDisplayName()); 
    	remoteFolder.open(Folder.OpenMode.READ_WRITE, null);
    	
    	// 4. Set up to download the entire message attachment
        Message remoteMessage = remoteFolder.getMessage(message.getServerId());
        if (remoteMessage == null) { 
        	if (LOG.isDebugEnabled()) {
    			LOG.debug("fetchMessageAttachment:" + 
    					" remote attachment: "+attachmentId+" serverid: "+message.getServerId()+" not found"); 
        	}
    		
    		listener.callbackFetchMessageAttachmentFailed(
    				messageId, attachmentId, attachment, MessagingEvent.RESULT_NOTFOUND, null); 
    		
    		return; 
        }
    	
    	// 5. Create a temp file to store attachment
    	final BinaryTempFileBody body = new BinaryTempFileBody(new BinaryTempFileBody.OutputBodyListener() {
	    		@Override 
	    	    public boolean isRequestStop() { 
	    			return listener.isRequestStop(); 
	    		}
	    		
				@Override
				public void onWrittenBytes(BinaryTempFileBody bd, long writtenBytes) {
					listener.callbackAttachmentDownloadBytes(message, attachment, 0, writtenBytes); 
				}
				
				@Override
				public void onOutputClosed(BinaryTempFileBody bd) { 
				}
				
				@Override
				public void onOutputFinished(BinaryTempFileBody bd, long countBytes) { 
					try { 
						if (countBytes > 0) { 
							AttachmentFile file = provider.saveAttachmentFile(message, bd); 
				        	if (file == null) { 
				        		throw new MessagingException("attachment: "+attachment.getFileName()+
				        				" cannot save for message: "+message.getId()); 
				        	}
				        	
				        	String oldContentUri = attachment.getContentUri(); 
				        	if (oldContentUri != null) { 
				        		AttachmentFile oldfile = provider.openAttachmentFile(attachment); 
				        		if (oldfile != null) 
				        			oldfile.deleteFile(); 
				        	}
				        	
				        	String contentUri = file.getContentUri(); 
				        	long size = file.getContentLength(); 
				        	
				        	AttachmentContent attachmentUpdate = attachment.startUpdate(); 
				        	attachmentUpdate.setContentUri(contentUri); 
				        	attachmentUpdate.setSize(size); 
				        	attachmentUpdate.commitUpdates(); 
						} else { 
							if (LOG.isDebugEnabled()) { 
				    			LOG.debug("fetchMessageAttachment:" + 
				    					" attachment: "+attachmentId+" download canceled"); 
							}
						}
					} catch (MessagingException ex) { 
						listener.callbackException(ex); 
					}
				}
			}); 
        
    	// 6. Start fetch attachment
        FetchProfile fp = new FetchProfile();
        fp.add(new FetchProfile.Attachment() { 
        		public String getContentId() { return attachment.getContentId(); }
        		public String getContentType() { return attachment.getMimeType(); }
        		public String getContentTransferEncoding() { return attachment.getContentTransferEncoding(); }
        		public String getLocation() { return attachment.getLocation(); }
        		public OutputBody getOutputBody() { return body; }
        	});
        
        remoteFolder.fetch(new Message[] { remoteMessage }, fp, null);
    	
    	listener.callbackFetchMessageAttachmentFinished(messageId, attachmentId, attachment); 
    }
	
}
