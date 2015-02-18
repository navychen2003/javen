package org.javenstudio.android.mail.controller;

import java.util.HashMap;
import java.util.Map;

import org.javenstudio.mail.Flag;
import org.javenstudio.mail.Folder;
import org.javenstudio.mail.Message;
import org.javenstudio.mail.MessagingException;
import org.javenstudio.mail.store.Store;

import org.javenstudio.android.mail.content.AccountContent;
import org.javenstudio.android.mail.content.MailContentProvider;
import org.javenstudio.android.mail.content.MailboxContent;
import org.javenstudio.android.mail.content.MessageContent;
import org.javenstudio.common.util.Logger;

public class MessagingProcessDeletes {
	private static Logger LOG = Logger.getLogger(MessagingProcessDeletes.class);

	public static void processPendingDeletes(final MessagingController controller, 
			final AccountContent account, final CallbackMessagingListener listener) throws MessagingException {
    	final MailContentProvider provider = MailContentProvider.getInstance(); 
    	
    	if (LOG.isDebugEnabled()) { 
    		LOG.debug("**** processPendingDeletes:" + 
    				" account="+account.getId()); 
    	}
    	
    	MessageActionQueue.AccountMessages accountMessages = 
    			controller.getMessageActionQueue().getAccountMessages(account.getId()); 
    	if (accountMessages == null || accountMessages.getMessageCount() <= 0) 
    		return; 
    	
    	MessageActionQueue.MessageActions[] actionMessages = accountMessages.removeDeleteMessages(); 
    	Map<Long, MailboxContent> mailboxMap = new HashMap<Long, MailboxContent>(); 
    	
    	// Defer setting up the store until we know we need to access it
        Store remoteStore = null;
        // Demand load mailbox (note order-by to reduce thrashing here)
        MailboxContent mailbox = null;
    	
        listener.setTotalCount(actionMessages != null ? actionMessages.length : 0);
        listener.setFinishedCount(0);
        
    	// loop through messages marked as deleted
    	for (int i=0; actionMessages != null && i < actionMessages.length; i++) { 
    		MessageActionQueue.MessageActions actionMessage = actionMessages[i]; 
    		if (actionMessage == null) 
    			continue; 
    		
    		boolean deleteFromTrash = false;
    		boolean deleteFromFolder = false;
    		
    		MessageContent message = provider.queryMessage(actionMessage.getMessageId()); 
            if (message == null) 
            	continue; 
    		
            long mailboxKey = message.getMailboxKey(); 
            if (mailbox == null || mailbox.getId() != mailboxKey) { 
            	mailbox = mailboxMap.get(mailboxKey); 
            	if (mailbox == null) { 
            		mailbox = provider.queryMailbox(mailboxKey); 
            		if (mailbox != null) 
            			mailboxMap.put(mailboxKey, mailbox); 
            	}
            }
            
            if (mailbox != null) { 
	            deleteFromTrash = mailbox.getType() == MailboxContent.TYPE_TRASH;
	            deleteFromFolder = !deleteFromTrash;
            }
            
            // Load the remote store if it will be needed
            if (remoteStore == null && (deleteFromTrash || deleteFromFolder)) 
                remoteStore = Store.getInstance(account.getStoreHostAuth().toUri()); 
            
            // Dispatch here for specific change types
            if (deleteFromTrash) {
                // Move message to trash
                controller.processPendingDeleteFromTrash(remoteStore, account, mailbox, message, 
                		listener);
                
            } else if (deleteFromFolder) { 
            	controller.processPendingDeleteFromFolder(remoteStore, account, mailbox, message, 
                		listener);
            }
            
            listener.addFinishedCount(1);
    	}
    	
    	if (remoteStore != null) 
    		remoteStore.closeFolders(false);
    	
    	// Finally, update mailbox count
    	for (MailboxContent changedMailbox : mailboxMap.values()) { 
    		long mailboxId = changedMailbox.getId(); 
    		int totalCount = provider.countTotalMessages(account.getId(), mailboxId); 
    		
    		MailboxContent updateMailbox = changedMailbox.startUpdate(); 
    		updateMailbox.setNewSyncCount(0); 
    		updateMailbox.setTotalSyncCount(totalCount); 
    		updateMailbox.commitUpdates(); 
    	}
    }
	
	public static void processPendingDeleteFromTrash(final MessagingController controller, 
			final Store remoteStore, final AccountContent account, MailboxContent mailbox, MessageContent message, 
            final CallbackMessagingListener listener) throws MessagingException {
    	final MailContentProvider provider = MailContentProvider.getInstance(); 
    	
    	if (mailbox.getId() != message.getMailboxKey()) 
    		return; 
    	
    	// 1. We only support delete-from-trash here
    	if (!controller.checkDeleteMessageFromTrash(remoteStore, account, mailbox, message)) 
    		return; 
    	
    	// 2.  Find the remote trash folder (that we are deleting from), and open it
        Folder remoteTrashFolder = remoteStore.getFolder(mailbox.getDisplayName());
        if (!remoteTrashFolder.exists()) 
            return;

        remoteTrashFolder.open(Folder.OpenMode.READ_WRITE, null);
        if (remoteTrashFolder.getMode() != Folder.OpenMode.READ_WRITE) {
            //remoteTrashFolder.close(false); // close outside
            return;
        }

        // 3. Find the remote original message
        Message remoteMessage = remoteTrashFolder.getMessage(message.getServerId());
        if (remoteMessage == null) {
            //remoteTrashFolder.close(false); // close outside
            return;
        }

        if (LOG.isDebugEnabled()) { 
    		LOG.debug("processPendingDeleteFromTrash:" + 
    			" delete message from trash for message id=" + message.getId() + 
    			" serverId=" + message.getServerId()); 
    	}
        
        try { 
        	listener.callbackDeleteMessageStarted(message.getId()); 
        	
	        // 4. Delete the message from the remote trash folder
	        remoteMessage.setFlag(Flag.DELETED, true);
	        remoteTrashFolder.expunge();
	        //remoteTrashFolder.close(false); // close outside
    	
	        // 5. Delete the local message from database
	        provider.deleteMessage(account.getId(), message.getId()); 
	        
	        listener.callbackDeleteMessageFinished(message.getId()); 
	        
        } catch (MessagingException ex) { 
        	listener.callbackDeleteMessageFailed(message.getId(), ex); 
        	throw ex; 
        }
    }
	
	public static void processPendingDeleteFromFolder(final MessagingController controller, 
			final Store remoteStore, final AccountContent account, MailboxContent mailbox, MessageContent message, 
            final CallbackMessagingListener listener) throws MessagingException {
    	final MailContentProvider provider = MailContentProvider.getInstance(); 
    	
    	if (mailbox.getId() != message.getMailboxKey()) 
    		return; 
    	
    	// 0. DRAFTS only delete from local-folder
    	if (mailbox.getType() == MailboxContent.TYPE_DRAFTS) { 
    		if (LOG.isDebugEnabled()) { 
        		LOG.debug("processPendingDeleteFromFolder:" + 
        			" delete message from local folder only for message id=" + message.getId()); 
        	}
    		
    		try { 
            	listener.callbackDeleteMessageStarted(message.getId()); 
            	
    	        // Delete the local message from database
    	        provider.deleteMessage(account.getId(), message.getId()); 
    	        
    	        listener.callbackDeleteMessageFinished(message.getId()); 
    	        
            } catch (MessagingException ex) { 
            	listener.callbackDeleteMessageFailed(message.getId(), ex); 
            	throw ex; 
            }
    		
    		return;
    	}
    	
    	// 1. We only support delete-from-folder on POP3
    	if (!controller.checkDeleteMessageFromFolder(remoteStore, account, mailbox, message)) 
    		return;
    	
    	// 2.  Find the remote folder (that we are deleting from), and open it
        Folder remoteFolder = remoteStore.getFolder(mailbox.getDisplayName());
        if (!remoteFolder.exists()) 
            return;

        remoteFolder.open(Folder.OpenMode.READ_WRITE, null);
        if (remoteFolder.getMode() != Folder.OpenMode.READ_WRITE) {
        	//remoteFolder.close(false); // close outside
            return;
        }

        // 3. Find the remote original message
        Message remoteMessage = remoteFolder.getMessage(message.getServerId());
        if (remoteMessage == null) {
        	//remoteFolder.close(false); // close outside
            return;
        }

        if (LOG.isDebugEnabled()) { 
    		LOG.debug("processPendingDeleteFromFolder:" + 
    			" delete message from folder for message id=" + message.getId() + 
    			" serverId=" + message.getServerId()); 
    	}
        
        try { 
        	listener.callbackDeleteMessageStarted(message.getId()); 
        	
	        // 4. Delete the message from the remote trash folder
	        remoteMessage.setFlag(Flag.DELETED, true);
	        remoteFolder.expunge();
	        //remoteFolder.close(false); // close outside
    	
	        // 5. Delete the local message from database
	        provider.deleteMessage(account.getId(), message.getId()); 
	        
	        listener.callbackDeleteMessageFinished(message.getId()); 
	        
        } catch (MessagingException ex) { 
        	listener.callbackDeleteMessageFailed(message.getId(), ex); 
        	throw ex; 
        }
    }
	
}
