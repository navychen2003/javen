package org.javenstudio.android.mail.controller;

import java.util.HashMap;
import java.util.Map;

import org.javenstudio.mail.MessagingException;
import org.javenstudio.mail.store.Store;

import org.javenstudio.android.mail.content.AccountContent;
import org.javenstudio.android.mail.content.MailContentProvider;
import org.javenstudio.android.mail.content.MailboxContent;
import org.javenstudio.android.mail.content.MessageContent;
import org.javenstudio.common.util.Logger;

public class MessagingProcessUpdates {
	private static Logger LOG = Logger.getLogger(MessagingProcessUpdates.class);

	public static void processPendingUpdates(final MessagingController controller, 
			final AccountContent account, final CallbackMessagingListener listener) throws MessagingException {
    	final MailContentProvider provider = MailContentProvider.getInstance(); 
    	
    	if (LOG.isDebugEnabled()) { 
    		LOG.debug("**** processPendingUpdates:" + 
    				" account="+account.getId()); 
    	}
    	
    	MessageActionQueue.AccountMessages accountMessages = 
    			controller.getMessageActionQueue().getAccountMessages(account.getId()); 
    	if (accountMessages == null || accountMessages.getMessageCount() <= 0) 
    		return; 
    	
    	MessageActionQueue.MessageActions[] actionMessages = accountMessages.removeMessages(); 
    	Map<Long, MailboxContent> mailboxMap = new HashMap<Long, MailboxContent>(); 
    	
    	// Defer setting up the store until we know we need to access it
        Store remoteStore = null;
        // Demand load mailbox (note order-by to reduce thrashing here)
        MailboxContent mailbox = null;
        
        listener.setTotalCount(actionMessages != null ? actionMessages.length : 0);
        listener.setFinishedCount(0);
        
    	// loop through messages marked as needing updates
    	for (int i=0; actionMessages != null && i < actionMessages.length; i++) { 
    		MessageActionQueue.MessageActions actionMessage = actionMessages[i]; 
    		if (actionMessage == null) 
    			continue; 
    		
    		boolean changeMoveToTrash = false;
    		boolean changeMoveFolder = false;
            boolean changeRead = false;
            boolean changeFlagged = false;
            boolean changeAnswered = false;
            
            MessageContent message = provider.queryMessage(actionMessage.getMessageId()); 
            if (message == null) 
            	continue; 
            
            long mailboxKey = actionMessage.updateMailboxKey() ? 
            		actionMessage.getMailboxKey() : message.getMailboxKey(); 
            if (mailbox == null || mailbox.getId() != mailboxKey) { 
            	mailbox = mailboxMap.get(mailboxKey); 
            	if (mailbox == null) { 
            		mailbox = provider.queryMailbox(mailboxKey); 
            		if (mailbox != null) 
            			mailboxMap.put(mailboxKey, mailbox); 
            	}
            }
            
            changeMoveToTrash = actionMessage.updateMailboxKey() && 
            		(mailbox != null) && (mailbox.getId() == mailboxKey) && 
            		(message.getMailboxKey() != mailbox.getId()) && 
                    (mailbox.getType() == MailboxContent.TYPE_TRASH);
            
            changeMoveFolder = !changeMoveToTrash && actionMessage.updateMailboxKey() &&
            		(mailbox != null) && (mailbox.getId() == mailboxKey) && 
            		(message.getMailboxKey() != mailbox.getId());
            
            changeRead = actionMessage.updateFlagRead() && 
            		message.getFlagRead() != actionMessage.getFlagRead();
            
            changeFlagged = actionMessage.updateFlagFavorite() && 
            		message.getFlagFavorite() != actionMessage.getFlagFavorite();
            
            changeAnswered = actionMessage.updateFlagAnswered() && 
            		message.getFlagAnswered() != actionMessage.getFlagAnswered();
            
            // Load the remote store if it will be needed
            if (remoteStore == null && (changeMoveToTrash || changeMoveFolder || changeRead || changeFlagged || changeAnswered)) 
                remoteStore = Store.getInstance(account.getStoreHostAuth().toUri()); 

            // Dispatch here for specific change types
            if (changeMoveToTrash) {
                // Move message to trash
                controller.processPendingMoveToTrash(remoteStore, account, mailbox, message, actionMessage, 
                        listener);
                
            } else if (changeMoveFolder) { 
            	controller.processPendingMoveFolder(remoteStore, account, mailbox, message, actionMessage, 
                        listener);
            	
            } else if (changeRead || changeFlagged || changeAnswered) {
                controller.processPendingFlagChange(remoteStore, account, mailbox, message, actionMessage, 
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
	
}
