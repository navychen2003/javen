package org.javenstudio.android.mail.content;

import org.javenstudio.mail.Body;
import org.javenstudio.mail.MessagingException;
import org.javenstudio.mail.content.MailboxData;

import org.javenstudio.android.mail.Preferences;
import org.javenstudio.common.util.EntityIterable;

public abstract class MailContentProvider {

	private static MailContentProvider sInstance = null; 
	private static final Object sLock = new Object(); 
	
	public static final MailContentProvider getInstance() { 
		synchronized (sLock) {
			if (sInstance == null) sInstance = newInstance(); 
			return sInstance; 
		}
	}
	
	private static MailContentProvider newInstance() { 
		// Pull in the actual implementation of the TalkConfiguration at run-time
		final String className = Preferences.getPreferences().getContentProviderClassName(); 
		MailContentProvider instance; 
        try {
            Class<?> clazz = Class.forName(className);
            instance = (MailContentProvider)clazz.newInstance();
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
        return instance; 
	}
	
	public static String getMailboxDefaultName(int type) { 
    	switch (type) { 
		case MailboxContent.TYPE_INBOX: 
			return MailboxData.NAME_INBOX;
		case MailboxContent.TYPE_OUTBOX: 
			return MailboxData.NAME_OUTBOX;
		case MailboxContent.TYPE_DRAFTS: 
			return MailboxData.NAME_DRAFTS;
		case MailboxContent.TYPE_SENT: 
			return MailboxData.NAME_SENT;
		case MailboxContent.TYPE_JUNK: 
			return MailboxData.NAME_JUNK;
		case MailboxContent.TYPE_TRASH: 
			return MailboxData.NAME_TRASH;
		}
    	return null;
    }
	
	protected MailContentProvider() {}
	
	public abstract AccountContent queryAccount(long accountId); 
	public abstract EntityIterable<AccountContent> queryAccounts(); 
	public abstract void deleteAccount(long accountId) throws MessagingException; 
	
	public abstract MailboxContent newMailboxContent(long accountId); 
	public abstract MailboxContent queryMailbox(long mailboxId); 
	public abstract EntityIterable<MailboxContent> queryMailboxes(long accountId); 
	public abstract MailboxContent queryMailboxWithType(long accountId, int type); 
	public abstract MailboxContent queryOrCreateMailboxWithType(long accountId, int type); 
	
	public abstract int inferMailboxTypeFromName(AccountContent account, String mailboxName); 
	
	public abstract EntityIterable<MessageContent> queryMessages(long accountId, long mailboxId);
	public abstract EntityIterable<MessageContent> queryMessagesWithOrderby(long accountId, long mailboxId, int orderby);
	public abstract EntityIterable<MessageContent> queryMessagesNoUid(long accountId, long mailboxId);
	public abstract MessageContent queryMessageWithUid(long accountId, long mailboxId, String uid);
	public abstract MessageContent newMessageContent(long accountId, long mailboxId); 
	public abstract MessageContent queryMessage(long messageId); 
	
	public abstract int countTotalMessages(long accountId, long mailboxId); 
	public abstract int countUnreadMessages(long accountId, long mailboxId); 
	
	public abstract void updateMessageFlags(long messageId, boolean flagRead, boolean flagFavorite, boolean flagAnswered); 
	public abstract void deleteMessage(long accountId, long messageId) throws MessagingException; 
	public abstract void deleteMailbox(long accountId, long mailboxId) throws MessagingException; 
	
	public abstract BodyContent newBodyContent(long messageId); 
	public abstract BodyContent queryBody(long bodyId); 
	public abstract BodyContent queryBodyWithMessageId(long messageId); 
	
	public abstract AttachmentContent newAttachmentContent(long messageId); 
	public abstract AttachmentContent queryAttachment(long attachmentId); 
	public abstract AttachmentContent[] queryMessageAttachments(long messageId); 
	public abstract EntityIterable<AttachmentContent> queryAttachments(long accountId, long messageId, int flag); 
	
	public abstract AttachmentFile saveAttachmentFile(MessageContent localMessage, Body body) throws MessagingException; 
	public abstract AttachmentFile openAttachmentFile(AttachmentContent attachment) throws MessagingException; 
	
}
