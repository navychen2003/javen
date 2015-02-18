package org.javenstudio.provider.app.picasa;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.content.Context;
import android.text.TextUtils;

import org.apache.http.client.methods.HttpUriRequest;
import org.javenstudio.android.account.SystemUser;
import org.javenstudio.android.account.AccountRegistry;
import org.javenstudio.common.util.Logger;

public class PicasaHelper {
	private static final Logger LOG = Logger.getLogger(PicasaHelper.class);

	private static final String GOOGLE_SOURCE_NAME = "google.com";
	private static final String GOOGLE_ACCOUNT_TYPE = "com.google";
	private static final String PICASA_SERVICE_NAME = "lh2"; // picasa service name
	private static final String PICASA_FEATURE_SERVICE_NAME = "service_" + PICASA_SERVICE_NAME;
	private static final String PICASA_AUTHTOKEN_TYPE = "oauth2:https://picasaweb.google.com/data/";
	
	private static final AccountRegistry.Feature GOOGLE_FEATURE = new AccountRegistry.Feature(
			GOOGLE_ACCOUNT_TYPE, GOOGLE_SOURCE_NAME, PICASA_AUTHTOKEN_TYPE, PICASA_SERVICE_NAME, 
			PICASA_FEATURE_SERVICE_NAME) { 
		@Override
		public SystemUser newAccountInfo(Account account, String sourceName) {
    		return new SystemUser(account, sourceName) { 
    			@Override
    			public String getUserId() { 
    				return canonicalizeUsername(getAccountName()); 
    			}
    		};
    	}
	};
	
    /**
     * Returns a canonical username for a Gmail account.  Lowercases the username and
     * strips off a "gmail.com" or "googlemail.com" domain, but leaves other domains alone.
     *
     * e.g., Passing in "User@gmail.com: will return "user".
     *
     * @param username The username to be canonicalized.
     * @return The username, lowercased and possibly stripped of its domain if a "gmail.com" or
     * "googlemail.com" domain.
     */
    public static String canonicalizeUsername(String username) {
        username = username.toLowerCase();
        if (username.contains("@gmail.") || username.contains("@googlemail.")) {
            // Strip the domain from GMail accounts for
            // canonicalization. TODO: is there an official way?
            username = username.substring(0, username.indexOf('@'));
        }
        return username;
    }
	
    private static SystemUser[] sAccounts = null;
    private static OnAccountsUpdateListener sAccountListener = null;
    private static final Object sAccountLock = new Object();
    
    public static SystemUser[] getAccounts(Context context) { 
    	synchronized (sAccountLock) { 
    		if (sAccounts == null) { 
    			sAccounts = AccountRegistry.getAccountsFuture(context, 
    					PicasaHelper.GOOGLE_FEATURE);
    			
    			if (sAccountListener == null) { 
    				sAccountListener = new OnAccountsUpdateListener() {
							@Override
							public void onAccountsUpdated(Account[] accounts) {
								if (LOG.isDebugEnabled())
									LOG.debug("onAccountsUpdated: accounts=" + accounts);
								
								synchronized (sAccountLock) { 
									sAccounts = null;
								}
							}
	    				};
	    			AccountManager.get(context).addOnAccountsUpdatedListener(
	    					sAccountListener, null, false);
    			}
    		}
    		return sAccounts;
    	}
    }
    
    public static SystemUser getAccount(Context context, String accountName) { 
    	if (context == null || accountName == null) 
    		return null;
    	
    	SystemUser[] accounts = getAccounts(context);
    	for (int i=0; accounts != null && i < accounts.length; i++) { 
    		SystemUser account = accounts[i];
    		if (account == null) continue;
    		if (accountName.equals(account.getAccountName())) 
    			return account;
    	}
    	
    	return null;
    }
    
    public static void resetAuthToken(Context context, SystemUser account) { 
    	if (context == null || account == null) 
			return;
    	
    	synchronized (sAccountLock) { 
    		sAccounts = null;
    		account.setAuthToken(null);
    	}
    }
    
    public static void initAuthRequest(Context context, 
    		HttpUriRequest request, SystemUser account) { 
		if (context == null || request == null || account == null) 
			return;
		
		String authToken = account.getAuthToken(); 
		if (authToken == null) { 
			authToken = AccountRegistry.getAccountAuthToken(context, 
					account.getAccount(), GOOGLE_FEATURE);
			account.setAuthToken(authToken);
		}
		
        // Specify GData protocol version 2.0.
        request.addHeader("GData-Version", "2");

        // Indicate support for gzip-compressed responses.
        //request.addHeader("Accept-Encoding", "gzip");

        // Specify authorization token if provided.
        if (!TextUtils.isEmpty(authToken)) {
            //request.addHeader("Authorization", "GoogleLogin auth=" + authToken);
            request.addHeader("Authorization", "Bearer " + authToken);
        }

        // Specify the ETag of a prior response, if available.
        //String etag = operation.inOutEtag;
        //if (etag != null) {
        //    request.addHeader("If-None-Match", etag);
        //}
		
	}
    
}
