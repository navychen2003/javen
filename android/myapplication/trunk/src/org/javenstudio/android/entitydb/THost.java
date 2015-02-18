package org.javenstudio.android.entitydb;

import org.javenstudio.cocoka.database.SQLiteEntityDB;

public class THost extends SQLiteEntityDB.TEntity {

	public static class Table extends SQLiteEntityDB.TTable<THost> {
		public static final String TABLE_NAME = "host"; 
		
		public static final String PREFIX = "prefix";
	    public static final String CLUSTERID = "clusterid";
	    public static final String CLUSTERDOMAIN = "clusterdomain";
	    public static final String MAILDOMAIN = "maildomain";
	    public static final String HOSTKEY = "hostkey";
		public static final String HOSTADDR = "hostaddr";
		public static final String HOSTNAME = "hostname";
		public static final String LANADDR = "lanaddr";
	    public static final String DOMAIN = "domain";
	    public static final String HTTPPORT = "httpport";
	    public static final String HTTPSPORT = "httpsport";
	    public static final String HEARTBEAT = "heartbeat";
	    public static final String ADMIN = "admin";
	    public static final String TITLE = "title";
	    public static final String TAGLINE = "tagline";
	    public static final String POSTER = "poster";
	    public static final String BACKGROUND = "background";
	    public static final String FLAG = "flag";
	    public static final String STATUS = "status";
	    public static final String FAILED_CODE = "failedCode";
	    public static final String FAILED_MESSAGE = "failedMessage";
	    public static final String CREATE_TIME = "createTime";
	    public static final String UPDATE_TIME = "updateTime";
	    
		public Table(SQLiteEntityDB.TDBOpenHelper helper) { 
			super(helper, TABLE_NAME, THost.class);
		}
	}
	
	public String prefix = null;
	public String clusterid = null;
	public String clusterdomain = null;
	public String maildomain = null;
	
	public String hostkey = null;
	public String hostaddr = null;
	public String hostname = null;
	public String lanaddr = null;
	public String domain = null;
	public Integer httpport = null;
	public Integer httpsport = null;
	
	public String admin = null;
	public String title = null;
	public String tagline = null;
	public String poster = null;
	public String background = null;
	
	public Integer flag = null; 
	public Integer status = null; 
	public Integer failedCode = null;
	public String failedMessage = null; 
	
	public Long heartbeat = null;
	public Long createTime = null;
	public Long updateTime = null;
	
	public THost() {
		super(); 
	}
	
	public THost(long id) {
		super(id); 
	}
	
}
