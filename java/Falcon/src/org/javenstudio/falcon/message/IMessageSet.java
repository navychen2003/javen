package org.javenstudio.falcon.message;

public interface IMessageSet {

	public int getTotalCount();
	public int getStart();
	
	public IMessage[] getMessages();
	public long getUpdateTime();
	
	public void first();
	public IMessage next();
	
}
