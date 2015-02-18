package org.javenstudio.android.mail.content;

public class LocalMessageInfo {

	private final long mId;
	private final boolean mFlagRead;
	private final boolean mFlagFavorite;
	private final boolean mFlagAnswered;
	private final int mFlagLoaded;
	private final String mServerId;
    
	public LocalMessageInfo(MessageContent message) { 
		mId = message.getId(); 
		mFlagRead = message.getFlagRead(); 
		mFlagFavorite = message.getFlagFavorite(); 
		mFlagAnswered = message.getFlagAnswered(); 
		mFlagLoaded = message.getFlagLoaded(); 
		mServerId = message.getServerId(); 
	}
	
	public long getId() { return mId; } 
	public boolean getFlagRead() { return mFlagRead; } 
	public boolean getFlagFavorite() { return mFlagFavorite; } 
	public boolean getFlagAnswered() { return mFlagAnswered; } 
	public int getFlagLoaded() { return mFlagLoaded; } 
	public String getServerId() { return mServerId; } 
	
}
