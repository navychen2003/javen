package org.javenstudio.android.mail.controller;

import java.util.HashMap;
import java.util.HashSet;

import org.javenstudio.mail.Folder;
import org.javenstudio.mail.MessagingException;
import org.javenstudio.mail.store.Store;

import org.javenstudio.android.mail.Constants;
import org.javenstudio.android.mail.content.AccountContent;
import org.javenstudio.android.mail.content.LocalMailboxInfo;
import org.javenstudio.android.mail.content.MailContentProvider;
import org.javenstudio.android.mail.content.MailboxContent;
import org.javenstudio.common.util.EntityIterable;
import org.javenstudio.common.util.Logger;

public class MessagingSynchronizeFolders {
	private static Logger LOG = Logger.getLogger(MessagingSynchronizeFolders.class);

	public static int synchronizeFolders(final MessagingController controller, 
			final AccountContent account) throws MessagingException {
		final MailContentProvider provider = MailContentProvider.getInstance(); 
		
		// Step 1:  Get remote folders, make a list, and add any local folders
        // that don't already exist.
		
		Store store = Store.getInstance(account.getStoreHostAuth().toUri());
		Folder[] remoteFolderList = store.getPersonalNamespaces();
		
		HashMap<String, Folder> remoteFolders = new HashMap<String, Folder>();
		HashSet<String> remoteFolderNames = new HashSet<String>();
		
        for (int i = 0, count = remoteFolderList.length; i < count; i++) {
        	final Folder folder = remoteFolderList[i]; 
        	if (folder == null) continue; 
        	
        	final String folderName = controller.normalizeFolderName(folder.getName()); 
        	if (folderName == null || folderName.length() == 0) 
        		continue;
        	
        	remoteFolders.put(folderName, folder); 
            remoteFolderNames.add(folderName); 
            
            if (LOG.isDebugEnabled()) { 
    			LOG.debug("synchronizeFolders:" + 
    					" remote folder: "+folderName); 
            }
        }
        
        HashMap<String, LocalMailboxInfo> localFolders = new HashMap<String, LocalMailboxInfo>();
        HashSet<String> localFolderNames = new HashSet<String>();
        
        EntityIterable<MailboxContent> mailboxIt = provider.queryMailboxes(account.getId()); 
        try { 
            while (mailboxIt.hasNext()) { 
            	MailboxContent mailbox = mailboxIt.next(); 
            	if (mailbox == null) continue; 
            	
            	LocalMailboxInfo info = new LocalMailboxInfo(mailbox); 
            	String folderName = controller.normalizeFolderName(info.getDisplayName());
            	if (folderName == null || folderName.length() == 0) 
            		continue;
            	
            	localFolders.put(folderName, info);
                localFolderNames.add(folderName);
                
                //if (LOG.isDebugEnabled()) { 
	    		//	LOG.debug("synchronizeFolders:" + 
                //			" local folder: "+info.getDisplayName()); 
                //}
            }
        } finally { 
        	mailboxIt.close(); 
        }
		
        // Short circuit the rest if the sets are the same (the usual case)
        int newFolders = 0; 
        
        if (!remoteFolderNames.equals(localFolderNames)) {
        	// They are different, so we have to do some adds and drops
        	
        	// Drops first, to make things smaller rather than larger
            HashSet<String> localsToDrop = new HashSet<String>(localFolderNames);
            localsToDrop.removeAll(remoteFolderNames);
            
            for (String localNameToDrop : localsToDrop) {
            	LocalMailboxInfo localInfo = localFolders.get(localNameToDrop);
            	if (localInfo == null) 
            		continue;
            	
                // Exclusion list - never delete local special folders, irrespective
                // of server-side existence.
            	switch (localInfo.getType()) {
            	case MailboxContent.TYPE_INBOX:
                case MailboxContent.TYPE_DRAFTS:
                case MailboxContent.TYPE_OUTBOX:
                case MailboxContent.TYPE_SENT:
                case MailboxContent.TYPE_TRASH:
                    break;
                default:
                	// Drop all attachment files related to this mailbox
                	// provider.deleteAllMailboxAttachmentFiles(account.getId(), localInfo.getId()); 
                	// Delete the mailbox.  Triggers will take care of
                    // related Message, Body and Attachment records. 
                	provider.deleteMailbox(account.getId(), localInfo.getId()); 
                	break; 
            	}
            }
        	
            // Now do the adds
            remoteFolderNames.removeAll(localFolderNames);
            
            for (String remoteNameToAdd : remoteFolderNames) {
            	Folder folder = remoteFolders.get(remoteNameToAdd); 
            	if (folder == null) 
            		continue; 
            	
            	MailboxContent mailbox = provider.newMailboxContent(account.getId()); 
            	
            	mailbox.setDisplayName(folder.getName()); 
            	mailbox.setAccountKey(account.getId()); 
            	mailbox.setType(provider.inferMailboxTypeFromName(account, folder.getName())); 
            	mailbox.setFlagVisible(true); 
            	if (mailbox.getVisibleLimit() <= 0) 
            		mailbox.setVisibleLimit(Constants.VISIBLE_LIMIT_DEFAULT); 
            	
            	mailbox.commitUpdates(); 
            	
            	newFolders ++; 
            }
        }
        
        return newFolders; 
	}
	
}
