package org.javenstudio.android.mail.controller;

import java.util.List;

import org.javenstudio.mail.Folder;
import org.javenstudio.mail.Message;
import org.javenstudio.mail.MessagingException;
import org.javenstudio.mail.Part;
import org.javenstudio.mail.store.Store;
import org.javenstudio.mail.store.StoreSynchronizer;
import org.javenstudio.util.StringUtils;

import org.javenstudio.android.mail.Constants;
import org.javenstudio.android.mail.content.AccountContent;
import org.javenstudio.android.mail.content.AttachmentContent;
import org.javenstudio.android.mail.content.BodyContent;
import org.javenstudio.android.mail.content.LocalMessageInfo;
import org.javenstudio.android.mail.content.MailContentProvider;
import org.javenstudio.android.mail.content.MailboxContent;
import org.javenstudio.android.mail.content.MessageContent;
import org.javenstudio.common.util.Logger;

public abstract class MessagingController extends MailController {
	private static Logger LOG = Logger.getLogger(MessagingController.class);

    /**
     * We write this into the serverId field of messages that will never be upsynced.
     */
    public static final String LOCAL_SERVERID_PREFIX = "Local-";
	
    //private static Flag[] FLAG_LIST_SEEN = new Flag[] { Flag.SEEN };
    //private static Flag[] FLAG_LIST_FLAGGED = new Flag[] { Flag.FLAGGED };
    
	/**
     * All access to mListeners *must* be synchronized
     */
    
	public MessagingController() { 
		super(); 
	}
	
	/**
     * Checks mail for one or multiple accounts. If account is null all accounts
     * are checked.  This entry point is for use by the mail checking service only, because it
     * gives slightly different callbacks (so the service doesn't get confused by callbacks
     * triggered by/for the foreground UI.
     *
     * TODO clean up the execution model which is unnecessarily threaded due to legacy code
     *
     * @param context
     * @param accountId the account to check
     * @param listener
     */
	@Override 
	protected final void checkMailSynchronous(final AccountContent account, 
			final CallbackMessagingListener listener) throws MessagingException {
		final MailContentProvider provider = MailContentProvider.getInstance(); 
		
		synchronizeFoldersSynchronous(account, listener); 
		
		// send any pending outbound messages.  note, there is a slight race condition
        // here if we somehow don't have a sent folder, but this should never happen
        // because the call to sendMessage() would have built one previously.
    	MailboxContent sentbox = provider.queryMailboxWithType(account.getId(), MailboxContent.TYPE_SENT); 
    	if (sentbox != null) 
            sendPendingMessagesSynchronous(account, null, sentbox, listener);
    	
    	// find mailbox # for inbox and sync it.
        // TODO we already know this in Controller, can we pass it in?
    	MailboxContent inbox = provider.queryMailboxWithType(account.getId(), MailboxContent.TYPE_INBOX);
        if (inbox != null) 
            synchronizeMailboxSynchronousInternal(account, inbox, listener);

    }
	
	/**
     * Finish loading a message that have been partially downloaded.
     *
     * @param messageId the message to load
     * @param listener the callback by which results will be reported
     */
    @Override 
    protected final void fetchMessageBodySynchronous(final AccountContent account, final long messageId, 
    		final CallbackMessagingListener listener) throws MessagingException { 
    	try { 
    		fetchMessageBodyInternal(account, messageId, listener); 
    	} catch (MessagingException ex) { 
    		listener.callbackFetchMessageBodyFailed(messageId, MessagingEvent.RESULT_EXCEPTION, ex); 
    		throw ex; 
    	}
    }
    
    protected void fetchMessageBodyInternal(final AccountContent account, final long messageId, 
    		final CallbackMessagingListener listener) throws MessagingException { 
    	MessagingFetchMessageBody.fetchMessageBody(this, account, messageId, listener);
    }
	
    /**
     * Finish loading a message attachment that have been partially downloaded.
     *
     * @param messageId the message to load
     * @param attachmentId the message attachment to load
     * @param listener the callback by which results will be reported
     */
    @Override 
    protected final void fetchMessageAttachmentSynchronous(
    		final AccountContent account, final long messageId, final long attachmentId, 
    		final CallbackMessagingListener listener) throws MessagingException { 
    	try { 
    		fetchMessageAttachmentInternal(account, messageId, attachmentId, listener); 
    	} catch (MessagingException ex) { 
    		listener.callbackFetchMessageAttachmentFailed(
    				messageId, attachmentId, null, MessagingEvent.RESULT_EXCEPTION, ex); 
    		throw ex; 
    	}
    }
    
    protected void fetchMessageAttachmentInternal(
    		final AccountContent account, final long messageId, final long attachmentId, 
    		final CallbackMessagingListener listener) throws MessagingException { 
    	MessagingFetchMessageAttachment.fetchMessageAttachment(this, 
    			account, messageId, attachmentId, listener);
    }
    
	/**
     * Lists folders that are available locally and remotely. This method calls
     * listFoldersCallback for local folders before it returns, and then for
     * remote folders at some later point. If there are no local folders
     * includeRemote is forced by this method. This method should be called from
     * a Thread as it may take several seconds to list the local folders.
     *
     * TODO this needs to cache the remote folder list
     * TODO break out an inner listFoldersSynchronized which could simplify checkMail
     *
     * @param account
     * @param listener
     * @throws MessagingException
     */
	@Override 
	protected final void synchronizeFoldersSynchronous(final AccountContent account, 
			final CallbackMessagingListener listener) throws MessagingException {
		listener.callbackSynchronizeFoldersStarted(); 
		
		int newFolders = 0; 
		try { 
			newFolders = synchronizeFoldersInternal(account); 
			
		} catch (MessagingException ex) { 
			listener.callbackSynchronizeFoldersFailed(newFolders, ex); 
			throw ex; 
			
		} finally { 
			listener.callbackSynchronizeFoldersFinished(newFolders); 
		}
	}
	
	@Override 
	protected final void synchronizeActionsSynchronous(final AccountContent account, 
			final CallbackMessagingListener listener) throws MessagingException { 
		listener.callbackSynchronizeActionsStarted(); 
		
		try { 
			processPendingActionsSynchronous(account, listener); 
			
		} catch (MessagingException ex) { 
			listener.callbackSynchronizeActionsFailed(ex); 
			throw ex; 
			
		} finally { 
			listener.callbackSynchronizeActionsFinished(); 
		}
	}
	
	protected String normalizeFolderName(String name) { 
		return StringUtils.trim(name != null ? name.toUpperCase() : name);
	}
	
	protected int synchronizeFoldersInternal(final AccountContent account) throws MessagingException {
		return MessagingSynchronizeFolders.synchronizeFolders(this, account);
	}
	
	@Override 
    protected final void synchronizeMailboxSynchronous(final AccountContent account, final long mailboxId, 
    		final CallbackMessagingListener listener) throws MessagingException {
		final MailContentProvider provider = MailContentProvider.getInstance(); 
		
		final MailboxContent folder = provider.queryMailbox(mailboxId); 
		if (folder == null) { 
			if (LOG.isDebugEnabled()) { 
    			LOG.debug("synchronizeMailboxSynchronous:" + 
    					" mailbox: "+mailboxId+" not found"); 
			}
			return; 
		}
		
		synchronizeMailboxSynchronousInternal(account, folder, listener); 
	}
	
    /**
     * Start foreground synchronization of the specified folder. This is called by
     * synchronizeMailbox or checkMail.
     * TODO this should use ID's instead of fully-restored objects
     * @param account
     * @param folder
     */
    protected void synchronizeMailboxSynchronousInternal(
    		final AccountContent account, final MailboxContent folder, 
    		final CallbackMessagingListener listener) throws MessagingException {
		MessagingSynchronizeMailbox.synchronizeMailbox(this, account, folder, listener);
    }
    
    /**
     * Send a message:
     * - move the message to Outbox (the message is assumed to be in Drafts).
     * - EAS service will take it from there
     * - trigger send for POP/IMAP
     * @param messageId the id of the message to send
     */
    @Override
    protected final void sendMessageSynchronous(final AccountContent account, final long messageId, 
    		final CallbackMessagingListener listener) throws MessagingException { 
    	try { 
    		sendMessageInternal(account, messageId, listener); 
    	} catch (MessagingException ex) { 
    		listener.callbackSendMessageFailed(messageId, MessagingEvent.RESULT_EXCEPTION, ex); 
    		throw ex; 
    	}
    }
    
    protected void sendMessageInternal(final AccountContent account, final long messageId, 
    		final CallbackMessagingListener listener) throws MessagingException { 
    	MessagingSendMessage.sendMessage(this, account, messageId, listener);
    }
    
    /**
     * Attempt to send any messages that are sitting in the Outbox.
     *
     * @param account
     * @param listener
     */
    protected void sendPendingMessagesSynchronous(
    		final AccountContent account, MailboxContent outbox, MailboxContent sentbox, 
    		final CallbackMessagingListener listener) throws MessagingException {
    	MessagingSendMessage.sendPendingMessages(this, account, outbox, sentbox, listener);
    }
    
    /**
     * Find messages in the updated table that need to be written back to server.
     *
     * Handles:
     *   Read/Unread
     *   Flagged
     *   Append (upload)
     *   Move To Trash
     *   Empty trash
     * TODO:
     *   Move
     *
     * @param account the account to scan for pending actions
     * @throws MessagingException
     */
    protected final void processPendingActionsSynchronous(final AccountContent account, 
    		final CallbackMessagingListener listener) throws MessagingException {
    	
    	try { 
    		listener.callbackProcessPendingDeletesStarted(); 
    		
    		// Handle deletes first, it's always better to get rid of things first
    		processPendingDeletesSynchronous(account, listener);
    		
    	} catch (MessagingException ex) { 
    		listener.callbackProcessPendingDeletesFailed(ex); 
    		throw ex; 
    		
    	} finally { 
    		listener.callbackProcessPendingDeletesFinished(); 
    	}

    	try { 
    		listener.callbackProcessPendingUploadsStarted(); 
    		
	        // Handle uploads (currently, only to sent messages)
	        processPendingUploadsSynchronous(account, listener);
	        
    	} catch (MessagingException ex) { 
    		listener.callbackProcessPendingUploadsFailed(ex); 
    		throw ex; 
    		
    	} finally { 
    		listener.callbackProcessPendingUploadsFinished(); 
    	}

    	try { 
    		listener.callbackProcessPendingUpdatesStarted(); 
    		
	        // Now handle updates / upsyncs
	        processPendingUpdatesSynchronous(account, listener);
	        
    	} catch (MessagingException ex) { 
    		listener.callbackProcessPendingUpdatesFailed(ex); 
    		throw ex; 
    		
    	} finally { 
    		listener.callbackProcessPendingUpdatesFinished(); 
    	}
    }
    
    /**
     * Scan for messages that are in the Message_Deletes table, look for differences that
     * we can deal with, and do the work.
     *
     * @param account
     * @param resolver
     * @param accountIdArgs
     */
    protected void processPendingDeletesSynchronous(final AccountContent account, 
    		final CallbackMessagingListener listener) throws MessagingException {
    	MessagingProcessDeletes.processPendingDeletes(this, account, listener);
    }
    
    protected boolean checkDeleteMessageFromTrash(Store remoteStore, 
    		AccountContent account, MailboxContent mailbox, MessageContent message) { 
    	if (remoteStore != null && account != null && mailbox != null && message != null) { 
    		return mailbox.getType() == MailboxContent.TYPE_TRASH; 
    	}
    	
    	return false;
    }
    
    /**
     * Process a pending trash message command.
     *
     * @param remoteStore the remote store we're working in
     * @param account The account in which we are working
     * @param oldMailbox The local trash mailbox
     * @param oldMessage The message that was deleted from the trash
     */
    protected void processPendingDeleteFromTrash(final Store remoteStore,
            final AccountContent account, MailboxContent mailbox, MessageContent message, 
            final CallbackMessagingListener listener) throws MessagingException {
    	MessagingProcessDeletes.processPendingDeleteFromTrash(this, 
    			remoteStore, account, mailbox, message, listener);
    }
    
    protected boolean checkDeleteMessageFromFolder(Store remoteStore, 
    		AccountContent account, MailboxContent mailbox, MessageContent message) { 
    	if (remoteStore != null && account != null && mailbox != null && message != null) { 
    		if (mailbox.getType() == MailboxContent.TYPE_TRASH) 
    			return true;
    		
    		if (remoteStore.supportDeleteMessageFromAnyFolder()) 
    			return true;
    	}
    	
    	return false;
    }
    
    /**
     * Process a pending folder message command.
     *
     * @param remoteStore the remote store we're working in
     * @param account The account in which we are working
     * @param oldMailbox The local folder mailbox
     * @param oldMessage The message that was deleted from the folder
     */
    protected void processPendingDeleteFromFolder(final Store remoteStore,
            final AccountContent account, MailboxContent mailbox, MessageContent message, 
            final CallbackMessagingListener listener) throws MessagingException {
    	MessagingProcessDeletes.processPendingDeleteFromFolder(this, 
    			remoteStore, account, mailbox, message, listener);
    }
    
    protected boolean checkUploadMessagesToRemote(final AccountContent account, final MailboxContent mailbox) { 
    	if (account != null && mailbox != null) { 
    		if (mailbox.getType() == MailboxContent.TYPE_SENT) 
    			return true;
    	}
    	
    	return false;
    }
    
    protected boolean checkUploadMessageToRemote(final Store remoteStore, 
    		final AccountContent account, final MailboxContent mailbox, final MessageContent message) { 
    	if (remoteStore != null && account != null && mailbox != null && message != null) { 
    		if (mailbox.getType() == MailboxContent.TYPE_SENT) 
    			return true;
    	}
    	
    	return false;
    }
    
    /**
     * Scan for messages that are in Sent, and are in need of upload,
     * and send them to the server.  "In need of upload" is defined as:
     *  serverId == null (no UID has been assigned)
     * or
     *  message is in the updated list
     *
     * Note we also look for messages that are moving from drafts->outbox->sent.  They never
     * go through "drafts" or "outbox" on the server, so we hang onto these until they can be
     * uploaded directly to the Sent folder.
     *
     * @param account
     * @param resolver
     * @param accountIdArgs
     */
    protected void processPendingUploadsSynchronous(final AccountContent account, 
    		final CallbackMessagingListener listener) throws MessagingException {
    	MessagingProcessUploads.processPendingUploads(this, account, listener);
    }
    
    /**
     * Process a pending append message command. This command uploads a local message to the
     * server, first checking to be sure that the server message is not newer than
     * the local message.
     *
     * @param remoteStore the remote store we're working in
     * @param account The account in which we are working
     * @param newMailbox The mailbox we're appending to
     * @param message The message we're appending
     * @return true if successfully uploaded
     */
    protected void processPendingAppendInternal(final Store remoteStore, 
    		final AccountContent account, final MailboxContent mailbox, final MessageContent message) 
    		throws MessagingException {
    	MessagingProcessUploads.processPendingAppendInternal(this, 
    			remoteStore, account, mailbox, message);
    }
    
    /**
     * Read a complete Provider message into a legacy message (for IMAP upload).  This
     * is basically the equivalent of LocalFolder.getMessages() + LocalFolder.fetch().
     */
    protected Message makeMessage(MessageContent localMessage)
            throws MessagingException {
    	return MessagingMakeMessage.makeMessage(this, localMessage);
    }
    
    /**
     * Scan for messages that are in the Message_Updates table, look for differences that
     * we can deal with, and do the work.
     *
     * @param account
     * @param resolver
     * @param accountIdArgs
     */
    protected void processPendingUpdatesSynchronous(final AccountContent account, 
    		final CallbackMessagingListener listener) throws MessagingException {
    	MessagingProcessUpdates.processPendingUpdates(this, account, listener);
    }
    
    /**
     * Process a pending move message command.
     *
     * @param remoteStore the remote store we're working in
     * @param account The account in which we are working
     * @param newMailbox The local trash mailbox
     * @param oldMessage The message copy that was saved in the updates shadow table
     * @param newMessage The message that was moved to the mailbox
     */
    protected final void processPendingMoveFolder(final Store remoteStore,
            final AccountContent account, final MailboxContent newMailbox, final MessageContent message,
            final MessageActionQueue.MessageActions actionMessage, 
            final CallbackMessagingListener listener) throws MessagingException {
    	
        if (LOG.isDebugEnabled()) { 
    		LOG.debug("processPendingMoveFolder:" + 
    			" move message to new folder for message id=" + message.getId() + 
    			" serverId=" + message.getServerId() + 
    			" oldMailboxId=" + message.getMailboxKey() + 
    			" newMailboxId=" + newMailbox.getId()); 
    	}
    	
        try { 
        	listener.callbackMoveMessageFolderStarted(
	        		message.getId(), message.getMailboxKey(), newMailbox.getId()); 
        
        	processPendingMoveFolderInternal(remoteStore, account, 
        			newMailbox, message, actionMessage, false, 
        			listener);
        	
        } catch (MessagingException ex) { 
        	listener.callbackMoveMessageFolderFailed(
	        		message.getId(), message.getMailboxKey(), newMailbox.getId(), ex); 
        	
        	throw ex; 
        	
        } finally { 
        	listener.callbackMoveMessageFolderFinished(
	        		message.getId(), message.getMailboxKey(), newMailbox.getId()); 
        }
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
    protected final void processPendingMoveToTrash(final Store remoteStore,
            final AccountContent account, final MailboxContent newMailbox, final MessageContent message,
            final MessageActionQueue.MessageActions actionMessage, 
            final CallbackMessagingListener listener) throws MessagingException {
    	
    	if (LOG.isDebugEnabled()) { 
    		LOG.debug("processPendingMoveToTrash:" + 
    			" move message to trash for message id=" + message.getId() + 
    			" serverId=" + message.getServerId() + 
    			" oldMailboxId=" + message.getMailboxKey() + 
    			" newMailboxId=" + newMailbox.getId()); 
    	}
    	
    	try { 
        	listener.callbackMoveMessageToTrashStarted(
	        		message.getId(), message.getMailboxKey(), newMailbox.getId()); 
        
        	processPendingMoveFolderInternal(remoteStore, account, 
        			newMailbox, message, actionMessage, true, 
        			listener);
        	
        } catch (MessagingException ex) { 
        	listener.callbackMoveMessageToTrashFailed(
	        		message.getId(), message.getMailboxKey(), newMailbox.getId(), ex); 
        	
        	throw ex; 
        	
        } finally { 
        	listener.callbackMoveMessageToTrashFinished(
	        		message.getId(), message.getMailboxKey(), newMailbox.getId()); 
        }
    }
    
    protected void processPendingMoveFolderInternal(final Store remoteStore,
            final AccountContent account, final MailboxContent newMailbox, final MessageContent message,
            final MessageActionQueue.MessageActions actionMessage, boolean deleteAction, 
            final CallbackMessagingListener listener) throws MessagingException {
    	MessagingProcessMove.processPendingMoveFolder(this, remoteStore, account, newMailbox, 
    			message, actionMessage, deleteAction, listener);
    }
    
    protected boolean checkDeletePolicyNeverWhenMoveMessageToTrash(
    		final AccountContent account, final MessageContent message) { 
    	return false; //account.getDeletePolicy() == AccountContent.DELETE_POLICY_NEVER; 
    }
    
    /**
     * Upsync changes to read or flagged
     *
     * @param remoteStore
     * @param mailbox
     * @param changeRead
     * @param changeFlagged
     * @param newMessage
     */
    protected void processPendingFlagChange(final Store remoteStore,
            final AccountContent account, final MailboxContent mailbox, final MessageContent message, 
            final MessageActionQueue.MessageActions actionMessage, 
            final CallbackMessagingListener listener) throws MessagingException {
    	MessagingProcessFlagChange.processPendingFlagChange(this, 
    			remoteStore, account, mailbox, message, actionMessage, listener);
    }
    
    protected Message getRemoteMessage(Store remoteStore, AccountContent account, 
    		MailboxContent mailbox, MessageContent message) throws MessagingException { 
    	if (remoteStore != null && message != null) { 
    		// 0. No remote update if the message is local-only
        	String serverId = message.getServerId(); 
            if (serverId == null || serverId.length() == 0 || serverId.startsWith(LOCAL_SERVERID_PREFIX))
                return null;
            
            // 2. Open the remote store & folder
            Folder remoteFolder = remoteStore.getFolder(mailbox.getDisplayName());
            if (!remoteFolder.exists()) 
                return null;
            
            remoteFolder.open(Folder.OpenMode.READ_WRITE, null);
            if (remoteFolder.getMode() != Folder.OpenMode.READ_WRITE) 
                return null;

            // 3. Finally, apply the changes to the message
            Message remoteMessage = remoteFolder.getMessage(serverId);
            if (remoteMessage == null) 
                return null;
            
            return remoteMessage;
    	}
    	
    	return null;
    }
    
    /**
     * Generic synchronizer - used for POP3 and IMAP.
     *
     * TODO Break this method up into smaller chunks.
     *
     * @param account the account to sync
     * @param folder the mailbox to sync
     * @return results of the sync pass
     * @throws MessagingException
     */
    protected StoreSynchronizer.SyncResults synchronizeMailboxGeneric(
    		final AccountContent account, final MailboxContent folder, 
    		final CallbackMessagingListener listener) throws MessagingException {
    	return MessagingSynchronizeMailbox.synchronizeMailboxGeneric(this, account, folder, listener);
    }
    
    protected boolean checkUnsyncedMessage(Store remoteStore, LocalMessageInfo localMessage) { 
    	if (remoteStore != null && localMessage != null) { 
    		if (!remoteStore.supportMessageFlagsAndEnvelope()) 
    			return (localMessage.getFlagLoaded() == MessageContent.FLAG_LOADED_UNLOADED);
    	}
    	
    	return (localMessage == null) || ( /*(!localMessage.getFlagRead()) && */
    			(localMessage.getFlagLoaded() == MessageContent.FLAG_LOADED_UNLOADED) || 
    			(localMessage.getFlagLoaded() == MessageContent.FLAG_LOADED_PARTIAL)); 
    }
    
    protected boolean shouldDeleteMessageNotOnRemote(AccountContent account, MailboxContent mailbox) { 
    	if (mailbox != null && mailbox.getType() == MailboxContent.TYPE_SENT) 
    		return false;
    	
    	return true; 
    }
    
    protected boolean shouldSyncLargeMessage(Message message) throws MessagingException { 
    	if (message != null && message.getSize() > Constants.MAX_SMALL_MESSAGE_SIZE) 
    		return true; 
    	
    	return false; 
    }
    
    protected boolean shouldSyncSmallMessage(Message message) throws MessagingException { 
    	if (message != null && message.getSize() <= Constants.MAX_SMALL_MESSAGE_SIZE) 
    		return true; 
    	
    	return false; 
    }
    
    protected final void copyOneMessageToProviderSynchronous(Message message, 
    		AccountContent account, MailboxContent folder, int loadStatus, 
    		final CallbackMessagingListener listener) throws MessagingException {
    	listener.callbackCopyOneMessageStarted(folder, message); 
    	
    	try { 
    		copyOneMessageToProvider(message, account, folder, loadStatus); 
    		
    	} catch (MessagingException ex) { 
    		listener.callbackCopyOneMessageFailed(folder, message, ex); 
    		throw ex; 
    		
    	} finally { 
    		listener.callbackCopyOneMessageFinished(folder, message); 
    	}
    }
    
    /**
     * Copy one downloaded message (which may have partially-loaded sections)
     * into a provider message
     *
     * @param message the remote message we've just downloaded
     * @param account the account it will be stored into
     * @param folder the mailbox it will be stored into
     * @param loadStatus when complete, the message will be marked with this status (e.g.
     *        EmailContent.Message.LOADED)
     */
    protected void copyOneMessageToProvider(Message message, 
    		AccountContent account, MailboxContent folder, int loadStatus) 
    		throws MessagingException {
    	MessagingSaveMessage.copyOneMessageToProvider(this, message, account, folder, loadStatus);
    }
    
    /**
     * Copy body text (plain and/or HTML) from MimeMessage to provider Message
     */
    protected int updateBodyFields(MessageContent localMessage, MessageContent updateMessage, 
    		BodyContent body, List<Part> viewables) throws MessagingException {
    	return MessagingSaveMessageBody.updateBodyFields(this, localMessage, updateMessage, body, viewables);
    }
    
    /**
     * Copy attachments from MimeMessage to provider Message.
     *
     * @param context a context for file operations
     * @param localMessage the attachments will be built against this message
     * @param attachments the attachments to add
     * @param upgrading if true, we are upgrading a local account - handle attachments differently
     * @throws IOException
     */
    protected final long updateAttachments(MessageContent localMessage, MessageContent updateMessage, 
    		List<Part> attachments) throws MessagingException {
    	long size = 0; 
    	
        for (Part attachmentPart : attachments) {
            size += addOneAttachment(localMessage, attachmentPart);
        }
        
        return size; 
    }
    
    /**
     * Add a single attachment part to the message
     *
     * This will skip adding attachments if they are already found in the attachments table.
     * The heuristic for this will fail (false-positive) if two identical attachments are
     * included in a single POP3 message.
     * TODO: Fix that, by (elsewhere) simulating an mLocation value based on the attachments
     * position within the list of multipart/mixed elements.  This would make every POP3 attachment
     * unique, and might also simplify the code (since we could just look at the positions, and
     * ignore the filename, etc.)
     *
     * TODO: Take a closer look at encoding and deal with it if necessary.
     *
     * @param context a context for file operations
     * @param localMessage the attachments will be built against this message
     * @param part a single attachment part from POP or IMAP
     * @param upgrading true if upgrading a local account - handle attachments differently
     * @throws IOException
     */
    protected long addOneAttachment(MessageContent localMessage, Part part) 
    		throws MessagingException {
    	return MessagingSaveMessageAttachment.addOneAttachment(this, localMessage, part);
    }
    
    /**
     * Helper function to append text to a StringBuffer, creating it if necessary.
     * Optimization:  The majority of the time we are *not* appending - we should have a path
     * that deals with single strings.
     */
    protected StringBuilder appendTextPart(StringBuilder sb, String newText) {
        if (newText == null) {
            return sb;
        } else if (sb == null) {
            sb = new StringBuilder(newText);
        } else {
            if (sb.length() > 0) 
                sb.append('\n');
            sb.append(newText);
        }
        return sb;
    }
    
    protected final void updateMessageFieldsSynchronous(MessageContent localMessage, Message message, 
    		final CallbackMessagingListener listener) throws MessagingException { 
    	listener.callbackUpdateMessageFieldsStarted(localMessage, message); 
    	
    	try { 
    		updateMessageFields(localMessage, message); 
    		
    	} catch (MessagingException ex) { 
    		listener.callbackUpdateMessageFieldsFailed(localMessage, message, ex); 
    		throw ex; 
    		
    	} finally { 
    		listener.callbackUpdateMessageFieldsFinished(localMessage, message); 
    	}
    }
    
    /**
     * Copy field-by-field from a "store" message to a "provider" message
     * @param message The message we've just downloaded (must be a MimeMessage)
     * @param localMessage The message we'd like to write into the DB
     * @result true if dirty (changes were made)
     */
    protected void updateMessageFields(MessageContent localMessage, Message message) 
    		throws MessagingException { 
    	MessagingSaveMessage.updateMessageFields(this, localMessage, message);
    }
    
    protected void setOtherMessageFields(MessageContent localMessage, Message message) 
    		throws MessagingException { 
    	// do nothing
    }
    
    protected void setOtherBodyFields(BodyContent body, List<Part> viewables) throws MessagingException { 
    	// do nothing
    }
    
    protected void setOtherAttachmentFields(AttachmentContent localAttachment, Part part) 
    		throws MessagingException { 
    	// do nothing
    }
    
}
