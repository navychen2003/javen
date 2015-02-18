package org.javenstudio.android.account;

import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.worker.job.Future;
import org.javenstudio.cocoka.worker.job.Job;
import org.javenstudio.cocoka.worker.job.JobContext;
import org.javenstudio.cocoka.worker.job.JobSubmit;
import org.javenstudio.common.util.Logger;

public class AccountRegistry {
	private static final Logger LOG = Logger.getLogger(AccountRegistry.class);

    public static class Feature { 
    	private final String mAccountType;
    	private final String mServiceName;
    	private final String mFeatureServiceName;
    	private final String mAuthTokenType;
    	private final String mSourceName;
    	
    	public Feature(String accountType, String sourceName, 
    			String authtokenType, String serviceName, String feature) { 
    		mAccountType = accountType;
    		mAuthTokenType = authtokenType;
    		mServiceName = serviceName;
    		mFeatureServiceName = feature;
    		mSourceName = sourceName;
    	}
    	
    	public SystemUser newAccountInfo(Account account, String sourceName) {
    		return new SystemUser(account, sourceName);
    	}
    	
    	@Override
    	public boolean equals(Object obj) { 
    		if (obj == this) return true;
    		if (obj == null || !(obj instanceof Feature)) 
    			return false;
    		
    		Feature other = (Feature)obj;
    		return  isEquals(this.mAccountType, other.mAccountType) && 
    				isEquals(this.mAuthTokenType, other.mAuthTokenType) &&
    				isEquals(this.mSourceName, other.mSourceName) &&
    				isEquals(this.mServiceName, other.mServiceName) &&
    				isEquals(this.mFeatureServiceName, other.mFeatureServiceName);
    	}
    	
    	@Override
    	public String toString() { 
    		return "Feature{accountType=" + mAccountType 
    				+ ", sourceName=" + mSourceName 
    				+ ", authtokenType=" + mAuthTokenType 
    				+ ", serviceName=" + mServiceName 
    				+ ", feature=" + mFeatureServiceName + "}";
    	}
    }
    
    public static AccountManager getAccountManager() { 
    	return AccountManager.get(ResourceHelper.getContext());
    }
    		
	public static SystemUser[] getAccounts(Context context, Feature f) { 
		if (context == null || f == null) 
			return null;
		
        List<SystemUser> list = new ArrayList<SystemUser>();
        AccountManager manager = AccountManager.get(context);
        
        try {
        	Account[] accounts = manager.getAccountsByTypeAndFeatures(f.mAccountType,
                    new String[] { f.mFeatureServiceName }, null, null).getResult();
        	
        	if (accounts != null) { 
        		for (Account account : accounts) { 
        			String authToken = getAccountAuthToken(context, account, f);
        			
        			if (LOG.isDebugEnabled()) 
    					LOG.debug("getAccounts: " + account + " sourceName=" + f.mSourceName);
        			
        			SystemUser accountInfo = f.newAccountInfo(account, f.mSourceName);
        			accountInfo.setAuthToken(authToken);
        			list.add(accountInfo);
        		}
        	}
        } catch (Throwable e) {
            if (LOG.isErrorEnabled())
            	LOG.error(e.toString(), e);
        }
        
        SystemUser[] accountInfos = list.toArray(new SystemUser[list.size()]);
        
        if (LOG.isDebugEnabled())
        	LOG.debug("getAccounts: " + f + " accountCount=" + list.size());
        
        return accountInfos;
	}
	
	public static String getAccountAuthToken(Context context, Account account, Feature f) { 
		if (context == null || account == null || f == null) 
			return null;
		
		AccountManager manager = AccountManager.get(context);
		
		try {
			String authToken = manager.blockingGetAuthToken(account, f.mAuthTokenType, true);
			
			if (authToken == null && context instanceof Activity) {
				Bundle bundle = manager.getAuthToken(account, f.mAuthTokenType, null, 
						(Activity) context, null, null).getResult();
	            authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
			}
			
			if (LOG.isDebugEnabled()) 
				LOG.debug("getAccountAuthToken: " + account + " authToken=" + authToken);
		
			return authToken;
		} catch (Throwable e) {
            if (LOG.isErrorEnabled())
            	LOG.error(e.toString(), e);
        }
		
		return null;
	}
	
	public static String getAccountAuthTokenFuture(
			final Context context, final Account account, final Feature f) { 
		if (context == null || account == null || f == null) 
			return null;
		
		Future<String> future = JobSubmit.submit(new Job<String>() {
				@Override
				public String run(JobContext jc) {
					return AccountRegistry.getAccountAuthToken(context, account, f);
				}
			});
		
		return future.get();
	}
	
	public static SystemUser[] getAccountsFuture(
			final Context context, final Feature f) { 
		if (context == null || f == null) 
			return null;
		
		Future<SystemUser[]> future = JobSubmit.submit(new Job<SystemUser[]>() {
				@Override
				public SystemUser[] run(JobContext jc) {
					return AccountRegistry.getAccounts(context, f);
				}
			});
		
		return future.get();
	}
	
	static <T> boolean isEquals(T obj1, T obj2) { 
		if (obj1 != null || obj2 != null) { 
			if (obj1 == null || obj2 == null) 
				return false;
			
			return obj1.equals(obj2);
		}
		
		return true;
	}
	
}
