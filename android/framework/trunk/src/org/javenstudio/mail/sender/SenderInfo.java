package org.javenstudio.mail.sender;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.util.StringUtils;

/**
 * Look up descriptive information about a particular type of sender.
 */
public class SenderInfo {

	private final String mScheme;
	private final String mClassName;
	
	public SenderInfo(String scheme, String className) { 
		mScheme = scheme; 
		mClassName = className; 
	}
	
	public String getScheme() { return mScheme; } 
    public String getClassName() { return mClassName; } 
    
    private final static Map<String, SenderInfo> mSenders = new HashMap<String, SenderInfo>(); 
    
    public static SenderInfo getSenderInfo(String uri) {
    	synchronized (mSenders) { 
    		if (uri != null) { 
    			if (mSenders.size() == 0) { 
    				registerSender(new SenderInfo(Sender.SENDER_SCHEME_SMTP, Rfc822SmtpSender.class.getName()));
    			}
    			
    			String scheme = URI.create(uri).getScheme(); 
    			int pos = scheme.indexOf('+'); 
    			if (pos > 0) scheme = scheme.substring(0, pos); 
    			
    			SenderInfo info = mSenders.get(scheme); 
    			if (info == null) 
    				throw new RuntimeException("Sender for scheme: "+scheme+" not registered"); 
    			
    			return info; 
    		}
    	}
    	
    	return null;
    }
    
    public static void registerSender(SenderInfo info) {
    	if (info == null) return; 
    	
        synchronized (mSenders) { 
    		if (!StringUtils.isEmpty(info.mScheme) && !StringUtils.isEmpty(info.mClassName)) 
    			mSenders.put(info.mScheme, info);
        }
    }
	
}
