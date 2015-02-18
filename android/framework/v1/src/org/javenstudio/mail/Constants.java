package org.javenstudio.mail;

public final class Constants {

	/**
     * A global suggestion to Store implementors on how much of the body
     * should be returned on FetchProfile.Item.BODY_SANE requests.
     */
    public static final int FETCH_BODY_SANE_SUGGESTED_SIZE = (50 * 1024);
	
    
    public static void setPreference(Preference instance) { 
    	Preference.setPreference(instance);
    }
    
}
