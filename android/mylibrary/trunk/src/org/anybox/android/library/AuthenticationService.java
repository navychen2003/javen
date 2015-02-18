package org.anybox.android.library;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.javenstudio.common.util.Logger;

public class AuthenticationService extends Service {
	private static final Logger LOG = Logger.getLogger(AuthenticationService.class);

    private Authenticator mAuthenticator;

    @Override
    public void onCreate() {
        if (LOG.isDebugEnabled()) 
            LOG.debug("AuthenticationService: started.");
        
        mAuthenticator = new Authenticator(this);
    }

    @Override
    public void onDestroy() {
    	if (LOG.isDebugEnabled()) 
            LOG.debug("AuthenticationService: stopped.");
    }

    @Override
    public IBinder onBind(Intent intent) {
    	if (LOG.isDebugEnabled()) 
            LOG.debug("AuthenticationService: onBind, intent=" + intent);
        
        return mAuthenticator.getIBinder();
    }
    
}
