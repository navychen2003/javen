package org.javenstudio.falcon.user.global;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IDatabase;
import org.javenstudio.falcon.util.ILockable;

public interface IUnitStore {

	public String getUserName();
	public ILockable.Lock getLock();
	
	public IDatabase getDatabase() throws ErrorException;
	
}
