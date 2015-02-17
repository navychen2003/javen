package org.javenstudio.android.mail.content;

import org.javenstudio.mail.content.MailboxData;

public interface MailboxContent extends MailboxData {

	public static final long NO_MAILBOX = -1;

    // Sentinel values for the mSyncInterval field of both Mailbox records
    public static final int CHECK_INTERVAL_NEVER = -1;
    public static final int CHECK_INTERVAL_PUSH = -2;
    // The following two sentinel values are used by EAS
    // Ping indicates that the EAS mailbox is synced based on a "ping" from the server
    public static final int CHECK_INTERVAL_PING = -3;
    // Push-Hold indicates an EAS push or ping Mailbox shouldn't sync just yet
    public static final int CHECK_INTERVAL_PUSH_HOLD = -4;
	
	// Types of mailboxes.  The list is ordered to match a typical UI presentation, e.g.
    // placing the inbox at the top.
    // The "main" mailbox for the account, almost always referred to as "Inbox"
    // Arrays of "special_mailbox_display_names" and "special_mailbox_icons" are depends on
    // types Id of mailboxes.
    public static final int TYPE_INBOX = 1;
    // The local outbox associated with the Account
    public static final int TYPE_OUTBOX = 2;
    // Holds drafts
    public static final int TYPE_DRAFTS = 3;
    // Holds sent mail
    public static final int TYPE_SENT = 4;
    // Holds deleted mail
    public static final int TYPE_TRASH = 5;
    // Holds junk mail
    public static final int TYPE_JUNK = 6;
    // Holds mail (generic)
    public static final int TYPE_MAIL = 10;
    // Parent-only mailbox; holds no mail
    public static final int TYPE_PARENT = 11;
    
    // Types after this are used for non-mail mailboxes (as in EAS)
    public static final int TYPE_NOT_EMAIL = 0x40;
    public static final int TYPE_CALENDAR = 0x41;
    public static final int TYPE_CONTACTS = 0x42;
    public static final int TYPE_TASKS = 0x43;
    public static final int TYPE_EAS_ACCOUNT_MAILBOX = 0x44;

    // Bit field flags
    public static final int FLAG_HAS_CHILDREN = 1<<0;
    public static final int FLAG_CHILDREN_VISIBLE = 1<<1;
    public static final int FLAG_CANT_PUSH = 1<<2;
    
	
	public long getId(); 
	public String getDisplayName(); 
	public long getAccountKey(); 
	public int getType(); 
	public int getVisibleLimit(); 
	public int getNewSyncCount(); 
	public int getTotalSyncCount(); 
	public int getUnreadCount(); 
	public int getTotalCount(); 
	public long getSyncTime(); 
	
	public void setDisplayName(String name); 
	public void setAccountKey(long id); 
	public void setType(int type); 
	public void setFlagVisible(boolean visible); 
	public void setVisibleLimit(int limit); 
	public void setNewSyncCount(int count); 
	public void setTotalSyncCount(int count); 
	public void setUnreadCount(int count); 
	public void setTotalCount(int count); 
	public void setSyncTime(long time); 
	
	public MailboxContent startUpdate(); 
	public void commitUpdates(); 
	public void commitDelete(); 
	
}
