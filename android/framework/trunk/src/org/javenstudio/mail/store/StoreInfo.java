package org.javenstudio.mail.store;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.util.StringUtils;

/**
 * Look up descriptive information about a particular type of store.
 */
public class StoreInfo {

	private final String mScheme;
	private final String mClassName;
	private boolean mPushSupported = false;
	private int mVisibleLimitDefault = 0;
	private int mVisibleLimitIncrement = 0;
	private int mAccountInstanceLimit = 0;
    
    public StoreInfo(String scheme, String className) { 
    	mScheme = scheme; 
    	mClassName = className; 
    }
    
    public String getScheme() { return mScheme; } 
    public String getClassName() { return mClassName; } 
    
    public boolean getPushSupported() { return mPushSupported; } 
    public void setPushSupported(boolean value) { mPushSupported = value; }
    
    public int getVisibleLimitDefault() { return mVisibleLimitDefault; } 
    public void setVisibleLimitDefault(int value) { mVisibleLimitDefault = value; } 
    
    public int getVisibleLimitIncrement() { return mVisibleLimitIncrement; } 
    public void setVisibleLimitIncrement(int value) { mVisibleLimitIncrement = value; } 
    
    public int getAccountInstanceLimit() { return mAccountInstanceLimit; } 
    public void setAccountInstanceLimit(int value) { mAccountInstanceLimit = value; } 
    
    private final static Map<String, StoreInfo> mStores = new HashMap<String, StoreInfo>(); 
    
    public static StoreInfo getStoreInfo(String uri) {
    	synchronized (mStores) { 
    		if (uri != null) { 
    			if (mStores.size() == 0) { 
    				registerStore(new StoreInfo(Store.STORE_SCHEME_POP3, Pop3Store.class.getName()));
    				registerStore(new StoreInfo(Store.STORE_SCHEME_IMAP, ImapStore.class.getName()));
    			}
    			
    			String scheme = URI.create(uri).getScheme(); 
    			int pos = scheme.indexOf('+'); 
    			if (pos > 0) scheme = scheme.substring(0, pos); 
    			
    			StoreInfo info = mStores.get(scheme); 
    			if (info == null) 
    				throw new RuntimeException("Store for scheme: "+scheme+" not registered"); 
    			
    			return info; 
    		}
    	}
    	
    	return null;
    }
    
    public static void registerStore(StoreInfo info) {
    	if (info == null) return; 
    	
    	synchronized (mStores) { 
			if (!StringUtils.isEmpty(info.mScheme) && !StringUtils.isEmpty(info.mClassName)) 
				mStores.put(info.mScheme, info);
    	}
    }
	
}
