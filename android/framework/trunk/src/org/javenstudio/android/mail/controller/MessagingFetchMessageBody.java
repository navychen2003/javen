package org.javenstudio.android.mail.controller;

import org.javenstudio.mail.FetchProfile;
import org.javenstudio.mail.Folder;
import org.javenstudio.mail.Message;
import org.javenstudio.mail.MessagingException;
import org.javenstudio.mail.store.Store;

import org.javenstudio.android.mail.content.AccountContent;
import org.javenstudio.android.mail.content.MailContentProvider;
import org.javenstudio.android.mail.content.MailboxContent;
import org.javenstudio.android.mail.content.MessageContent;
import org.javenstudio.common.util.Logger;

public class MessagingFetchMessageBody {
	private static Logger LOG = Logger.getLogger(MessagingFetchMessageBody.class);

	public static void fetchMessageBody(final MessagingController controller, 
			final AccountContent account, final long messageId, 
    		final CallbackMessagingListener listener) throws MessagingException { 
    	final MailContentProvider provider = MailContentProvider.getInstance(); 
		
    	// 1. Resample the message, in case it disappeared or synced while
        // this command was in queue
    	MessageContent message = provider.queryMessage(messageId); 
    	if (message == null) { 
    		if (LOG.isDebugEnabled()) { 
    			LOG.debug("fetchMessageBody:" + 
    					" message: "+messageId+" not found"); 
    		}
    		return; 
    	}
    	
    	if (message.getFlagLoaded() == MessageContent.FLAG_LOADED_COMPLETE) { 
    		if (LOG.isDebugEnabled()) { 
    			LOG.debug("fetchMessageBody:" + 
    					" message: "+messageId+" already load complete"); 
    		}
    		return; 
    	}
    	
    	listener.callbackFetchMessageBodyStarted(messageId); 
    	
    	// 2. Open the remote folder.
        // TODO all of these could be narrower projections
        // TODO combine with common code in loadAttachment
    	MailboxContent mailbox = provider.queryMailbox(message.getMailboxKey()); 
    	if (mailbox == null) { 
    		if (LOG.isDebugEnabled()) {
    			LOG.debug("fetchMessageBody:" + 
    					" mailbox: "+message.getMailboxKey()+" not found"); 
    		}
    		
    		throw new MessagingException("mailbox: "+message.getMailboxKey()+" not found"); 
    	}
	    
    	Store remoteStore = Store.getInstance(account.getStoreHostAuth().toUri()); 
    	Folder remoteFolder = remoteStore.getFolder(mailbox.getDisplayName()); 
    	remoteFolder.open(Folder.OpenMode.READ_WRITE, null);
    	
        // 3. Not supported, because IMAP & POP don't use it: structure prefetch
//      if (remoteStore.requireStructurePrefetch()) {
//      // For remote stores that require it, prefetch the message structure.
//      FetchProfile fp = new FetchProfile();
//      fp.add(FetchProfile.Item.STRUCTURE);
//      localFolder.fetch(new Message[] { message }, fp, null);
//
//      ArrayList<Part> viewables = new ArrayList<Part>();
//      ArrayList<Part> attachments = new ArrayList<Part>();
//      MimeUtility.collectParts(message, viewables, attachments);
//      fp.clear();
//      for (Part part : viewables) {
//          fp.add(part);
//      }
//
//      remoteFolder.fetch(new Message[] { message }, fp, null);
//
//      // Store the updated message locally
//      localFolder.updateMessage((LocalMessage)message);
    	
    	// 4. Set up to download the entire message
        Message remoteMessage = remoteFolder.getMessage(message.getServerId());
        FetchProfile fp = new FetchProfile();
        fp.add(FetchProfile.Item.BODY);
        remoteFolder.fetch(new Message[] { remoteMessage }, fp, null);

        // 5. Write to provider
        controller.copyOneMessageToProviderSynchronous(remoteMessage, account, mailbox,
                MessageContent.FLAG_LOADED_COMPLETE, listener);
    	
        listener.callbackFetchMessageBodyFinished(messageId); 
    }
	
}
