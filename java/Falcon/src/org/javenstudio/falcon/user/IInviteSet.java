package org.javenstudio.falcon.user;

public interface IInviteSet {

	public int getTotalCount();
	public int getStart();
	
	public IInvite[] getInvites();
	public long getUpdateTime();
	
	public void first();
	public IInvite next();
	
}
