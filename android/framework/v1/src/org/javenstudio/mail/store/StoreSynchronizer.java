package org.javenstudio.mail.store;

import org.javenstudio.mail.MessagingException;
import org.javenstudio.mail.content.MailboxData;

/**
 * This interface allows a store to define a completely different synchronizer algorithm,
 * as necessary.
 */
public interface StoreSynchronizer {

	/**
     * An object of this class is returned by SynchronizeMessagesSynchronous to report
     * the results of the sync run.
     */
    public static class SyncResults {
        /**
         * The total # of messages in the remote folder
         */
    	private final int mTotalMessages;
    	
        /**
         * The # of unread messages in the remote folder
         */
        private final int mUnreadMessages;
        
        /**
         * The # of new messages in the folder
         */
        private final int mNewSyncMessages;
        
        /**
         * The # of total messages in the folder
         */
        private final int mTotalSyncMessages;
        
        public SyncResults(int totalMessages, int unreadMessages, int totalSyncMessages, int newSyncMessages) {
            mTotalMessages = totalMessages;
            mUnreadMessages = unreadMessages; 
            mNewSyncMessages = newSyncMessages;
            mTotalSyncMessages = totalSyncMessages; 
        }
        
        public int getTotalMessages() { 
        	return mTotalMessages; 
        }
        
        public int getUnreadMessages() { 
        	return mUnreadMessages; 
        }
        
        public int getNewSyncMessages() { 
        	return mNewSyncMessages; 
        }
        
        public int getTotalSyncMessages() { 
        	return mTotalSyncMessages; 
        }
    }
	
    /**
     * The job of this method is to synchronize messages between a remote folder and the
     * corresponding local folder.
     * 
     * The following callbacks should be called during this operation:
     *  {@link MessagingListener#synchronizeMailboxNewMessage(Account, String, Message)}
     *  {@link MessagingListener#synchronizeMailboxRemovedMessage(Account, String, Message)}
     *  
     * Callbacks (through listeners) *must* be synchronized on the listeners object, e.g.
     *   synchronized (listeners) {
     *       for(MessagingListener listener : listeners) {
     *           listener.synchronizeMailboxNewMessage(account, folder, message);
     *       }
     *   }
     *
     * @param account The account to synchronize
     * @param folder The folder to synchronize
     * @param listeners callbacks to make during sync operation
     * @param context if needed for making system calls
     * @return an object describing the sync results
     */
    public SyncResults synchronizeMessagesSynchronous(MailboxData folder) throws MessagingException;
    
}
