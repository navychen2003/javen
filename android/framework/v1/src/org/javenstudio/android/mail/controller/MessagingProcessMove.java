package org.javenstudio.android.mail.controller;

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

public class MessagingProcessMove {
	private static Logger LOG = Logger.getLogger(MessagingProcessMove.class);

	public static void processPendingMoveFolder(final MessagingController controller, 
			final Store remoteStore, final AccountContent account, final MailboxContent newMailbox, final MessageContent message,
            final MessageActionQueue.MessageActions actionMessage, boolean deleteAction, 
            final CallbackMessagingListener listener) throws MessagingException {
    	final MailContentProvider provider = MailContentProvider.getInstance(); 
    	
    	if (message.getId() != actionMessage.getMessageId()) 
    		return; 
    	
    	// 0. No remote update if the message is local-only
    	String serverId = message.getServerId(); 
        if (serverId == null || serverId.length() == 0 || serverId.startsWith(MessagingController.LOCAL_SERVERID_PREFIX))
            return;
    	
        // 1. Escape early if we can't find the local mailbox
        // TODO smaller projection here
        final MailboxContent mailbox = provider.queryMailbox(message.getMailboxKey()); 
        if (mailbox == null) 
        	// can't find old mailbox, it may have been deleted.  just return.
        	return; 
        
        final long oldMailboxId = mailbox.getId(); 
        final long newMailboxId = newMailbox.getId(); 
        
        if (oldMailboxId == newMailboxId) 
        	return; 
    	
        if (deleteAction) { 
        	// 2. We don't support delete-from-trash here
            if (mailbox.getType() == MailboxContent.TYPE_TRASH || 
            	newMailbox.getType() != MailboxContent.TYPE_TRASH) 
                return;
            
            // 3. If DELETE_POLICY_NEVER, simply write back the deleted sentinel and return
            //
            // This sentinel takes the place of the server-side message, and locally "deletes" it
            // by inhibiting future sync or display of the message.  It will eventually go out of
            // scope when it becomes old, or is deleted on the server, and the regular sync code
            // will clean it up for us.
            if (controller.checkDeletePolicyNeverWhenMoveMessageToTrash(account, message)) { 
            	if (LOG.isDebugEnabled()) { 
            		LOG.debug("processPendingMoveToTrash:" + 
            			" DELETE_POLICY_NEVER for message id=" + message.getId() + 
            			" simply write back the deleted sentinel"); 
            	}
            	
            	MessageContent updateMessage = message.startUpdate(); 
            	updateMessage.setFlagLoaded(MessageContent.FLAG_LOADED_DELETED); 
            	updateMessage.setFlagRead(true); 
            	updateMessage.commitUpdates(); 
            	return; 
            }
            
            // The rest of this method handles server-side deletion
        }
        
        // 4.  Find the remote mailbox (that we deleted from), and open it
        Folder remoteFolder = remoteStore.getFolder(mailbox.getDisplayName());
        if (!remoteFolder.exists()) 
            return;

        remoteFolder.open(Folder.OpenMode.READ_WRITE, null);
        if (remoteFolder.getMode() != Folder.OpenMode.READ_WRITE) {
            //remoteFolder.close(false); // close outside
            return;
        }
        
        // 5. Find the remote original message
        Message remoteMessage = remoteFolder.getMessage(serverId);
        if (remoteMessage == null) {
            //remoteFolder.close(false); // close outside
            return;
        }
        
        // 6. Find the remote trash folder, and create it if not found
        Folder remoteNewFolder = remoteStore.getFolder(newMailbox.getDisplayName());
        if (!remoteNewFolder.exists()) {
            /*
             * If the remote trash folder doesn't exist we try to create it.
             */
        	remoteNewFolder.create(Folder.FolderType.HOLDS_MESSAGES);
        }
        
        // 7.  Try to copy the message into the remote trash folder
        // Note, this entire section will be skipped for POP3 because there's no remote trash
        if (remoteNewFolder.exists()) {
            /*
             * Because remoteTrashFolder may be new, we need to explicitly open it
             */
        	remoteNewFolder.open(Folder.OpenMode.READ_WRITE, null);
            if (remoteNewFolder.getMode() != Folder.OpenMode.READ_WRITE) {
                //remoteFolder.close(false); // close outside
                //remoteNewFolder.close(false); // close outside
                return;
            }
            
            remoteFolder.copyMessages(new Message[] { remoteMessage }, remoteNewFolder,
		                    new Folder.MessageUpdateCallbacks() {
		                public void onMessageUidChange(Message remoteMessage, String newUid) {
		                    // update the UID in the local trash folder, because some stores will
		                    // have to change it when copying to remoteTrashFolder
		                	if (newUid != null && newUid.length() > 0) { 
			                    MessageContent updateMessage = message.startUpdate(); 
			                    updateMessage.setServerId(newUid); 
			                    updateMessage.commitUpdates(); 
		                	}
		                }
		                
		                /**
		                 * This will be called if the deleted message doesn't exist and can't be
		                 * deleted (e.g. it was already deleted from the server.)  In this case,
		                 * attempt to delete the local copy as well.
		                 */
		                public void onMessageNotFound(Message remoteMessage) {
		                	try { 
		                		// delete message
		                		provider.deleteMessage(account.getId(), message.getId()); 
		                	} catch (Exception e) { 
		                		LOG.error("delete remote not-existed message "+message.getId()+" error", e); 
		                	}
		                }
		            }
	            );
            
            //remoteNewFolder.close(false); // close outside
        }
        
        // 8. Delete the message from the remote source folder
        remoteMessage.setFlag(Flag.DELETED, true);
        remoteFolder.expunge();
        //remoteFolder.close(false); // close outside
        
        // 9. Update the local message content
        MessageContent updateMessage = message.startUpdate(); 
        updateMessage.setMailboxKey(newMailboxId); 
        updateMessage.setMailboxType(newMailbox.getType());
        if (deleteAction) 
        	updateMessage.setFlagLoaded(MessageContent.FLAG_LOADED_DELETED); 
    	updateMessage.setFlagRead(true); 
    	updateMessage.commitUpdates(); 
    }
	
	/**
     * Process a pending trash message command.
     *
     * @param remoteStore the remote store we're working in
     * @param account The account in which we are working
     * @param newMailbox The local trash mailbox
     * @param oldMessage The message copy that was saved in the updates shadow table
     * @param newMessage The message that was moved to the mailbox
     */
    protected static void processPendingMoveToTrashDeprecated(final MessagingController controller, 
    		final Store remoteStore, final AccountContent account, final MailboxContent newMailbox, final MessageContent message,
            final MessageActionQueue.MessageActions actionMessage, 
            final CallbackMessagingListener listener) throws MessagingException {
    	final MailContentProvider provider = MailContentProvider.getInstance(); 
    	
    	if (message.getId() != actionMessage.getMessageId()) 
    		return; 
    	
    	// 0. No remote update if the message is local-only
    	String serverId = message.getServerId(); 
        if (serverId == null || serverId.length() == 0 || serverId.startsWith(MessagingController.LOCAL_SERVERID_PREFIX))
            return;
    	
        // 1. Escape early if we can't find the local mailbox
        // TODO smaller projection here
        final MailboxContent mailbox = provider.queryMailbox(message.getMailboxKey()); 
        if (mailbox == null) 
        	// can't find old mailbox, it may have been deleted.  just return.
        	return; 
        
        final long oldMailboxId = mailbox.getId(); 
        final long newMailboxId = newMailbox.getId(); 
        
        if (oldMailboxId == newMailboxId) 
        	return; 
        
        // 2. We don't support delete-from-trash here
        if (mailbox.getType() == MailboxContent.TYPE_TRASH || newMailbox.getType() != MailboxContent.TYPE_TRASH) 
            return;
        
        // 3. If DELETE_POLICY_NEVER, simply write back the deleted sentinel and return
        //
        // This sentinel takes the place of the server-side message, and locally "deletes" it
        // by inhibiting future sync or display of the message.  It will eventually go out of
        // scope when it becomes old, or is deleted on the server, and the regular sync code
        // will clean it up for us.
        if (controller.checkDeletePolicyNeverWhenMoveMessageToTrash(account, message)) { 
        	if (LOG.isDebugEnabled()) { 
        		LOG.debug("processPendingMoveToTrash:" + 
        			" DELETE_POLICY_NEVER for message id=" + message.getId() + 
        			" simply write back the deleted sentinel"); 
        	}
        	
        	MessageContent updateMessage = message.startUpdate(); 
        	updateMessage.setFlagLoaded(MessageContent.FLAG_LOADED_DELETED); 
        	updateMessage.setFlagRead(true); 
        	updateMessage.commitUpdates(); 
        	return; 
        }
        
        // The rest of this method handles server-side deletion

        // 4.  Find the remote mailbox (that we deleted from), and open it
        Folder remoteFolder = remoteStore.getFolder(mailbox.getDisplayName());
        if (!remoteFolder.exists()) 
            return;

        remoteFolder.open(Folder.OpenMode.READ_WRITE, null);
        if (remoteFolder.getMode() != Folder.OpenMode.READ_WRITE) {
            //remoteFolder.close(false); // close outside
            return;
        }
        
        // 5. Find the remote original message
        Message remoteMessage = remoteFolder.getMessage(serverId);
        if (remoteMessage == null) {
            //remoteFolder.close(false); // close outside
            return;
        }
        
        // 6. Find the remote trash folder, and create it if not found
        Folder remoteTrashFolder = remoteStore.getFolder(newMailbox.getDisplayName());
        if (!remoteTrashFolder.exists()) {
            /*
             * If the remote trash folder doesn't exist we try to create it.
             */
            remoteTrashFolder.create(Folder.FolderType.HOLDS_MESSAGES);
        }
        
        if (LOG.isDebugEnabled()) { 
    		LOG.debug("processPendingMoveToTrash:" + 
    			" move message to trash for message id=" + message.getId() + " serverId=" + serverId + 
    			" oldMailboxId=" + oldMailboxId + " newMailboxId=" + newMailboxId); 
    	}
        
        try { 
        	listener.callbackMoveMessageToTrashStarted(
	        		message.getId(), oldMailboxId, newMailboxId); 
        	
	        // 7.  Try to copy the message into the remote trash folder
	        // Note, this entire section will be skipped for POP3 because there's no remote trash
	        if (remoteTrashFolder.exists()) {
	            /*
	             * Because remoteTrashFolder may be new, we need to explicitly open it
	             */
	            remoteTrashFolder.open(Folder.OpenMode.READ_WRITE, null);
	            if (remoteTrashFolder.getMode() != Folder.OpenMode.READ_WRITE) {
	                //remoteFolder.close(false); // close outside
	                //remoteTrashFolder.close(false); // close outside
	                return;
	            }
	            
	            remoteFolder.copyMessages(new Message[] { remoteMessage }, remoteTrashFolder,
			                    new Folder.MessageUpdateCallbacks() {
			                public void onMessageUidChange(Message remoteMessage, String newUid) {
			                    // update the UID in the local trash folder, because some stores will
			                    // have to change it when copying to remoteTrashFolder
			                	if (newUid != null && newUid.length() > 0) { 
				                    MessageContent updateMessage = message.startUpdate(); 
				                    updateMessage.setServerId(newUid); 
				                    updateMessage.commitUpdates(); 
			                	}
			                }
			                
			                /**
			                 * This will be called if the deleted message doesn't exist and can't be
			                 * deleted (e.g. it was already deleted from the server.)  In this case,
			                 * attempt to delete the local copy as well.
			                 */
			                public void onMessageNotFound(Message remoteMessage) {
			                	try { 
			                		// delete message
			                		provider.deleteMessage(account.getId(), message.getId()); 
			                	} catch (Exception e) { 
			                		LOG.error("delete message "+message.getId()+" error", e); 
			                	}
			                }
			            }
		            );
	            
	            //remoteTrashFolder.close(false); // close outside
	        }
	        
	        // 8. Delete the message from the remote source folder
	        remoteMessage.setFlag(Flag.DELETED, true);
	        remoteFolder.expunge();
	        //remoteFolder.close(false); // close outside
	        
	        // 9. Update the local message content
	        MessageContent updateMessage = message.startUpdate(); 
	        updateMessage.setMailboxKey(newMailboxId); 
        	updateMessage.setFlagLoaded(MessageContent.FLAG_LOADED_DELETED); 
        	updateMessage.setFlagRead(true); 
        	updateMessage.commitUpdates(); 
	        
        } catch (MessagingException ex) { 
        	listener.callbackMoveMessageToTrashFailed(
	        		message.getId(), oldMailboxId, newMailboxId, ex); 
        	
        	throw ex; 
        	
        } finally { 
        	listener.callbackMoveMessageToTrashFinished(
	        		message.getId(), oldMailboxId, newMailboxId); 
        }
    }
	
}
