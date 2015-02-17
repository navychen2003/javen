package org.javenstudio.cocoka.database.example;

import org.javenstudio.cocoka.database.SQLiteEntityDB;

public class ExampleQuery extends SQLiteEntityDB.TQuery<ExampleEntity> {

	public ExampleQuery() {
		super(ExampleDatabase.getDatabase(), ExampleEntity.class); 
	}
	
	public void setEmailAddress(String address) {
		whereAnd(newEqualsClause(ExampleEntity.Table.EMAIL_ADDRESS, address));
	}
	
	public static ExampleEntity queryAccount(long id) { 
		return ExampleDatabase.queryEntity(ExampleEntity.class, id);
	}
	
}
