package org.javenstudio.android.mail.content;

public interface AccountContent {

	public final static int FLAGS_NOTIFY_NEW_MAIL = 1;
    public final static int FLAGS_VIBRATE_ALWAYS = 2;
    public static final int FLAGS_DELETE_POLICY_MASK = 4+8;
    public static final int FLAGS_DELETE_POLICY_SHIFT = 2;
    public static final int FLAGS_INCOMPLETE = 16;
    public static final int FLAGS_SECURITY_HOLD = 32;
    public static final int FLAGS_VIBRATE_WHEN_SILENT = 64;
    public static final int FLAGS_MSG_LIST_ON_DELETE = 128;
    public static final int FLAGS_DEFAULT_FOLDER_LIST = 256;
    public static final int FLAGS_SIGNATURE_TOGGLE = 512;

    public static final int DELETE_POLICY_NEVER = 0;
    public static final int DELETE_POLICY_7DAYS = 1;        // not supported
    public static final int DELETE_POLICY_ON_DELETE = 2;

    // Sentinel values for the mSyncInterval field of both Account records
    public static final int CHECK_INTERVAL_NEVER = -1;
    public static final int CHECK_INTERVAL_PUSH = -2;
	
    
    public HostAuthContent getStoreHostAuth(); 
    public HostAuthContent getSenderHostAuth(); 
    
    public long getId(); 
    public boolean isDefaultAccount(); 
    public boolean isSaved(); 
    
    public String getEmailAddress(); 
    public String getDisplayName(); 
    public String getSenderName(); 
    public String getSignature(); 
    public String getRingtone(); 
    
    public int getSyncInterval(); 
    public int getFlags(); 
    public int getDeletePolicy(); 
    
    public AccountContent startUpdate(); 
    public void commitUpdates(); 
    public void commitDelete(); 
    
    public void setDefaultAccount(boolean value); 
    public void setEmailAddress(String value); 
    public void setDisplayName(String value); 
    public void setSenderName(String value); 
    public void setSignature(String value); 
    public void setSyncInterval(int value); 
    public void setSyncLookback(int value); 
    public void setRingtone(String value); 
    public void setFlags(int value); 
    public void setDeletePolicy(int newPolicy); 
    
}
