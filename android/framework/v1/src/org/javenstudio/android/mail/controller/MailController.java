package org.javenstudio.android.mail.controller;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.javenstudio.android.mail.Constants;
import org.javenstudio.android.mail.MessageActionException;
import org.javenstudio.android.mail.Preferences;
import org.javenstudio.android.mail.content.AccountContent;
import org.javenstudio.android.mail.content.MailContentProvider;
import org.javenstudio.android.mail.content.MailboxContent;
import org.javenstudio.common.util.Logger;
import org.javenstudio.mail.MessagingException;
import org.javenstudio.mail.store.StoreInfo;

public abstract class MailController {
	private static Logger LOG = Logger.getLogger(MailController.class);
	
	/**
     * Values for HEADER_ANDROID_BODY_QUOTED_PART to tag body parts
     */
    public static final String BODY_QUOTED_PART_REPLY = "quoted-reply";
    public static final String BODY_QUOTED_PART_FORWARD = "quoted-forward";
    public static final String BODY_QUOTED_PART_INTRO = "quoted-intro";
	
	private static MailController sInstance = null; 
	private static final Object sLock = new Object(); 
	
	/**
     * Gets or creates the singleton instance of Controller.
     * @param _context The context that will be used for all underlying system access
     */
    public synchronized static MailController getInstance() {
    	synchronized (sLock) { 
	        if (sInstance == null) 
	            sInstance = createController();
	        return sInstance;
    	}
    }
	
    @SuppressWarnings({"unchecked"})
	private static MailController createController() {
    	String className = Preferences.getPreferences().getControllerClassName(); 
        try {
            Class<?> clazz = Class.forName(className); 
            try {
            	Constructor<MailController> ctor = (Constructor<MailController>)clazz.getConstructor(); 
            	return ctor.newInstance(); 
            } catch (InvocationTargetException e) {
            	throw new RuntimeException(
                		className + " could not be instantiated", e);
            } catch (NoSuchMethodException e) { 
            	LOG.warn(className + " has no method: contructor(Context)", e); 
            }
            return (MailController)clazz.newInstance(); 
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(
            		className + " could not be loaded", ex);
        } catch (InstantiationException ex) {
            throw new RuntimeException(
            		className + " could not be instantiated", ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(
            		className + " could not be instantiated", ex);
        }
	}
    
    private final MailProvider mMailProvider; 
    
	protected MailController() { 
		mMailProvider = createMailProvider(); 
	}
	
	protected abstract MailProvider createMailProvider(); 
	
	protected MessagingQueue getMessagingQueue() { 
		return MessagingQueue.getInstance(); 
	}
	
	public final MessageActionQueue getMessageActionQueue() { 
		return MessageActionQueue.getInstance(); 
	}
	
	public final void registerListener(MessagingListener listener) {
		getMessagingQueue().registerListener(listener);
    }

    public final void unregisterListener(MessagingListener listener) {
    	getMessagingQueue().unregisterListener(listener);
    }
	
    public final MessagingEvent getLastEvent() { 
    	return getMessagingQueue().getLastEvent(); 
    }
    
	/**
     * Search the list of known Email providers looking for one that matches the user's email
     * domain.  We check for vendor supplied values first, then we look in providers_product.xml,
     * and finally by the entries in platform providers.xml.  This provides a nominal override
     * capability.
     *
     * A match is defined as any provider entry for which the "domain" attribute matches.
     *
     * @param domain The domain portion of the user's email address
     * @return suitable Provider definition, or null if no match found
     */
	public final MailProvider.Provider findProviderForDomain(String domain) { 
		return mMailProvider.findProviderForDomain(domain); 
	}
	
	/**
     * Request a remote update of mailboxes for an account.
     *
     * TODO: Clean up threading in MessagingController cases (or perhaps here in Controller)
     */
    public final void synchronizeFolders(final long accountId, final MessagingCallback callback) {
    	final MailContentProvider provider = MailContentProvider.getInstance(); 
		
    	final AccountContent account = provider.queryAccount(accountId); 
    	if (account == null) { 
    		if (LOG.isDebugEnabled()) 
    			LOG.debug("synchronizeFolders: account: "+accountId+" not found"); 
    		return; 
    	}
    	
    	final CallbackMessagingListener listener = new CallbackMessagingListener(this, 
    			MessagingQueue.COMMAND_SYNCHRONIZEFOLDERS, account, callback); 
    	
    	getMessagingQueue().putCommand(listener, new Runnable() {
	    		public void run() { 
	    			listener.callbackStarted(); 
	    			try {
	    				synchronizeFoldersSynchronous(account, listener); 
	                    
	                } catch (MessagingException ex) {
	                	MessageActionException me = 
	                			(ex instanceof MessageActionException) ? (MessageActionException)ex : 
	                				new MessageActionException(ex.toString(), ex);
	                	me.setAccountKey(accountId); 
	                	
	                    listener.callbackException(me); 
	                } catch (NullPointerException ne) {
	                	MessageActionException me = new MessageActionException(ne.toString(), ne); 
	                	me.setAccountKey(accountId); 
	                	
	                	listener.callbackException(me); 
	                } finally { 
	                	listener.callbackFinished(); 
	                }
	    		}
	    	}); 
    }
	
    public final void synchronizeActions(final long accountId, final MessagingCallback callback) {
    	final MailContentProvider provider = MailContentProvider.getInstance(); 
		
    	final AccountContent account = provider.queryAccount(accountId); 
    	if (account == null) { 
    		if (LOG.isDebugEnabled()) 
    			LOG.debug("synchronizeActions: account: "+accountId+" not found"); 
    		return; 
    	}
    	
    	final CallbackMessagingListener listener = new CallbackMessagingListener(this, 
    			MessagingQueue.COMMAND_SYNCHRONIZEACTIONS, account, callback); 
    	
    	getMessagingQueue().putCommand(listener, new Runnable() {
	    		public void run() { 
	    			listener.callbackStarted(); 
	    			try {
	    				synchronizeActionsSynchronous(account, listener); 
	                    
	                } catch (MessagingException ex) {
	                	MessageActionException me = 
	                			(ex instanceof MessageActionException) ? (MessageActionException)ex : 
	                				new MessageActionException(ex.toString(), ex);
	                	me.setAccountKey(accountId); 
	                	
	                    listener.callbackException(me); 
	                } catch (NullPointerException ne) {
	                	MessageActionException me = new MessageActionException(ne.toString(), ne); 
	                	me.setAccountKey(accountId); 
	                	
	                	listener.callbackException(me); 
	                } finally { 
	                	listener.callbackFinished(); 
	                }
	    		}
	    	}); 
    }
    
	/**
     * Request a remote update of a mailbox.  For use by the timed service.
     *
     * Functionally this is quite similar to updateMailbox(), but it's a separate API and
     * separate callback in order to keep UI callbacks from affecting the service loop.
     */
    public final void checkMail(final long accountId, final MessagingCallback callback) {
    	final MailContentProvider provider = MailContentProvider.getInstance(); 
		
		final AccountContent account = provider.queryAccount(accountId); 
		if (account == null) { 
			if (LOG.isDebugEnabled()) 
    			LOG.debug("checkMail: account: "+accountId+" not found"); 
			return; 
		}
		
        final CallbackMessagingListener listener = new CallbackMessagingListener(this, 
        		MessagingQueue.COMMAND_CHECKMAIL, account, callback); 
        
        // Put this on the queue as well so it follows listFolders
        getMessagingQueue().putCommand(listener, new Runnable() {
	    		public void run() { 
	    			listener.callbackStarted(); 
	    			try { 
	    				checkMailSynchronous(account, listener); 
	
	                } catch (MessagingException ex) {
	                	MessageActionException me = 
	                			(ex instanceof MessageActionException) ? (MessageActionException)ex : 
	                				new MessageActionException(ex.toString(), ex);
	                	me.setAccountKey(accountId); 
	                	
	                    listener.callbackException(me); 
	                } catch (NullPointerException ne) {
	                	MessageActionException me = new MessageActionException(ne.toString(), ne); 
	                	me.setAccountKey(accountId); 
	                	
	                	listener.callbackException(me); 
	                } finally { 
	                	listener.callbackFinished(); 
	                }
	    		}
	        }); 
    }
	
    protected boolean updateMailboxVisibleLimit(final long accountId, final long mailboxId, boolean more) { 
    	final MailContentProvider provider = MailContentProvider.getInstance(); 
		
		final AccountContent account = provider.queryAccount(accountId); 
    	final MailboxContent mailbox = provider.queryMailbox(mailboxId);
    	
    	if (account != null && mailbox != null) { 
    		final int totalCount = mailbox.getTotalCount(); 
    		final int totalSyncCount = mailbox.getTotalSyncCount(); 
    		
    		final StoreInfo info = StoreInfo.getStoreInfo(account.getStoreHostAuth().toUri());
			
			int visibleLimitIncrement = info.getVisibleLimitIncrement();
			int visibleLimitDefault = info.getVisibleLimitDefault();
			if (visibleLimitIncrement <= 0) 
				visibleLimitIncrement = Constants.VISIBLE_LIMIT_INCREMENT;
			if (visibleLimitDefault <= 0) 
				visibleLimitDefault = Constants.VISIBLE_LIMIT_DEFAULT;
    		
    		int visibleLimit = mailbox.getVisibleLimit();
    		
    		if (!more) { 
    			if (visibleLimit > totalSyncCount) { 
    				visibleLimit = ((int)(totalSyncCount / visibleLimitIncrement)) * visibleLimitIncrement; 
    				
    				if (visibleLimit <= 0) 
        				visibleLimit = visibleLimitDefault; 
        			if (visibleLimit > totalCount) 
        				visibleLimit = totalCount; 
    			}
    			
    		} else if (totalCount > totalSyncCount && totalCount > 0) { 
    			visibleLimit = ((int)(totalSyncCount / visibleLimitIncrement) + 1) * visibleLimitIncrement; 
    			if (visibleLimit <= totalSyncCount) 
    				visibleLimit = totalSyncCount + visibleLimitIncrement; 
    			
    			if (visibleLimit <= 0) 
    				visibleLimit = visibleLimitDefault; 
    			if (visibleLimit > totalCount) 
    				visibleLimit = totalCount; 
    		}
    		
    		if (visibleLimit > 0 && visibleLimit != mailbox.getVisibleLimit()) { 
				MailboxContent mailboxUpdate = mailbox.startUpdate();
				mailboxUpdate.setVisibleLimit(visibleLimit); 
				mailboxUpdate.commitUpdates(); 
				
				return true;
			}
    	}
    	
    	return false;
    }
    
    public final void synchronizeMailboxMore(final long accountId, final long mailboxId, final MessagingCallback callback) { 
    	if (updateMailboxVisibleLimit(accountId, mailboxId, true)) 
    		synchronizeMailboxInternal(accountId, mailboxId, callback); 
    }
    
    public final void synchronizeMailbox(final long accountId, final long mailboxId, final MessagingCallback callback) { 
    	updateMailboxVisibleLimit(accountId, mailboxId, false); 
    	synchronizeMailboxInternal(accountId, mailboxId, callback); 
    }
    
    /**
     * Start background synchronization of the specified folder.
     * @param account
     * @param folder
     * @param listener
     */
	private final void synchronizeMailboxInternal(final long accountId, final long mailboxId, final MessagingCallback callback) { 
		final MailContentProvider provider = MailContentProvider.getInstance(); 
		
		final AccountContent account = provider.queryAccount(accountId); 
		if (account == null) { 
			if (LOG.isDebugEnabled()) 
    			LOG.debug("synchronizeMailbox: account: "+accountId+" not found"); 
			return; 
		}
		
        final CallbackMessagingListener listener = new CallbackMessagingListener(this, 
        		MessagingQueue.COMMAND_SYNCHRONIZEMAILBOX, account, callback); 
        
        listener.setMailboxId(mailboxId); 
        
        getMessagingQueue().putCommand(listener, new Runnable() {
	            public void run() {
	            	listener.callbackStarted(); 
	    			try {
	    				synchronizeMailboxSynchronous(account, mailboxId, listener);
	    				
	    			} catch (MessagingException ex) {
	    				MessageActionException me = 
	                			(ex instanceof MessageActionException) ? (MessageActionException)ex : 
	                				new MessageActionException(ex.toString(), ex);
	                	me.setAccountKey(accountId); 
	                	me.setMailboxKey(mailboxId); 
	                	
	                    listener.callbackException(ex); 
	                } catch (NullPointerException ne) {
	                	MessageActionException me = new MessageActionException(ne.toString(), ne); 
	                	me.setAccountKey(accountId); 
	                	me.setMailboxKey(mailboxId); 
	                	
	                	listener.callbackException(me); 
	                } finally { 
	                	listener.callbackFinished(); 
	                }
	            }
	        });
	}
    
    /**
     * Request that any final work necessary be done, to load a message.
     *
     * Note, this assumes that the caller has already checked message.mFlagLoaded and that
     * additional work is needed.  There is no optimization here for a message which is already
     * loaded.
     *
     * @param messageId the message to load
     * @param callback the Controller callback by which results will be reported
     */
    public final void fetchMessageBody(final long accountId, final long messageId, final MessagingCallback callback) {
    	final MailContentProvider provider = MailContentProvider.getInstance(); 
		
    	final AccountContent account = provider.queryAccount(accountId); 
    	if (account == null) { 
    		if (LOG.isDebugEnabled()) 
    			LOG.debug("fetchMessageBody: account: "+accountId+" not found"); 
    		return; 
    	}
    	
    	final CallbackMessagingListener listener = new CallbackMessagingListener(this, 
    			MessagingQueue.COMMAND_FETCHMESSAGEBODY, account, callback); 
    	
    	listener.setMessageId(messageId); 
    	
    	getMessagingQueue().putCommand(listener, new Runnable() {
	            public void run() {
	            	listener.callbackStarted(); 
	    			try { 
	    				fetchMessageBodySynchronous(account, messageId, listener); 
		            	
	                } catch (MessagingException ex) {
	                	MessageActionException me = 
	                			(ex instanceof MessageActionException) ? (MessageActionException)ex : 
	                				new MessageActionException(ex.toString(), ex);
	                	me.setAccountKey(accountId); 
	                	me.setMessageKey(messageId); 
	                	
	                    listener.callbackException(ex); 
	                } catch (NullPointerException ne) {
	                	MessageActionException me = new MessageActionException(ne.toString(), ne); 
	                	me.setAccountKey(accountId); 
	                	me.setMessageKey(messageId); 
	                	
	                	listener.callbackException(me); 
	                } finally { 
	                	listener.callbackFinished(); 
	                }
	            }
	        });
    }
    
    /**
     * Request that any final work necessary be done, to load a message attachment.
     *
     * Note, this assumes that the caller has already checked message.mFlagLoaded and that
     * additional work is needed.  There is no optimization here for a message which is already
     * loaded.
     *
     * @param messageId the message to load
     * @param attachmentId the message attachment to load
     * @param callback the Controller callback by which results will be reported
     */
    public final void fetchMessageAttachment(final long accountId, final long messageId, final long attachmentId, 
    		final MessagingCallback callback) {
    	final MailContentProvider provider = MailContentProvider.getInstance(); 
		
    	final AccountContent account = provider.queryAccount(accountId); 
    	if (account == null) { 
    		if (LOG.isDebugEnabled()) 
    			LOG.debug("fetchMessageAttachment: account: "+accountId+" not found"); 
    		return; 
    	}
    	
    	final CallbackMessagingListener listener = new CallbackMessagingListener(this, 
    			MessagingQueue.COMMAND_FETCHMESSAGEATTACHMENT, account, callback); 
    	
    	listener.setMessageId(messageId); 
    	listener.setAttachmentId(attachmentId); 
    	
    	getMessagingQueue().putCommand(listener, new Runnable() {
	            public void run() {
	            	listener.callbackStarted(); 
	    			try { 
	    				fetchMessageAttachmentSynchronous(account, messageId, attachmentId, listener); 
		            	
	                } catch (MessagingException ex) {
	                	MessageActionException me = 
	                			(ex instanceof MessageActionException) ? (MessageActionException)ex : 
	                				new MessageActionException(ex.toString(), ex);
	                	me.setAccountKey(accountId); 
	                	me.setMessageKey(messageId); 
	                	me.setAttachmentKey(attachmentId); 
	                	
	                    listener.callbackException(ex); 
	                } catch (NullPointerException ne) {
	                	MessageActionException me = new MessageActionException(ne.toString(), ne); 
	                	me.setAccountKey(accountId); 
	                	me.setMessageKey(messageId); 
	                	me.setAttachmentKey(attachmentId); 
	                	
	                	listener.callbackException(me); 
	                } finally { 
	                	listener.callbackFinished(); 
	                }
	            }
	        });
    }
    
    /**
     * Send a message:
     * - move the message to Outbox (the message is assumed to be in Drafts).
     * - EAS service will take it from there
     * - trigger send for POP/IMAP
     * @param messageId the id of the message to send
     */
    public final void sendMessage(final long accountId, final long messageId, final MessagingCallback callback) {
    	final MailContentProvider provider = MailContentProvider.getInstance(); 
		
    	final AccountContent account = provider.queryAccount(accountId); 
    	if (account == null) { 
    		if (LOG.isDebugEnabled()) 
    			LOG.debug("sendMessage: account: "+accountId+" not found"); 
    		return; 
    	}
    	
    	final CallbackMessagingListener listener = new CallbackMessagingListener(this, 
    			MessagingQueue.COMMAND_SENDMESSAGE, account, callback); 
    	
    	listener.setMessageId(messageId); 
    	
    	getMessagingQueue().putCommand(listener, new Runnable() {
	            public void run() {
	            	listener.callbackStarted(); 
	    			try { 
	    				sendMessageSynchronous(account, messageId, listener); 
		            	
	                } catch (MessagingException ex) {
	                	MessageActionException me = 
	                			(ex instanceof MessageActionException) ? (MessageActionException)ex : 
	                				new MessageActionException(ex.toString(), ex);
	                	me.setAccountKey(accountId); 
	                	me.setMessageKey(messageId); 
	                	
	                    listener.callbackException(ex); 
	                } catch (NullPointerException ne) {
	                	MessageActionException me = new MessageActionException(ne.toString(), ne); 
	                	me.setAccountKey(accountId); 
	                	me.setMessageKey(messageId); 
	                	
	                	listener.callbackException(me); 
	                } finally { 
	                	listener.callbackFinished(); 
	                }
	            }
	        });
    }
    
    protected abstract void checkMailSynchronous(final AccountContent account, 
			final CallbackMessagingListener listener) throws MessagingException; 
    
    protected abstract void synchronizeFoldersSynchronous(final AccountContent account, 
			final CallbackMessagingListener listener) throws MessagingException; 
    
    protected abstract void synchronizeActionsSynchronous(final AccountContent account, 
			final CallbackMessagingListener listener) throws MessagingException; 
    
    protected abstract void synchronizeMailboxSynchronous(final AccountContent account, final long mailboxId, 
    		final CallbackMessagingListener listener) throws MessagingException; 
    
    protected abstract void fetchMessageBodySynchronous(final AccountContent account, final long messageId, 
    		final CallbackMessagingListener listener) throws MessagingException; 
    
    protected abstract void fetchMessageAttachmentSynchronous(final AccountContent account, final long messageId, final long attachmentId, 
    		final CallbackMessagingListener listener) throws MessagingException; 
    
    protected abstract void sendMessageSynchronous(final AccountContent account, final long messageId, 
    		final CallbackMessagingListener listener) throws MessagingException; 
    
}
