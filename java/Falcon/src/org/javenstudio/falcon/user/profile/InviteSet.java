package org.javenstudio.falcon.user.profile;

import org.javenstudio.falcon.user.IInviteSet;

public class InviteSet implements IInviteSet {

	private final Invite[] mInvites;
	private final int mTotalCount;
	private final int mStart;
	private final long mUpdateTime;
	private int mIndex = 0;
	
	public InviteSet(Invite[] invites, long updateTime) {
		this(invites, invites.length, 0, updateTime);
	}
	
	public InviteSet(Invite[] invites, 
			int totalCount, int start, long updateTime) {
		mInvites = invites;
		mTotalCount = totalCount;
		mStart = start;
		mUpdateTime = updateTime;
	}
	
	@Override
	public long getUpdateTime() {
		return mUpdateTime;
	}
	
	@Override
	public int getTotalCount() {
		return mTotalCount;
	}
	
	@Override
	public int getStart() {
		return mStart;
	}
	
	@Override
	public Invite[] getInvites() {
		return mInvites;
	}
	
	@Override
	public synchronized void first() {
		mIndex = 0;
	}
	
	@Override
	public synchronized Invite next() {
		if (mInvites != null && mInvites.length > 0) {
			if (mIndex >= 0 && mIndex < mInvites.length) 
				return mInvites[mIndex++];
		}
		return null;
	}
	
}
