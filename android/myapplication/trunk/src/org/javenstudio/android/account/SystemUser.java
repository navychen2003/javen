package org.javenstudio.android.account;

import org.javenstudio.android.data.image.Image;

import android.accounts.Account;

public class SystemUser extends AccountUser implements AppUser {

	private final Account mAccount;
	private final String mSourceName;
	private String mAuthToken;
	
	public SystemUser(Account account, String sourceName) { 
		this(account, sourceName, null);
	}
	
	public SystemUser(Account account, String sourceName, String authToken) { 
		mAccount = account;
		mAuthToken = authToken;
		mSourceName = sourceName;
		
		if (account == null) 
			throw new NullPointerException("Account is null");
	}
	
	public Account getAccount() { return mAccount; }
	public long getAccountId() { return 0; }
	public String getAccountName() { return mAccount.name; }
	public String getAccountFullname() { return mAccount.name; }
	public String getSourceName() { return mSourceName; }
	
	public String getUserId() { return getAccountName(); }
	public String getUserTitle() { return getAccountName(); }
	public String getUserEmail() { return getAccountName(); }
	public String getMailAddress() { return getAccountName(); }
	
	public Image getAvatarImage() { return null; }
	public int getStatisticCount(int type) { return 0; }
	
	public String getAuthToken() { return mAuthToken; }
	public void setAuthToken(String token) { mAuthToken = token; }
	
	@Override
	public boolean equals(Object obj) { 
		if (obj == this) return true;
		if (obj == null || !(obj instanceof SystemUser)) 
			return false;
		
		SystemUser other = (SystemUser)obj;
		
		return this.mAccount.equals(other.mAccount) && 
				AccountRegistry.isEquals(this.mAuthToken, other.mAuthToken) && 
				AccountRegistry.isEquals(this.mSourceName, other.mSourceName);
	}
	
	@Override
	public String toString() { 
		return "Account:" + getAccountName();
	}
	
}
