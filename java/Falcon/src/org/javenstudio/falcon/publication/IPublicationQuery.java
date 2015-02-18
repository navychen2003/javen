package org.javenstudio.falcon.publication;

public interface IPublicationQuery {

	public static enum GroupBy {
		STREAM
	}
	
	public String[] getChannelNames();
	public String getStreamId();
	public String getStatus();
	public String getFlag();
	public String getOwner();
	
	public long getResultStart();
	public int getResultCount();
	
	public long getTimeStart();
	public long getTimeEnd();
	
	public int getRowSize();
	public int getGroupRowSize();
	
	public GroupBy getGroupBy();
	
}
