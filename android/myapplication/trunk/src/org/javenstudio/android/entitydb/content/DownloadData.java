package org.javenstudio.android.entitydb.content;

public interface DownloadData {

	public static final int STATUS_ADDED = 0;
	public static final int STATUS_PENDING = 1;
	public static final int STATUS_RUNNING = 2;
	public static final int STATUS_FINISHED = 3;
	public static final int STATUS_FAILED = 4;
	public static final int STATUS_ABORTED = 5;
	
	public static final int CODE_UNKNOWN = 5001;
	public static final int CODE_NOACCOUNT = 5404;
	
	public long getId();
	
	public String getContentName();
	public String getContentUri();
	public String getContentType();
	public String getSaveApp();
	public String getSavePath();
	public String getSourcePrefix();
	public String getSourceAccount();
	public String getSourceAccountId();
	public String getSourceFile();
	public String getSourceFileId();
	public int getStatus();
	public int getFailedCode();
	public int getFailedCount();
	public String getFailedMessage();
	public int getRetryAfter();
	public long getStartTime();
	public long getUpdateTime();
	public long getFinishTime();
	
	public DownloadData startUpdate();
	public long commitUpdates();
	public void commitDelete();
	
	public void setContentName(String text);
	public void setContentUri(String text);
	public void setContentType(String text);
	public void setSaveApp(String text);
	public void setSavePath(String text);
	public void setSourcePrefix(String text);
	public void setSourceAccount(String text);
	public void setSourceAccountId(String text);
	public void setSourceFile(String text);
	public void setSourceFileId(String text);
	public void setStatus(int status);
	public void setFailedCode(int code);
	public void setFailedCount(int count);
	public void setFailedMessage(String text);
	public void setRetryAfter(int after);
	public void setStartTime(long time);
	public void setUpdateTime(long time);
	public void setFinishTime(long time);
	
}
