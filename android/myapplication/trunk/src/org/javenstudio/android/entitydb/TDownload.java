package org.javenstudio.android.entitydb;

import org.javenstudio.cocoka.database.SQLiteEntityDB;

public class TDownload extends SQLiteEntityDB.TEntity {

	public static class Table extends SQLiteEntityDB.TTable<TDownload> {
		public static final String TABLE_NAME = "download"; 
		
	    public static final String CONTENT_URI = "contentUri";
	    public static final String CONTENT_NAME = "contentName";
	    public static final String CONTENT_TYPE = "contentType";
	    public static final String SAVE_APP = "saveApp";
	    public static final String SAVE_PATH = "savePath";
	    public static final String SOURCE_PREFIX = "sourcePrefix";
	    public static final String SOURCE_ACCOUNT = "sourceAccount";
	    public static final String SOURCE_ACCOUNTID = "sourceAccountId";
	    public static final String SOURCE_FILE = "sourceFile";
	    public static final String SOURCE_FILEID = "sourceFileId";
	    public static final String STATUS = "status";
	    public static final String FAILED_CODE = "failedCode";
	    public static final String FAILED_MESSAGE = "failedMessage";
	    public static final String FAILED_COUNT = "failedCount";
	    public static final String RETRY_AFTER = "retryAfter";
	    public static final String START_TIME = "startTime";
	    public static final String UPDATE_TIME = "updateTime";
	    public static final String FINISH_TIME = "finishTime";
	    
		public Table(SQLiteEntityDB.TDBOpenHelper helper) { 
			super(helper, TABLE_NAME, TDownload.class);
		}
	}
	
	public String contentUri = null; 
	public String contentName = null; 
	public String contentType = null; 
	public String saveApp = null; 
	public String savePath = null; 
	
	public String sourcePrefix = null; 
	public String sourceAccount = null; 
	public String sourceAccountId = null; 
	public String sourceFile = null; 
	public String sourceFileId = null; 
	
	public Integer status = null; 
	public Integer failedCode = null;
	public Integer failedCount = null;
	public String failedMessage = null; 
	
	public Integer retryAfter = null;
	public Long startTime = null;
	public Long updateTime = null;
	public Long finishTime = null;
	
	public TDownload() {
		super(); 
	}
	
	public TDownload(long id) {
		super(id); 
	}
	
}
