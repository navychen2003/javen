package org.javenstudio.falcon.user.profile;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.util.ILockable;

public class UserProfile extends Profile {

	private final IUser mUser;
	
	private final ILockable.Lock mLock =
		new ILockable.Lock() {
			@Override
			public ILockable.Lock getParentLock() {
				return UserProfile.this.getUser().getLock();
			}
			@Override
			public String getName() {
				return "Profile(" + UserProfile.this.getUser().getUserName() + ")";
			}
		};
	
	public UserProfile(IUser user, IProfileStore store) throws ErrorException { 
		super(user.getUserKey(), store);
		mUser = user;
		loadProfile(false);
	}
	
	@Override
	public IUser getUser() { return mUser; }
	
	@Override
	public ILockable.Lock getLock() { return mLock; }
	
}
