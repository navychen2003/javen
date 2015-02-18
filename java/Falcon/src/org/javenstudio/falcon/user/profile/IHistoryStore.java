package org.javenstudio.falcon.user.profile;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;

public interface IHistoryStore {

	public NamedList<Object> loadHistoryList(HistoryManager manager) 
			throws ErrorException;
	
	public void saveHistoryList(HistoryManager manager, 
			NamedList<Object> items) throws ErrorException;
	
}
