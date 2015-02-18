package org.javenstudio.android.mail.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.javenstudio.mail.FetchProfile;
import org.javenstudio.mail.Flag;
import org.javenstudio.mail.Folder;
import org.javenstudio.mail.Message;
import org.javenstudio.mail.MessageRetrievalListener;
import org.javenstudio.mail.MessagingException;
import org.javenstudio.mail.Part;
import org.javenstudio.mail.internet.MimeUtility;
import org.javenstudio.mail.store.Store;
import org.javenstudio.mail.store.StoreInfo;
import org.javenstudio.mail.store.StoreSynchronizer;

import org.javenstudio.android.mail.content.AccountContent;
import org.javenstudio.android.mail.content.LocalMessageInfo;
import org.javenstudio.android.mail.content.MailContentProvider;
import org.javenstudio.android.mail.content.MailboxContent;
import org.javenstudio.android.mail.content.MessageContent;
import org.javenstudio.common.util.EntityIterable;
import org.javenstudio.common.util.Logger;

public class MessagingSynchronizeMailbox {
	private static Logger LOG = Logger.getLogger(MessagingSynchronizeMailbox.class);

	public static void synchronizeMailbox(final MessagingController controller, 
			final AccountContent account, final MailboxContent folder, 
    		final CallbackMessagingListener listener) throws MessagingException {
		/*
         * We don't ever sync the Outbox.
         */
        if (folder.getType() == MailboxContent.TYPE_OUTBOX) 
            return;
		
    	listener.callbackSynchronizeMailboxStarted(folder); 

		controller.processPendingActionsSynchronous(account, listener);

        Store remoteStore = Store.getInstance(account.getStoreHostAuth().toUri()); 
        StoreSynchronizer customSync = remoteStore.getMessageSynchronizer();
        
        StoreSynchronizer.SyncResults results;
        if (customSync == null) 
            results = controller.synchronizeMailboxGeneric(account, folder, listener);
        else 
            results = customSync.synchronizeMessagesSynchronous(folder);
        
        if (results != null) { 
        	MailboxContent mailbox = folder.startUpdate(); 
        	
        	mailbox.setNewSyncCount(results.getNewSyncMessages()); 
        	mailbox.setTotalSyncCount(results.getTotalSyncMessages()); 
        	mailbox.setUnreadCount(results.getUnreadMessages()); 
        	mailbox.setTotalCount(results.getTotalMessages()); 
        	mailbox.setSyncTime(System.currentTimeMillis()); 
        	
        	mailbox.commitUpdates(); 
        	
        	listener.callbackSynchronizeMailboxFinished(folder, 
        			results.getTotalMessages(), results.getNewSyncMessages()); 
        	
        } else { 
        	listener.callbackSynchronizeMailboxFinished(folder, 0, 0); 
        }
    }
	
	public static StoreSynchronizer.SyncResults synchronizeMailboxGeneric(final MessagingController controller, 
			final AccountContent account, final MailboxContent folder, 
			final CallbackMessagingListener listener) throws MessagingException {
		final MailContentProvider provider = MailContentProvider.getInstance(); 
		
		if (LOG.isDebugEnabled()) { 
			LOG.debug("**** synchronizeMailboxGeneric:" + 
					" account="+account.getId()+" mailbox="+folder.getId()); 
		}
		
		// 0.  We do not ever sync DRAFTS or OUTBOX (down or up)
		if (folder.getType() == MailboxContent.TYPE_DRAFTS || folder.getType() == MailboxContent.TYPE_OUTBOX) {
			if (LOG.isDebugEnabled()) { 
				LOG.debug("synchronizeMailboxGeneric: Step 0: " + 
						"We do not ever sync DRAFTS or OUTBOX."); 
			}
			
		    int totalMessages = provider.countTotalMessages(account.getId(), folder.getId());
		    return new StoreSynchronizer.SyncResults(totalMessages, 0, totalMessages, 0);
		}
		
		// 1.  Get the message list from the local store and create an index of the uids
		if (LOG.isDebugEnabled()) { 
			LOG.debug("synchronizeMailboxGeneric: Step 1: " + 
					"Get the message list from the local store and create an index of the uids."); 
		}

		HashMap<String, LocalMessageInfo> localMessageMap = new HashMap<String, LocalMessageInfo>();
		
		EntityIterable<MessageContent> messageIt = provider.queryMessages(
				account.getId(), folder.getId()); 
		try { 
		    while (messageIt.hasNext()) { 
		    	MessageContent message = messageIt.next(); 
		    	if (message == null) continue; 
		    	
		    	LocalMessageInfo info = new LocalMessageInfo(message); 
		    	localMessageMap.put(info.getServerId(), info);
		    }
		} finally { 
			messageIt.close(); 
		}

		// 1a. Count the unread messages before changing anything
		if (LOG.isDebugEnabled()) { 
			LOG.debug("synchronizeMailboxGeneric: Step 1a: " + 
					"Count the unread messages before changing anything"); 
		}
		int localUnreadCount = provider.countUnreadMessages(account.getId(), folder.getId()); 
		
		// 2.  Open the remote folder and create the remote folder if necessary
		if (LOG.isDebugEnabled()) { 
			LOG.debug("synchronizeMailboxGeneric: Step 2: " + 
					"Open the remote folder and create the remote folder if necessary."); 
		}
		
		Store remoteStore = Store.getInstance(account.getStoreHostAuth().toUri()); 
		Folder remoteFolder = remoteStore.getFolder(folder.getDisplayName());

		/**
		 * If the folder is a "special" folder we need to see if it exists
		 * on the remote server. It if does not exist we'll try to create it. If we
		 * can't create we'll abort. This will happen on every single Pop3 folder as
		 * designed and on Imap folders during error conditions. This allows us
		 * to treat Pop3 and Imap the same in this code.
		 */
		if (folder.getType() == MailboxContent.TYPE_TRASH || folder.getType() == MailboxContent.TYPE_SENT
		        || folder.getType() == MailboxContent.TYPE_DRAFTS) {
		    if (!remoteFolder.exists()) {
		    	if (LOG.isDebugEnabled()) { 
		    		LOG.debug("synchronizeMailboxGeneric: Step 2: " + 
		    				"The folder is a \"special\" folder we need to see if it exists, if does not exist we'll try to create it, " + 
		    				"folder name is \"" + remoteFolder.getName() + "\""); 
		        }
		        if (!remoteFolder.create(Folder.FolderType.HOLDS_MESSAGES)) {
		        	if (LOG.isDebugEnabled()) { 
		        		LOG.debug("synchronizeMailboxGeneric: Step 2: " + 
		        				"The remote folder is failed to create and just return, folder name is \"" + remoteFolder.getName() + "\""); 
		            }
		            return new StoreSynchronizer.SyncResults(0, 0, 0, 0);
		        }
		    }
		}

		// 3, Open the remote folder. This pre-loads certain metadata like message count.
		if (LOG.isDebugEnabled()) { 
			LOG.debug("synchronizeMailboxGeneric: Step 3: " + 
					"Open the remote folder. This pre-loads certain metadata like message count."); 
		}
		remoteFolder.open(Folder.OpenMode.READ_WRITE, null);
		
		// 4. Trash any remote messages that are marked as trashed locally.
		if (LOG.isDebugEnabled()) { 
			LOG.debug("synchronizeMailboxGeneric: Step 4: " + 
					"Trash any remote messages that are marked as trashed locally."); 
		}
		// TODO - this comment was here, but no code was here.
		
		// 5. Get the remote message count.
		if (LOG.isDebugEnabled()) { 
			LOG.debug("synchronizeMailboxGeneric: Step 5: " + 
					"Get the remote message count."); 
		}
		int remoteMessageCount = remoteFolder.getMessageCount();
		
		// 6. Determine the limit # of messages to download
		if (LOG.isDebugEnabled()) { 
			LOG.debug("synchronizeMailboxGeneric: Step 6: " + 
					"Determine the limit # of messages to download."); 
		}
		int visibleLimit = folder.getVisibleLimit();
		if (visibleLimit <= 0) {
		    StoreInfo info = StoreInfo.getStoreInfo(account.getStoreHostAuth().toUri());
		    visibleLimit = info.getVisibleLimitDefault();
		}
		
		// 7.  Create a list of messages to download
		if (LOG.isDebugEnabled()) { 
			LOG.debug("synchronizeMailboxGeneric: Step 7: " + 
					"Create a list of messages to download."); 
		}
		Message[] remoteMessages = new Message[0];
		final ArrayList<Message> unsyncedMessages = new ArrayList<Message>();
		final HashMap<String, Message> remoteUidMap = new HashMap<String, Message>();

		int newMessageCount = 0;
		if (remoteMessageCount > 0) {
			if (LOG.isDebugEnabled()) { 
				LOG.debug("synchronizeMailboxGeneric: Step 7: " + 
						"Remote store has " + remoteMessageCount + " messages, message numbers start at 1."); 
		    }
			/*
		     * Message numbers start at 1.
		     */
		    int remoteStart = Math.max(0, remoteMessageCount - visibleLimit) + 1;
		    int remoteEnd = remoteMessageCount;
		    remoteMessages = remoteFolder.getMessages(remoteStart, remoteEnd, null);
		    for (Message message : remoteMessages) {
		        remoteUidMap.put(message.getUid(), message);
		    }
		    
		    /**
		     * Get a list of the messages that are in the remote list but not on the
		     * local store, or messages that are in the local store but failed to download
		     * on the last sync. These are the new messages that we will download.
		     * Note, we also skip syncing messages which are flagged as "deleted message" sentinels,
		     * because they are locally deleted and we don't need or want the old message from
		     * the server.
		     * DUSTIN 10-7 do not download any messages marked as read on local
		     */
		    for (Message message : remoteMessages) {
		        LocalMessageInfo localMessage = localMessageMap.get(message.getUid());
		        if (localMessage == null) {
		            newMessageCount ++;
		        }
		        if (localMessage == null || controller.checkUnsyncedMessage(remoteStore, localMessage)) {
		        	unsyncedMessages.add(message);
		        }
		    }
		}

		// 8.  Download basic info about the new/unloaded messages (if any)
		if (LOG.isDebugEnabled()) { 
			LOG.debug("synchronizeMailboxGeneric: Step 8: " + 
					"Download basic info about the new/unloaded messages (if any)."); 
		}
		/*
		 * A list of messages that were downloaded and which did not have the Seen flag set.
		 * This will serve to indicate the true "new" message count that will be reported to
		 * the user via notification.
		 */
		final ArrayList<Message> newMessages = new ArrayList<Message>();

		/*
		 * Fetch the flags and envelope only of the new messages. This is intended to get us
		 * critical data as fast as possible, and then we'll fill in the details.
		 */
		if (unsyncedMessages.size() > 0) {
			if (LOG.isDebugEnabled()) { 
				LOG.debug("synchronizeMailboxGeneric: Step 8: " + 
						"Fetch the flags and envelope only of the new messages, " + unsyncedMessages.size() + " unsynced messages."); 
		    }
			FetchProfile fp = new FetchProfile();
		    fp.add(FetchProfile.Item.FLAGS);
		    fp.add(FetchProfile.Item.ENVELOPE);
			
		    listener.setTotalCount(unsyncedMessages.size());
		    listener.setFinishedCount(0);
		    
		    remoteFolder.fetch(unsyncedMessages.toArray(new Message[0]), fp,
		            new MessageRetrievalListener() {
		                public void messageFinished(Message message, int number, int ofTotal) {
		                	try {
		                        // Determine if the new message was already known (e.g. partial)
		                        // And create or reload the full message info
		                        MessageContent localMessage = provider.queryMessageWithUid(
		                        		account.getId(), folder.getId(), message.getUid()); 
		                        if (localMessage == null) { 
		                            localMessage = provider.newMessageContent(account.getId(), folder.getId());
		                            localMessage.setAccountKey(account.getId()); 
		                            localMessage.setMailboxKey(folder.getId()); 
		                            localMessage.setMailboxType(folder.getType()); 
		                            localMessage.setFlagLoaded(MessageContent.FLAG_LOADED_UNLOADED);
		                        } else 
		                        	localMessage = localMessage.startUpdate(); 
		                        
		                        if (localMessage != null) {
		                            // Copy the fields that are available into the message
		                        	controller.updateMessageFieldsSynchronous(localMessage, message, listener);
		                            // Track the "new" ness of the downloaded message
		                            if (!message.isSet(Flag.SEEN)) 
		                                newMessages.add(message);
		                        }
		                        
		                        listener.addFinishedCount(1);
		                	} catch (Exception e) {
		                        LOG.error("Error while storing downloaded message.", e);
		                    }
		                }
		                
		                public void messageStarted(String uid, int number, int ofTotal) {
		                }
		    	});
		}

		// 9. Refresh the flags for any messages in the local store that we didn't just download.
		if (LOG.isDebugEnabled()) { 
			LOG.debug("synchronizeMailboxGeneric: Step 9: " + 
					"Refresh the flags for any messages in the local store that we didn't just download."); 
		}
		FetchProfile fp = new FetchProfile();
		fp.add(FetchProfile.Item.FLAGS);
		remoteFolder.fetch(remoteMessages, fp, null);
		boolean remoteSupportsSeen = false;
		boolean remoteSupportsFlagged = false;
		boolean remoteSupportsAnswered = false;
		for (Flag flag : remoteFolder.getPermanentFlags()) {
		    if (flag == Flag.SEEN) 
		        remoteSupportsSeen = true;
		    if (flag == Flag.FLAGGED) 
		        remoteSupportsFlagged = true;
		    if (flag == Flag.ANSWERED) 
		    	remoteSupportsAnswered = true;
		}

		// Update the SEEN & FLAGGED (star) flags (if supported remotely - e.g. not for POP3)
		if (remoteSupportsSeen || remoteSupportsFlagged || remoteSupportsAnswered) {
			if (LOG.isDebugEnabled()) { 
				LOG.debug("synchronizeMailboxGeneric: Step 9: " + 
						"Update the SEEN & FLAGGED (star) flags (if supported remotely - e.g. not for POP3)"); 
		    }
		    for (Message remoteMessage : remoteMessages) {
		        LocalMessageInfo localMessageInfo = localMessageMap.get(remoteMessage.getUid());
		        if (localMessageInfo == null) 
		            continue;
		        
		        boolean localSeen = localMessageInfo.getFlagRead();
		        boolean remoteSeen = remoteMessage.isSet(Flag.SEEN);
		        boolean newSeen = (remoteSupportsSeen && (remoteSeen != localSeen));
		        
		        boolean localFlagged = localMessageInfo.getFlagFavorite();
		        boolean remoteFlagged = remoteMessage.isSet(Flag.FLAGGED);
		        boolean newFlagged = (remoteSupportsFlagged && (localFlagged != remoteFlagged));
		        
		        boolean localAnswered = localMessageInfo.getFlagAnswered();
		        boolean remoteAnswered = remoteMessage.isSet(Flag.ANSWERED);
		        boolean newAnswered = (remoteSupportsAnswered && (localAnswered != remoteAnswered));
		        
		        if (newSeen || newFlagged || newAnswered) {
		        	provider.updateMessageFlags(localMessageInfo.getId(), remoteSeen, remoteFlagged, remoteAnswered); 
		        }
		    }
		}

		// 10. Compute and store the unread message count.
		if (LOG.isDebugEnabled()) { 
			LOG.debug("synchronizeMailboxGeneric: Step 10: " + 
					"Compute and store the unread message count."); 
		}
		int remoteUnreadMessageCount = remoteFolder.getUnreadMessageCount();
		if (remoteUnreadMessageCount < 0) {
		    if (remoteSupportsSeen) {
		        /**
		         * If remote folder doesn't supported unread message count but supports
		         * seen flag, use local folder's unread message count and the size of
		         * new messages. This mode is not used for POP3, or IMAP.
		         */
		        remoteUnreadMessageCount = folder.getUnreadCount() + newMessages.size();
		    } else {
		        /**
		         * If remote folder doesn't supported unread message count and doesn't
		         * support seen flag, use localUnreadCount and newMessageCount which
		         * don't rely on remote SEEN flag.  This mode is used by POP3.
		         */
		        remoteUnreadMessageCount = localUnreadCount + newMessageCount;
		    }
		} else {
		    /**
		     * If remote folder supports unread message count, use remoteUnreadMessageCount.
		     * This mode is used by IMAP.
		     */
		}

		// 11. Remove any messages that are in the local store but no longer on the remote store.
		if (LOG.isDebugEnabled()) { 
			LOG.debug("synchronizeMailboxGeneric: Step 11: " + 
					"Remove any messages that are in the local store but no longer on the remote store."); 
		}
		if (controller.shouldDeleteMessageNotOnRemote(account, folder)) { 
		    HashSet<String> localUidsToDelete = new HashSet<String>(localMessageMap.keySet());
		    localUidsToDelete.removeAll(remoteUidMap.keySet());
		    for (String uidToDelete : localUidsToDelete) {
		        LocalMessageInfo infoToDelete = localMessageMap.get(uidToDelete);
		        if (LOG.isDebugEnabled()) { 
		    		LOG.debug("synchronizeMailboxGeneric: Step 11: For one message: " + 
		    				"Delete the message not on the remote store, messageId=" + infoToDelete.getId()); 
		        }
		        
		        // Delete the message itself
		        provider.deleteMessage(account.getId(), infoToDelete.getId()); 
		    }
		}

		// 12. Divide the unsynced messages into small & large (by size)
		if (LOG.isDebugEnabled()) { 
			LOG.debug("synchronizeMailboxGeneric: Step 12: " + 
					"Divide the unsynced messages into small & large (by size)."); 
		}
		// TODO doing this work here (synchronously) is problematic because it prevents the UI
		// from affecting the order (e.g. download a message because the user requested it.)  Much
		// of this logic should move out to a different sync loop that attempts to update small
		// groups of messages at a time, as a background task.  However, we can't just return
		// (yet) because POP messages don't have an envelope yet....
		
		ArrayList<Message> largeMessages = new ArrayList<Message>();
		ArrayList<Message> smallMessages = new ArrayList<Message>();
		for (Message message : unsyncedMessages) {
		    if (controller.shouldSyncLargeMessage(message)) {
		        largeMessages.add(message);
		    } else if (controller.shouldSyncSmallMessage(message)) {
		        smallMessages.add(message);
		    }
		}

		// 13. Download small messages
		if (LOG.isDebugEnabled()) { 
			LOG.debug("synchronizeMailboxGeneric: Step 13: " + 
					"Download small messages."); 
		}
		// TODO Problems with this implementation.  1. For IMAP, where we get a real envelope,
		// this is going to be inefficient and duplicate work we've already done.  2.  It's going
		// back to the DB for a local message that we already had (and discarded).
		
		// For small messages, we specify "body", which returns everything (incl. attachments)
		fp = new FetchProfile();
		fp.add(FetchProfile.Item.BODY);
		
		listener.setTotalCount(smallMessages.size());
		listener.setFinishedCount(0);
		
		remoteFolder.fetch(smallMessages.toArray(new Message[smallMessages.size()]), fp,
		        new MessageRetrievalListener() {
		            public void messageFinished(Message message, int number, int ofTotal) {
		            	try { 
		                    // Store the updated message locally and mark it fully loaded
		            		controller.copyOneMessageToProviderSynchronous(message, account, folder,
		                            MessageContent.FLAG_LOADED_COMPLETE, listener);
		            		
		            		listener.addFinishedCount(1);
		            	} catch (MessagingException ex) { 
		            		LOG.error("copy small message: "+message+" error", ex); 
		            	}
		            }
		
		            public void messageStarted(String uid, int number, int ofTotal) {
		            }
		});

		// 14. Download large messages.  We ask the server to give us the message structure,
		// but not all of the attachments.
		if (LOG.isDebugEnabled()) { 
			LOG.debug("synchronizeMailboxGeneric: Step 14: " + 
					"Download large messages.  We ask the server to give us the message structure, but not all of the attachments."); 
		}
		fp.clear();
		fp.add(FetchProfile.Item.STRUCTURE);
		
		listener.setTotalCount(largeMessages.size());
		listener.setFinishedCount(0);
		
		remoteFolder.fetch(largeMessages.toArray(new Message[largeMessages.size()]), fp, null);
		for (Message message : largeMessages) {
		    if (message.getBody() == null) {
		    	if (LOG.isDebugEnabled()) { 
		    		LOG.debug("synchronizeMailboxGeneric: Step 14: For one message: " + 
		    				"POP doesn't support STRUCTURE mode, UUID=" + message.getUid()); 
		        }
		        // POP doesn't support STRUCTURE mode, so we'll just do a partial download
		        // (hopefully enough to see some/all of the body) and mark the message for
		        // further download.
		        fp.clear();
		        fp.add(FetchProfile.Item.BODY_SANE);
		        //  TODO a good optimization here would be to make sure that all Stores set
		        //  the proper size after this fetch and compare the before and after size. If
		        //  they equal we can mark this SYNCHRONIZED instead of PARTIALLY_SYNCHRONIZED
		        remoteFolder.fetch(new Message[] { message }, fp, null);
		
		        // Store the partially-loaded message and mark it partially loaded
		        controller.copyOneMessageToProviderSynchronous(message, account, folder,
		                MessageContent.FLAG_LOADED_PARTIAL, listener);
		        
		        listener.addFinishedCount(1);
		        
		    } else {
		    	if (LOG.isDebugEnabled()) { 
		    		LOG.debug("synchronizeMailboxGeneric: Step 14: For one message: " + 
		    				"We have a structure to deal with, UUID=" + message.getUid()); 
		        }
		        // We have a structure to deal with, from which
		        // we can pull down the parts we want to actually store.
		        // Build a list of parts we are interested in. Text parts will be downloaded
		        // right now, attachments will be left for later.
		        ArrayList<Part> viewables = new ArrayList<Part>();
		        ArrayList<Part> attachments = new ArrayList<Part>();
		        MimeUtility.collectParts(message, viewables, attachments);
		        
		        // Download the viewables immediately
		        for (Part part : viewables) {
		            fp.clear();
		            fp.add(part);
		            // TODO what happens if the network connection dies? We've got partial
		            // messages with incorrect status stored.
		            remoteFolder.fetch(new Message[] { message }, fp, null);
		        }
		        
		        // Store the updated message locally and mark it fully loaded
		        controller.copyOneMessageToProviderSynchronous(message, account, folder,
		                MessageContent.FLAG_LOADED_COMPLETE, listener);
		        
		        listener.addFinishedCount(1);
		    }
		}

		// 15. Clean up and report results
		if (LOG.isDebugEnabled()) { 
			LOG.debug("synchronizeMailboxGeneric: Step 15: " + 
					"Clean up and report results."); 
		}
		remoteFolder.close(false);
		// TODO - more
		
		if (remoteUnreadMessageCount < 0) 
			remoteUnreadMessageCount = newMessages.size(); 
		
		int localTotalCount = provider.countTotalMessages(account.getId(), folder.getId()); 
		
		return new StoreSynchronizer.SyncResults(
				remoteMessageCount, remoteUnreadMessageCount, 
				localTotalCount, newMessages.size());
	}
	
}
