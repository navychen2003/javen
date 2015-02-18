package org.javenstudio.android.mail.content;

import org.javenstudio.mail.content.MessageData;

public interface MessageContent extends MessageData {

	// Bits used in mFlagMessage for \\Read and \\Flagged and \\Answered
    public static final int FLAG_MESSAGE_EMPTY = 0;
    public static final int FLAG_MESSAGE_READ = 1<<0;
    public static final int FLAG_MESSAGE_FAVORITE = 1<<1;
    public static final int FLAG_MESSAGE_ANSWERED = 1<<2;
    public static final int FLAG_MESSAGE_MASK = 
    	FLAG_MESSAGE_READ | FLAG_MESSAGE_FAVORITE | FLAG_MESSAGE_ANSWERED;

    // Values used in mFlagLoaded
    public static final int FLAG_LOADED_UNLOADED = 0;
    public static final int FLAG_LOADED_COMPLETE = 1;
    public static final int FLAG_LOADED_PARTIAL = 2;
    public static final int FLAG_LOADED_DELETED = 3;

    // Bits used in mFlags
    // The following three states are mutually exclusive, and indicate whether the message is an
    // original, a reply, or a forward
    public static final int FLAG_TYPE_ORIGINAL = 0;
    public static final int FLAG_TYPE_REPLY = 1<<0;
    public static final int FLAG_TYPE_FORWARD = 1<<1;
    public static final int FLAG_TYPE_MASK = FLAG_TYPE_REPLY | FLAG_TYPE_FORWARD;
    // The following flags indicate messages that are determined to be incoming meeting related
    // (e.g. invites from others)
    public static final int FLAG_INCOMING_MEETING_INVITE = 1<<2;
    public static final int FLAG_INCOMING_MEETING_CANCEL = 1<<3;
    public static final int FLAG_INCOMING_MEETING_MASK =
        FLAG_INCOMING_MEETING_INVITE | FLAG_INCOMING_MEETING_CANCEL;
    // The following flags indicate messages that are outgoing and meeting related
    // (e.g. invites TO others)
    public static final int FLAG_OUTGOING_MEETING_INVITE = 1<<4;
    public static final int FLAG_OUTGOING_MEETING_CANCEL = 1<<5;
    public static final int FLAG_OUTGOING_MEETING_ACCEPT = 1<<6;
    public static final int FLAG_OUTGOING_MEETING_DECLINE = 1<<7;
    public static final int FLAG_OUTGOING_MEETING_TENTATIVE = 1<<8;
    public static final int FLAG_OUTGOING_MEETING_MASK =
        FLAG_OUTGOING_MEETING_INVITE | FLAG_OUTGOING_MEETING_CANCEL |
        FLAG_OUTGOING_MEETING_ACCEPT | FLAG_OUTGOING_MEETING_DECLINE |
        FLAG_OUTGOING_MEETING_TENTATIVE;
    public static final int FLAG_OUTGOING_MEETING_REQUEST_MASK =
        FLAG_OUTGOING_MEETING_INVITE | FLAG_OUTGOING_MEETING_CANCEL;
    
	
	public long getId(); 
	
	public String getDisplayName(); 
	public long getTimeStamp(); 
	public String getSubject(); 
	
	public boolean getFlagRead(); 
	public boolean getFlagFavorite(); 
	public boolean getFlagAnswered(); 
	public boolean getFlagAttachment(); 
	public int getFlagLoaded(); 
	public int getFlags(); 
	
	public String getServerId(); 
	public long getServerTimeStamp(); 
	
	public String getClientId(); 
	public String getMessageId(); 
	
	public long getAccountKey(); 
	public long getMailboxKey(); 
	public int getMailboxType(); 
	
	public String getFrom(); 
	public String getTo(); 
	public String getCc(); 
	public String getBcc(); 
	public String getReplyTo(); 
	
	public String getMeetingInfo(); 
	public long getSize(); 
	
	public MessageContent startUpdate(); 
    public void commitUpdates(); 
    public void commitDelete(); 
	
    public void setAccountKey(long accountId); 
    public void setMailboxKey(long mailboxId); 
    public void setMailboxType(int type); 
    
    public void setDisplayName(String name); 
    public void setTimeStamp(long time); 
    public void setSubject(String subject); 
    
    public void setFlagRead(boolean read); 
    public void setFlagFavorite(boolean favorite); 
    public void setFlagAnswered(boolean answered); 
    public void setFlagAttachment(boolean attr); 
    public void setFlagLoaded(int loaded); 
    public void setFlags(int flags); 
    
    public void setServerId(String id); 
    public void setServerTimeStamp(long time); 
    
    public void setClientId(String id); 
    public void setMessageId(String id); 
    
    public void setFrom(String s); 
    public void setTo(String s); 
    public void setCc(String s); 
    public void setBcc(String s); 
    public void setReplyTo(String s); 
    
    public void setMeetingInfo(String s); 
    public void setSize(long size); 
    
}
