package org.javenstudio.android.entitydb;

import org.javenstudio.cocoka.database.SQLiteEntityDB;

public class TUpload extends SQLiteEntityDB.TEntity {

	public static class Table extends SQLiteEntityDB.TTable<TUpload> {
		public static final String TABLE_NAME = "upload"; 
		
	    public static final String CONTENT_URI = "contentUri";
	    public static final String CONTENT_NAME = "contentName";
	    public static final String CONTENT_TYPE = "contentType";
	    public static final String DATA_PATH = "dataPath";
	    public static final String FILE_PATH = "filePath";
	    public static final String DEST_PREFIX = "destPrefix";
	    public static final String DEST_ACCOUNT = "destAccount";
	    public static final String DEST_ACCOUNTID = "destAccountId";
	    public static final String DEST_PATH = "destPath";
	    public static final String DEST_PATHID = "destPathId";
	    public static final String STATUS = "status";
	    public static final String FAILED_CODE = "failedCode";
	    public static final String FAILED_MESSAGE = "failedMessage";
	    public static final String FAILED_COUNT = "failedCount";
	    public static final String RETRY_AFTER = "retryAfter";
	    public static final String START_TIME = "startTime";
	    public static final String UPDATE_TIME = "updateTime";
	    public static final String FINISH_TIME = "finishTime";
	    
		public Table(SQLiteEntityDB.TDBOpenHelper helper) { 
			super(helper, TABLE_NAME, TUpload.class);
		}
	}
	
	public String contentUri = null; 
	public String contentName = null; 
	public String contentType = null; 
	public String dataPath = null; 
	public String filePath = null; 
	
	public String destPrefix = null; 
	public String destAccount = null; 
	public String destAccountId = null; 
	public String destPath = null; 
	public String destPathId = null; 
	
	public Integer status = null; 
	public Integer failedCode = null;
	public Integer failedCount = null;
	public String failedMessage = null; 
	
	public Integer retryAfter = null;
	public Long startTime = null;
	public Long updateTime = null;
	public Long finishTime = null;
	
	public TUpload() {
		super(); 
	}
	
	public TUpload(long id) {
		super(id); 
	}
	
}
