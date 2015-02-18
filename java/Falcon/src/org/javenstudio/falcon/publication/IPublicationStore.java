package org.javenstudio.falcon.publication;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IDatabase;

public interface IPublicationStore {

	public String getUserName();
	public IDatabase getDatabase() throws ErrorException;
	
}
