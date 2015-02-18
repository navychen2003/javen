package org.javenstudio.cocoka.database.example;

import org.javenstudio.cocoka.database.SQLiteEntityDB;

public class ExampleUpdater extends SQLiteEntityDB.TUpdater {

	private ExampleEntity mEntity; 
	
	public ExampleUpdater(ExampleEntity data) {
		super(ExampleDatabase.getDatabase()); 
		mEntity = data;
	} 
	
	@Override
	protected SQLiteEntityDB.TEntity[] getEntities() {
		return new SQLiteEntityDB.TEntity[]{ mEntity }; 
	}
	
	public void setAccount(ExampleEntity data) { 
		mEntity = data;
	}
	
}
