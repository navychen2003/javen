package org.javenstudio.android.mail.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.mail.FetchProfile;
import org.javenstudio.mail.Flag;
import org.javenstudio.mail.Folder;
import org.javenstudio.mail.Message;
import org.javenstudio.mail.MessagingException;
import org.javenstudio.mail.store.Store;

import org.javenstudio.android.mail.content.AccountContent;
import org.javenstudio.android.mail.content.MailContentProvider;
import org.javenstudio.android.mail.content.MailboxContent;
import org.javenstudio.android.mail.content.MessageContent;
import org.javenstudio.common.util.EntityIterable;
import org.javenstudio.common.util.Logger;

public class MessagingProcessUploads {
	private static Logger LOG = Logger.getLogger(MessagingProcessUploads.class);

	public static void processPendingUploads(final MessagingController controller, 
			final AccountContent account, final CallbackMessagingListener listener) throws MessagingException {
		final MailContentProvider provider = MailContentProvider.getInstance(); 
    	
    	if (LOG.isDebugEnabled()) { 
    		LOG.debug("**** processPendingUploads:" + 
    				" account="+account.getId()); 
    	}
		
    	final MailboxContent sentbox = provider.queryMailboxWithType(account.getId(), MailboxContent.TYPE_SENT);
    	if (sentbox == null) { 
    		if (LOG.isDebugEnabled()) { 
        		LOG.debug("processPendingUploads:" + 
        				" account: "+account.getId()+" has no sent mailbox"); 
        	}
    		return;
    	}
		
    	if (!controller.checkUploadMessagesToRemote(account, sentbox)) { 
    		if (LOG.isDebugEnabled()) { 
        		LOG.debug("processPendingUploads:" + 
        				" account: "+account.getId()+" mailbox: "+sentbox.getId()+" disable upload"); 
        	}
    		return;
    	}
    	
    	Map<Long, MessageContent> messages = new HashMap<Long, MessageContent>();
    	
    	EntityIterable<MessageContent> messageIt = provider.queryMessagesNoUid(
        		account.getId(), sentbox.getId()); 
        try { 
	        while (messageIt.hasNext()) { 
	        	MessageContent message = messageIt.next(); 
	        	if (message != null && message.getAccountKey() == account.getId() && 
	        			message.getMailboxKey() == sentbox.getId()) { 
	        		String serverId = message.getServerId();
	        		if (serverId == null || serverId.length() == 0)
	        			messages.put(message.getId(), message);
	        	}
	        }
        } finally { 
        	messageIt.close(); 
        }
        
        if (messages.size() <= 0) 
        	return;
    	
        Store remoteStore = null; 
        
        listener.setTotalCount(messages.size());
        listener.setFinishedCount(0); 
        
        for (MessageContent message : messages.values()) { 
        	if (message == null) continue; 
        	
        	if (LOG.isDebugEnabled()) { 
    			LOG.debug("processPendingUploads:" + 
    					" account: " + account.getId() + " upload message: " + message.getId()); 
    		}
        	
        	final long messageId = message.getId();
        	listener.callbackUploadMessageStarted(message.getId()); 
        	
        	try { 
        		if (remoteStore == null) 
        			remoteStore = Store.getInstance(account.getStoreHostAuth().toUri());
        		
        		if (controller.checkUploadMessageToRemote(remoteStore, account, sentbox, message))
        			controller.processPendingAppendInternal(remoteStore, account, sentbox, message);
        		
        		listener.callbackUploadMessageFinished(messageId); 
        		listener.addFinishedCount(1);
        		
        	} catch (MessagingException ex) { 
        		listener.callbackUploadMessageFailed(messageId, MessagingEvent.RESULT_EXCEPTION, ex); 
        		throw ex; 
        	}
        }
    }
	
	public static boolean processPendingAppendInternal(final MessagingController controller, 
			final Store remoteStore, final AccountContent account, final MailboxContent mailbox, 
			final MessageContent message) throws MessagingException {
		if (controller == null || remoteStore == null || account == null || mailbox == null || message == null) 
			return false;
		
		String serverId = message.getServerId();
		MessageContent messageUpdate = message.startUpdate();
		
		boolean updateInternalDate = false;
        boolean updateMessage = false;
        boolean deleteMessage = false;

        // 1. Find the remote folder that we're appending to and create and/or open it
        Folder remoteFolder = remoteStore.getFolder(mailbox.getDisplayName());
        if (!remoteFolder.exists()) {
            if (!remoteFolder.canCreate(Folder.FolderType.HOLDS_MESSAGES)) {
                // This is POP3, we cannot actually upload.  Instead, we'll update the message
                // locally with a fake serverId (so we don't keep trying here) and return.
            	
                if (serverId == null || serverId.length() == 0) {
                    messageUpdate.setServerId(MessagingController.LOCAL_SERVERID_PREFIX + message.getId());
                    messageUpdate.commitUpdates();
                }
                return true;
            }
            if (!remoteFolder.create(Folder.FolderType.HOLDS_MESSAGES)) {
                // This is a (hopefully) transient error and we return false to try again later
                return false;
            }
        }
        remoteFolder.open(Folder.OpenMode.READ_WRITE, null);
        if (remoteFolder.getMode() != Folder.OpenMode.READ_WRITE) 
            return false;
        
        // 2. If possible, load a remote message with the matching UID
        Message remoteMessage = null;
        if (serverId != null && serverId.length() > 0) 
            remoteMessage = remoteFolder.getMessage(serverId);
        
        // 3. If a remote message could not be found, upload our local message
        if (remoteMessage == null) {
            // 3a. Create a legacy message to upload
            Message localMessage = controller.makeMessage(message);

            // 3b. Upload it
            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.BODY);
            remoteFolder.appendMessages(new Message[] { localMessage });

            // 3b. And record the UID from the server
            serverId = localMessage.getUid();
            messageUpdate.setServerId(serverId);
            
            updateInternalDate = true;
            updateMessage = true;
            
        } else {
            // 4. If the remote message exists we need to determine which copy to keep.
            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);
            remoteFolder.fetch(new Message[] { remoteMessage }, fp, null);
            
            Date localDate = new Date(message.getServerTimeStamp());
            Date remoteDate = remoteMessage.getInternalDate();
            
            if (remoteDate.compareTo(localDate) > 0) {
                // 4a. If the remote message is newer than ours we'll just
                // delete ours and move on. A sync will get the server message
                // if we need to be able to see it.
                deleteMessage = true;
                
            } else {
                // 4b. Otherwise we'll upload our message and then delete the remote message.

                // Create a legacy message to upload
                Message localMessage = controller.makeMessage(message);

                // 4c. Upload it
                fp.clear();
                fp = new FetchProfile();
                fp.add(FetchProfile.Item.BODY);
                remoteFolder.appendMessages(new Message[] { localMessage });

                // 4d. Record the UID and new internalDate from the server
                serverId = localMessage.getUid();
                messageUpdate.setServerId(serverId);
                
                updateInternalDate = true;
                updateMessage = true;

                // 4e. And delete the old copy of the message from the server
                remoteMessage.setFlag(Flag.DELETED, true);
            }
        }
        
        // 5. If requested, Best-effort to capture new "internaldate" from the server
        if (updateInternalDate && serverId != null && serverId.length() > 0) {
            try {
                Message remoteMessage2 = remoteFolder.getMessage(serverId);
                if (remoteMessage2 != null) {
                    FetchProfile fp2 = new FetchProfile();
                    fp2.add(FetchProfile.Item.ENVELOPE);
                    remoteFolder.fetch(new Message[] { remoteMessage2 }, fp2, null);
                    messageUpdate.setServerTimeStamp(remoteMessage2.getInternalDate().getTime());
                    updateMessage = true;
                }
            } catch (MessagingException me) {
                // skip it - we can live without this
            }
        }
        
        // 6. Perform required edits to local copy of message
        if (deleteMessage || updateMessage) {
            if (deleteMessage) {
                messageUpdate.commitDelete();
            } else if (updateMessage) {
                messageUpdate.commitUpdates();
            }
            return true;
        }
        
		return false;
    }
	
}
