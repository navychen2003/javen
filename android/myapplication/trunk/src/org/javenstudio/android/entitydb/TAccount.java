package org.javenstudio.android.entitydb;

import org.javenstudio.cocoka.database.SQLiteEntityDB;

public class TAccount extends SQLiteEntityDB.TEntity {

	public static class Table extends SQLiteEntityDB.TTable<TAccount> {
		public static final String TABLE_NAME = "account"; 
		
		public static final String PREFIX = "prefix";
		public static final String HOSTID = "hostid";
	    public static final String USERKEY = "userkey";
	    public static final String USERNAME = "username";
	    public static final String NICKNAME = "nickname";
	    public static final String MAILADDRESS = "mailaddress";
	    public static final String TYPE = "type";
	    public static final String CATEGORY = "category";
	    public static final String AVATAR = "avatar";
	    public static final String BACKGROUND = "background";
	    public static final String EMAIL = "email";
	    public static final String TOKEN = "token";
	    public static final String AUTHKEY = "authkey";
	    public static final String DEVICEKEY = "devicekey";
	    public static final String CLIENTKEY = "clientkey";
	    public static final String USEDSPACE = "usedspace";
	    public static final String USABLESPACE = "usablespace";
	    public static final String FREESPACE = "freespace";
	    public static final String PURCHASED = "purchased";
	    public static final String CAPACITY = "capacity";
	    public static final String FLAG = "flag";
	    public static final String STATUS = "status";
	    public static final String FAILED_CODE = "failedCode";
	    public static final String FAILED_MESSAGE = "failedMessage";
	    public static final String CREATE_TIME = "createTime";
	    public static final String UPDATE_TIME = "updateTime";
	    public static final String KEYGEN_TIME = "keygenTime";
	    
		public Table(SQLiteEntityDB.TDBOpenHelper helper) { 
			super(helper, TABLE_NAME, TAccount.class);
		}
	}
	
	public String prefix = null;
	public Long hostid = null;
	
	public String userkey = null;
	public String username = null; 
	public String nickname = null; 
	public String mailaddress = null;
	public String type = null;
	public String category = null;
	public String avatar = null;
	public String background = null;
	public String email = null; 
	public String token = null; 
	public String authkey = null;
	public String devicekey = null;
	public String clientkey = null;
	
	public Long usedspace = null;
	public Long usablespace = null;
	public Long freespace = null;
	public Long purchased = null;
	public Long capacity = null;
	
	public Integer flag = null; 
	public Integer status = null; 
	public Integer failedCode = null;
	public String failedMessage = null; 
	
	public Long createTime = null;
	public Long updateTime = null;
	public Long keygenTime = null;
	
	public TAccount() {
		super(); 
	}
	
	public TAccount(long id) {
		super(id); 
	}
	
}
