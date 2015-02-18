package org.anybox.android.library;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import org.javenstudio.common.util.Logger;

public class Authenticator extends AbstractAccountAuthenticator {
	private static final Logger LOG = Logger.getLogger(Authenticator.class);
	
    public static final String ACCOUNT_TYPE = "org.anybox.android.library";
    public static final String AUTHTOKEN_TYPE = "org.anybox.android.library";
	
    private final Context mContext;

    public Authenticator(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
            String authTokenType, String[] requiredFeatures, Bundle options) {
    	if (LOG.isDebugEnabled()) {
        	LOG.debug("addAccount: accountType=" + accountType + " authTokenType=" + authTokenType 
        			+ " requiredFeatures=" + requiredFeatures);
    	}
    	
        final Intent intent = new Intent(mContext, RegisterActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        
        return bundle;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, 
    		Account account, Bundle options) {
    	if (LOG.isDebugEnabled())
        	LOG.debug("confirmCredentials: account=" + account);
    	
        return null;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
    	if (LOG.isDebugEnabled())
        	LOG.debug("editProperties: accountType=" + accountType);
    	
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
            String authTokenType, Bundle loginOptions) throws NetworkErrorException {
    	if (LOG.isDebugEnabled())
        	LOG.debug("getAuthToken: account=" + account + " authTokenType=" + authTokenType);

        // If the caller requested an authToken type we don't support, then
        // return an error
        if (!authTokenType.equals(AUTHTOKEN_TYPE)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType");
            return result;
        }

        // Extract the username and password from the Account Manager, and ask
        // the server for an appropriate AuthToken.
        final AccountManager am = AccountManager.get(mContext);
        final String password = am.getPassword(account);
        if (password != null) {
            final String authToken = "[AuthToken]"; //NetworkUtilities.authenticate(account.name, password);
            if (!TextUtils.isEmpty(authToken)) {
                final Bundle result = new Bundle();
                result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                result.putString(AccountManager.KEY_ACCOUNT_TYPE, ACCOUNT_TYPE);
                result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
                return result;
            }
        }

        // If we get here, then we couldn't access the user's password - so we
        // need to re-prompt them for their credentials. We do that by creating
        // an intent to display our AuthenticatorActivity panel.
        final Intent intent = new Intent(mContext, RegisterActivity.class);
        intent.putExtra(RegisterActivity.PARAM_USERNAME, account.name);
        intent.putExtra(RegisterActivity.PARAM_AUTHTOKEN_TYPE, authTokenType);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        
        return bundle;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        // null means we don't support multiple authToken types
    	if (LOG.isDebugEnabled())
        	LOG.debug("getAuthTokenLabel: authTokenType=" + authTokenType);
    	
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, 
    		Account account, String[] features) {
        // This call is used to query whether the Authenticator supports
        // specific features. We don't expect to get called, so we always
        // return false (no) for any queries.
    	if (LOG.isDebugEnabled())
        	LOG.debug("hasFeatures: account=" + account + " features=" + features);
    	
        final Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        
        return result;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, 
    		Account account, String authTokenType, Bundle loginOptions) {
    	if (LOG.isDebugEnabled())
        	LOG.debug("updateCredentials: account=" + account + " authTokenType=" + authTokenType);
    	
        return null;
    }
    
}
