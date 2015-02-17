package org.javenstudio.android.mail.controller;

import org.javenstudio.mail.Flag;
import org.javenstudio.mail.Message;
import org.javenstudio.mail.MessagingException;
import org.javenstudio.mail.store.Store;

import org.javenstudio.android.mail.content.AccountContent;
import org.javenstudio.android.mail.content.MailboxContent;
import org.javenstudio.android.mail.content.MessageContent;
import org.javenstudio.common.util.Logger;

public class MessagingProcessFlagChange {
	private static Logger LOG = Logger.getLogger(MessagingProcessFlagChange.class);

	public static void processPendingFlagChange(final MessagingController controller, 
			final Store remoteStore, final AccountContent account, final MailboxContent mailbox, final MessageContent message, 
            final MessageActionQueue.MessageActions actionMessage, 
            final CallbackMessagingListener listener) throws MessagingException {
    	if (message.getId() != actionMessage.getMessageId()) 
    		return; 
    	
        // 1. No remote update for DRAFTS or OUTBOX
        if (mailbox.getType() == MailboxContent.TYPE_DRAFTS || mailbox.getType() == MailboxContent.TYPE_OUTBOX)
            return;

        boolean newFlagRead = message.getFlagRead(); 
        boolean newFlagFavorite = message.getFlagFavorite(); 
        boolean newFlagAnswered = message.getFlagAnswered();
        
        boolean updateFlagRead = false; 
        boolean updateFlagFavorite = false; 
        boolean updateFlagAnswered = false; 
        
        if (actionMessage.updateFlagRead()) {
        	boolean oldRead = message.getFlagRead(); 
        	boolean newRead = actionMessage.getFlagRead(); 
        	if (oldRead != newRead) { 
        		newFlagRead = newRead; 
        		updateFlagRead = true; 
        	}
        }
        
        if (actionMessage.updateFlagFavorite()) {
        	boolean oldFavorite = message.getFlagFavorite(); 
        	boolean newFavorite = actionMessage.getFlagFavorite(); 
        	if (oldFavorite != newFavorite) { 
        		newFlagFavorite = newFavorite; 
        		updateFlagFavorite = true; 
        	}
        }
        
        if (actionMessage.updateFlagAnswered()) {
        	boolean oldAnswered = message.getFlagAnswered(); 
        	boolean newAnswered = actionMessage.getFlagAnswered(); 
        	if (oldAnswered != newAnswered) { 
        		newFlagAnswered = newAnswered; 
        		updateFlagAnswered = true; 
        	}
        }
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("processPendingFlagChange:" + 
            	" update flags for message id=" + message.getId() + " serverId=" + message.getServerId() + 
				" flagRead=" + newFlagRead + " flagFavorite=" + newFlagFavorite + 
				" flagAnswered=" + newFlagAnswered);
        }
        
        try { 
        	listener.callbackUpdateMessageFlagStarted(
        			message.getId(), newFlagRead, newFlagFavorite); 
        	
	        Message remoteMessage = null; 
	        MessageContent updateMessage = message.startUpdate(); 
	        boolean contentChanged = false; 
	        
	        if (updateFlagRead) {
	        	if (remoteMessage == null && remoteStore.supportMessageFlag(Flag.SEEN)) { 
	        		remoteMessage = controller.getRemoteMessage(remoteStore, account, mailbox, message);
	        		if (remoteMessage == null) 
	        			return;
	        		
	        		remoteMessage.setFlag(Flag.SEEN, newFlagRead);
	        	}
	    		updateMessage.setFlagRead(newFlagRead); 
	    		contentChanged = true; 
	        }
	        
	        if (updateFlagFavorite) {
	        	if (remoteMessage == null && remoteStore.supportMessageFlag(Flag.FLAGGED)) { 
	        		remoteMessage = controller.getRemoteMessage(remoteStore, account, mailbox, message);
	        		if (remoteMessage == null) 
	        			return;
	        		
	        		remoteMessage.setFlag(Flag.FLAGGED, newFlagFavorite);
	        	}
	    		updateMessage.setFlagFavorite(newFlagFavorite); 
	    		contentChanged = true; 
	        }
	        
	        if (updateFlagAnswered) {
	        	if (remoteMessage == null && remoteStore.supportMessageFlag(Flag.ANSWERED)) { 
	        		remoteMessage = controller.getRemoteMessage(remoteStore, account, mailbox, message);
	        		if (remoteMessage == null) 
	        			return;
	        		
	        		remoteMessage.setFlag(Flag.ANSWERED, newFlagAnswered);
	        	}
	    		updateMessage.setFlagAnswered(newFlagAnswered); 
	    		contentChanged = true; 
	        }
	    	
	        if (contentChanged) 
	        	updateMessage.commitUpdates(); 
	        
	        listener.callbackUpdateMessageFlagFinished(
	        		message.getId(), newFlagRead, newFlagFavorite); 
	        
        } catch (MessagingException ex) { 
        	listener.callbackUpdateMessageFlagFailed(
	        		message.getId(), newFlagRead, newFlagFavorite, ex); 
        	
        	throw ex; 
        }
    }
	
}
