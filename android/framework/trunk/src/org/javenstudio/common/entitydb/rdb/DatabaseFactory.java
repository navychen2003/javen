package org.javenstudio.common.entitydb.rdb;

public interface DatabaseFactory {

	public Database openReadableDatabase(String path); 
	public Database openWritableDatabase(String path); 
	public Database createDatabase(); 
	
}
