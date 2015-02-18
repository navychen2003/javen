package org.javenstudio.common.entitydb.db;

import org.javenstudio.common.entitydb.IDatabase;

public interface DBFactory {

	public String getDatabaseName(); 
	public String getDatabasePath(); 
	
	public IDatabase openWritableDatabase(); 
	public IDatabase openReadableDatabase(); 
	
    public void onOpenDatabase(IDatabase db); 

}
