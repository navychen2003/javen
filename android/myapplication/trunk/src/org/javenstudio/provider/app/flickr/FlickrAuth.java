package org.javenstudio.provider.app.flickr;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import android.net.Uri;
import android.util.Base64;

import org.javenstudio.common.util.Logger;

public class FlickrAuth {
	private static final Logger LOG = Logger.getLogger(FlickrAuth.class);

	static String computeSignature(String baseString, String key) throws Exception {
        //here key is the appsecret+"&"
        byte[] byteHMAC = null;
        Mac mac = Mac.getInstance("HmacSHA1");
        SecretKeySpec spec = new SecretKeySpec(key.getBytes(), "HmacSHA1");
        mac.init(spec);
        byteHMAC = mac.doFinal(baseString.getBytes("UTF-8"));
        return new String(Base64.encode(byteHMAC, Base64.NO_WRAP));
	}
	
	static String getSignatureRequests() { 
		String packageName = "org.javenstudio.android.gallery";
		String consumerKey = "d683a4c48629ed724d94c064f27611b8";
		String timestamp = Long.toString((System.currentTimeMillis())/1000);
		String nonce = Long.toString(System.nanoTime());
		
		String base = "GET&http%3A%2F%2Fwww.flickr.com%2Fservices%2Foauth%2Frequest_token&" 
				+ "oauth_callback%3Dsoft%253A%252F%252F" + packageName 
				+ "%26oauth_consumer_key%3D" + consumerKey + "%26oauth_nonce%3D" + nonce 
				+ "%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D" + timestamp 
				+ "%26oauth_version%3D1.0";
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("getSignatureRequests: baseString=" + base);
		
		try {
			String signature = computeSignature(base, "e3c368ff79c8c949&");
			String result = "http://www.flickr.com/services/oauth/request_token" 
					+ "?oauth_nonce=" + nonce 
					+ "&oauth_timestamp=" + timestamp 
					+ "&oauth_consumer_key=" + Uri.encode(consumerKey) 
					+ "&oauth_signature_method=HMAC-SHA1&oauth_version=1.0" 
					+ "&oauth_signature=" + Uri.encode(signature) 
					+ "&oauth_callback=soft%3A%2F%2F" + packageName;
			
			if (LOG.isDebugEnabled()) 
				LOG.debug("getSignatureRequests: result=" + result);
			
			return result;
		} catch (Throwable e) { 
			if (LOG.isWarnEnabled())
				LOG.warn("getSignatureRequests: signature error: " + e.toString(), e);
			
			return null;
		}
	}
	
	public static String authenticate(String username, String password) { 
		return getSignatureRequests();
	}
	
}
