package org.javenstudio.android.mail.content;

public class LocalMailboxInfo {

	private final long mId;
    private final String mDisplayName;
    private final long mAccountKey;
    private final int mType;
    
    public LocalMailboxInfo(MailboxContent mailbox) { 
    	mId = mailbox.getId(); 
    	mDisplayName = mailbox.getDisplayName(); 
    	mAccountKey = mailbox.getAccountKey(); 
    	mType = mailbox.getType(); 
    }
    
    public long getId() { return mId; } 
    public String getDisplayName() { return mDisplayName; } 
    public long getAccountKey() { return mAccountKey; } 
    public int getType() { return mType; } 
    
}
