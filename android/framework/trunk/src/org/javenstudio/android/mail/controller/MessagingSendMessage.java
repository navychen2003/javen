package org.javenstudio.android.mail.controller;

import java.util.HashMap;
import java.util.Map;

import org.javenstudio.mail.MessagingException;
import org.javenstudio.mail.sender.Sender;
import org.javenstudio.mail.store.Store;

import org.javenstudio.android.mail.content.AccountContent;
import org.javenstudio.android.mail.content.MailContentProvider;
import org.javenstudio.android.mail.content.MailboxContent;
import org.javenstudio.android.mail.content.MessageContent;
import org.javenstudio.common.util.EntityIterable;
import org.javenstudio.common.util.Logger;

public class MessagingSendMessage {
	private static Logger LOG = Logger.getLogger(MessagingSendMessage.class);

	public static void sendMessage(final MessagingController controller, 
			final AccountContent account, final long messageId, 
    		final CallbackMessagingListener listener) throws MessagingException { 
    	final MailContentProvider provider = MailContentProvider.getInstance(); 
    	
    	MessageContent message = provider.queryMessage(messageId); 
    	if (message == null) { 
    		if (LOG.isDebugEnabled()) { 
    			LOG.debug("sendMessage:" + 
    					" message: "+messageId+" not found"); 
    		}
    		return; 
    	}
    	
    	final MailboxContent folder = provider.queryMailbox(message.getMailboxKey()); 
		if (folder == null) { 
			if (LOG.isDebugEnabled()) { 
    			LOG.debug("sendMessage:" + 
    					" mailbox: "+message.getMailboxKey()+" not found"); 
			}
			return; 
		}
		
		if (folder.getType() != MailboxContent.TYPE_DRAFTS && folder.getType() != MailboxContent.TYPE_OUTBOX) { 
			if (LOG.isDebugEnabled()) { 
    			LOG.debug("sendMessage:" + 
    					" mailbox: "+message.getMailboxKey()+" isn't draft mailbox"); 
			}
			return; 
		}
		
		MailboxContent outbox = provider.queryOrCreateMailboxWithType(account.getId(), MailboxContent.TYPE_OUTBOX); 
		if (outbox == null) { 
			if (LOG.isDebugEnabled()) { 
    			LOG.debug("sendMessage:" + 
    					" out mailbox not found for account: " + account.getId()); 
			}
			return; 
		}
		
		if (folder.getType() == MailboxContent.TYPE_DRAFTS) { 
			if (outbox.getId() != folder.getId()) { 
				MessageContent messageUpdate = message.startUpdate();
				messageUpdate.setMailboxKey(outbox.getId()); 
				messageUpdate.setMailboxType(outbox.getType()); 
				messageUpdate.commitUpdates();
			}
		}
		
		controller.sendPendingMessagesSynchronous(account, outbox, null, listener);
    }
	
	public static void sendPendingMessages(final MessagingController controller, 
			final AccountContent account, MailboxContent outbox, MailboxContent sentbox, 
    		final CallbackMessagingListener listener) throws MessagingException {
    	final MailContentProvider provider = MailContentProvider.getInstance(); 
    	
    	if (outbox == null) { 
    		outbox = provider.queryMailboxWithType(account.getId(), MailboxContent.TYPE_OUTBOX); 
    		if (outbox == null) { 
	    		if (LOG.isDebugEnabled()) { 
	    			LOG.debug("sendPendingMessages:" + 
	    					" out mailbox not found for account: " + account.getId()); 
				}
				return;
    		}
    	}
    	
    	if (outbox.getType() != MailboxContent.TYPE_OUTBOX) 
    		return;
    	
    	if (sentbox != null && sentbox.getType() != MailboxContent.TYPE_SENT) 
    		return;
    	
    	if (LOG.isDebugEnabled()) { 
			LOG.debug("**** sendPendingMessages:" + 
					" account: " + account.getId() + " outbox: " + outbox.getId()); 
		}
    	
    	Map<Long, MessageContent> messages = new HashMap<Long, MessageContent>();
    	
    	EntityIterable<MessageContent> messageIt = provider.queryMessages(
        		account.getId(), outbox.getId()); 
        try { 
	        while (messageIt.hasNext()) { 
	        	MessageContent message = messageIt.next(); 
	        	if (message != null && message.getAccountKey() == account.getId() && 
	        			message.getMailboxKey() == outbox.getId()) { 
	        		messages.put(message.getId(), message);
	        	}
	        }
        } finally { 
        	messageIt.close(); 
        }
        
        if (messages.size() <= 0) 
        	return;
        
        provider.queryMailboxWithType(account.getId(), MailboxContent.TYPE_SENT);
        
        Sender sender = null; 
        Store remoteStore = null; 
        boolean requireMoveMessageToSentFolder = false; 
        
        listener.setTotalCount(messages.size());
        listener.setFinishedCount(0); 
        
        for (MessageContent message : messages.values()) { 
        	if (message == null) continue; 
        	
        	if (LOG.isDebugEnabled()) { 
    			LOG.debug("sendPendingMessages:" + 
    					" account: " + account.getId() + " send message: " + message.getId()); 
    		}
        	
        	final long messageId = message.getId();
        	listener.callbackSendMessageStarted(message.getId()); 
        	
        	try { 
        		if (sender == null) 
        			sender = Sender.getInstance(account.getSenderHostAuth().toUri());
        		
        		if (remoteStore == null) { 
        			remoteStore = Store.getInstance(account.getStoreHostAuth().toUri());
        			requireMoveMessageToSentFolder = remoteStore.requireCopyMessageToSentFolder();
        		}
        		
        		sender.sendMessage(message);
        		
        		if (requireMoveMessageToSentFolder) { 
        			if (sentbox == null) 
        				sentbox = provider.queryOrCreateMailboxWithType(account.getId(), MailboxContent.TYPE_SENT); 
        			
        			MessageContent messageUpdate = message.startUpdate(); 
        			messageUpdate.setMailboxKey(sentbox.getId()); 
        			messageUpdate.setMailboxType(sentbox.getType()); 
        			messageUpdate.commitUpdates();
        		}
        		
        		listener.callbackSendMessageFinished(messageId); 
        		listener.addFinishedCount(1);
        		
        	} catch (MessagingException ex) { 
        		listener.callbackSendMessageFailed(messageId, MessagingEvent.RESULT_EXCEPTION, ex); 
        		throw ex; 
        	}
        }
    }
	
}
