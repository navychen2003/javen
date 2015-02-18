package org.javenstudio.android.entitydb.content;

public interface FetchData {

	public static final int STATUS_OK = 0;
	public static final int STATUS_DIRTY = 1;
	
	public long getId();
	
	public String getContentName();
	public String getContentUri();
	public String getContentType();
	public String getPrefix();
	public String getAccount();
	public String getEntryId();
	public int getEntryType();
	public int getTotalResults();
	public int getStartIndex();
	public int getItemsPerPage();
	public int getStatus();
	public int getFailedCode();
	public String getFailedMessage();
	public long getCreateTime();
	public long getUpdateTime();
	
	public FetchData startUpdate();
	public long commitUpdates();
	public void commitDelete();
	
	public void setContentName(String text);
	public void setContentUri(String text);
	public void setContentType(String text);
	public void setPrefix(String text);
	public void setAccount(String text);
	public void setEntryId(String text);
	public void setEntryType(int type); 
	public void setTotalResults(int results);
	public void setStartIndex(int index);
	public void setItemsPerPage(int items);
	public void setStatus(int status);
	public void setFailedCode(int code);
	public void setFailedMessage(String text);
	public void setCreateTime(long time);
	public void setUpdateTime(long time);
	
}
