package org.javenstudio.falcon.user;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.IdentityUtils;

public abstract class UserClient implements IUserClient {
	private static final Logger LOG = Logger.getLogger(UserClient.class);

	private final Member mUser;
	private final String mKey;
	private final long mLoginTime;
	protected String mToken;
	
	private volatile long mUpdateTime;
	private volatile long mAccessTime;
	
	public UserClient(Member user, String token) throws ErrorException { 
		if (user == null || token == null) throw new NullPointerException();
		mUser = user;
		mLoginTime = System.currentTimeMillis();
		mKey = UserHelper.newClientKey(user.getUserName()+"@"+mLoginTime);
		mToken = token;
		mUpdateTime = mLoginTime;
		mAccessTime = mLoginTime;
	}
	
	@Override
	public Member getUser() {
		return mUser;
	}
	
	@Override
	public synchronized void logout() { 
		if (LOG.isDebugEnabled()) LOG.debug("logout");
		try {
			getUser().removeClient(this, true);
		} catch (Throwable e) { 
			if (LOG.isErrorEnabled())
				LOG.error("logout: error: " + e, e);
		}
	}
	
	@Override
	public String getClientId() {
		return IdentityUtils.toIdentity(mUser.getUserKey() + mKey, 32);
	}

	@Override
	public String getClientKey() {
		return mKey;
	}

	@Override
	public synchronized String getToken() {
		return mToken;
	}

	@Override
	public long getLoginTime() {
		return mLoginTime;
	}

	@Override
	public long getUpdateTime() {
		return mUpdateTime;
	}

	public void setUpdateTime(long time) { 
		mUpdateTime = time;
	}
	
	@Override
	public long getAccessTime() { 
		return mAccessTime;
	}
	
	public void setAccessTime(long time) { 
		mAccessTime = time;
	}
	
	synchronized String changeToken() throws ErrorException {
		mToken = UserHelper.newClientToken(getUser().getUserName());
		return mToken;
	}
	
	@Override
	public synchronized void refreshToken() throws ErrorException { 
		if (LOG.isDebugEnabled()) LOG.debug("refreshToken");
		getUser().refreshToken(this);
	}
	
	@Override
	public synchronized void close() { 
		if (LOG.isDebugEnabled()) LOG.debug("close");
		try {
			getUser().removeClient(this, false);
		} catch (Throwable e) { 
			if (LOG.isErrorEnabled())
				LOG.error("close: error: " + e, e);
		}
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{user=" + getUser().getUserName() 
				+ ",token=" + mToken + "}";
	}
	
}
