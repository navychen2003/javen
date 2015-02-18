package org.javenstudio.falcon.message;

public interface IMessageQuery {

	public static enum GroupBy {
		STREAM
	}
	
	public String[] getFolderNames();
	public String getChatUser();
	public String getStreamId();
	public String getStatus();
	public String getFlag();
	public String getAccount();
	
	public long getResultStart();
	public int getResultCount();
	
	public long getTimeStart();
	public long getTimeEnd();
	
	public int getRowSize();
	public int getGroupRowSize();
	
	public GroupBy getGroupBy();
	
}
