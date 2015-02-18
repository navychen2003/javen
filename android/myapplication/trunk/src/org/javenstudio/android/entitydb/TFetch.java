package org.javenstudio.android.entitydb;

import org.javenstudio.cocoka.database.SQLiteEntityDB;

public class TFetch extends SQLiteEntityDB.TEntity {

	public static class Table extends SQLiteEntityDB.TTable<TFetch> {
		public static final String TABLE_NAME = "fetch"; 
		
	    public static final String CONTENT_URI = "contentUri";
	    public static final String CONTENT_NAME = "contentName";
	    public static final String CONTENT_TYPE = "contentType";
	    public static final String PREFIX = "prefix";
	    public static final String ACCOUNT = "account";
	    public static final String ENTRY_ID = "entryId";
	    public static final String ENTRY_TYPE = "entryType";
	    public static final String TOTAL_RESULTS = "totalResults";
	    public static final String START_INDEX = "startIndex";
	    public static final String ITEMS_PERPAGE = "itemsPerPage";
	    public static final String STATUS = "status";
	    public static final String FAILED_CODE = "failedCode";
	    public static final String FAILED_MESSAGE = "failedMessage";
	    public static final String CREATE_TIME = "createTime";
	    public static final String UPDATE_TIME = "updateTime";
	    
		public Table(SQLiteEntityDB.TDBOpenHelper helper) { 
			super(helper, TABLE_NAME, TFetch.class);
		}
	}
	
	public String contentUri = null; 
	public String contentName = null; 
	public String contentType = null; 
	
	public String prefix = null; 
	public String account = null;
	public String entryId = null;
	public Integer entryType = null; 
	public Integer totalResults = null;
	public Integer startIndex = null;
	public Integer itemsPerPage = null;
	
	public Integer status = null; 
	public Integer failedCode = null;
	public String failedMessage = null; 
	
	public Long createTime = null;
	public Long updateTime = null;
	
	public TFetch() {
		super(); 
	}
	
	public TFetch(long id) {
		super(id); 
	}
	
}
