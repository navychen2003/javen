package org.javenstudio.cocoka.database.example;

import org.javenstudio.cocoka.database.SQLiteEntityDB;

public class ExampleEntity extends SQLiteEntityDB.TEntity {
    
    public static class Table extends SQLiteEntityDB.TTable<ExampleEntity> {
    	public static final String TABLE_NAME = "account"; 
    	
    	// The display name of the account (user-settable)
        public static final String DISPLAY_NAME = "displayName";
        // The email address corresponding to this account
        public static final String EMAIL_ADDRESS = "emailAddress";
        
    	public Table(SQLiteEntityDB.TDBOpenHelper helper) { 
    		super(helper, TABLE_NAME, ExampleEntity.class); 
    	}
    }
    
	
	public String displayName = null; 
	public String emailAddress = null; 
	
	public ExampleEntity() {
		super(); 
	}
	
	public ExampleEntity(long id) {
		super(id); 
	}
	
}
