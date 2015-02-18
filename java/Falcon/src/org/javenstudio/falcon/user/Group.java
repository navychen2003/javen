package org.javenstudio.falcon.user;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.profile.MemberManager;
import org.javenstudio.falcon.util.ILockable;

public class Group extends User implements IGroup {
	private static final Logger LOG = Logger.getLogger(Group.class);

	private MemberManager mMemberRef = null;
	
	Group(UserManager manager, IUserData data) throws ErrorException { 
		super(manager, data);
		
		if (LOG.isDebugEnabled())
			LOG.debug("create: group=" + this);
	}
	
	@Override
	public synchronized MemberManager getMemberManager() throws ErrorException {
		MemberManager manager = mMemberRef;
		if (manager == null || manager.isClosed()) {
			manager = new MemberManager(this, getUserManager().getStore()); 
			mMemberRef = manager;
		}
		return manager;
	}
	
	public synchronized MemberManager removeMemberManager() { 
		MemberManager manager = mMemberRef;
		mMemberRef = null;
		return manager;
	}
	
	@Override
	public synchronized void close() {
		if (LOG.isDebugEnabled()) LOG.debug("close");
		
		try {
			getLock().lock(ILockable.Type.WRITE, null);
			try {
				MemberManager mm = mMemberRef;
				if (mm != null) mm.close();
				mMemberRef = null;
				
				super.close();
			} finally { 
				getLock().unlock(ILockable.Type.WRITE);
			}
		} catch (ErrorException e) { 
			if (LOG.isWarnEnabled())
				LOG.warn("close: error: " + e, e);
		}
	}
	
}
