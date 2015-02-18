package org.javenstudio.falcon.datum;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.Path;

public interface IDatabaseStore {

	public FileSystem getDatabaseFs() throws ErrorException;
	
	public Path getDatabasePath(IDatabase.Manager manager) 
			throws ErrorException;
	
}
