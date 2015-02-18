package org.javenstudio.falcon.user.profile;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;

public interface IPreferenceStore {

	public NamedList<Object> loadPreference(Preference preference) 
			throws ErrorException;
	
	public void savePreference(Preference preference, 
			NamedList<Object> items) throws ErrorException;
	
}
