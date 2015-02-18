package org.javenstudio.falcon.publication;

import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicLong;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.message.MessageHelper;
import org.javenstudio.falcon.util.IdentityUtils;

public class PublicationHelper {
	//private static final Logger LOG = Logger.getLogger(PublicationHelper.class);

	private static final AtomicLong sCounter1 = new AtomicLong(1);
	private static final AtomicLong sCounter2 = new AtomicLong(1);
	
	public static String newStreamKey() throws ErrorException { 
		return IdentityUtils.newKey("" 
				+ System.currentTimeMillis() + "-" + sCounter1.getAndIncrement(), 7) 
				+ '0';
	}
	
	public static String newPublicationKey() throws ErrorException { 
		return IdentityUtils.newKey("" 
				+ System.currentTimeMillis() + "-" 
				+ sCounter1.getAndIncrement(), 7) 
				+ '1';
	}
	
	public static boolean isStreamKeyOkay(String streamKey) {
		if (streamKey == null || streamKey.length() != 8) return false;
		return true;
	}
	
	public static String newPublicationId(String streamKey, 
			String publishKey, String publishType) throws ErrorException { 
		if (streamKey == null || streamKey.length() != 8) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Wrong publication stream key: " + streamKey);
		}
		if (publishKey == null || publishKey.length() != 8) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Wrong publication key: " + publishKey);
		}
		
		return streamKey + "-" + publishKey + "-publish-" 
			+ publishType + "-" + sCounter2.getAndIncrement();
	}
	
	public static String getStreamKey(String publishId) {
		if (publishId != null && publishId.indexOf("-publish-") >= 0) {
			StringTokenizer st = new StringTokenizer(publishId, " \t\r\n-_,.;/\\\'\"[]{}()+&@|?<>`~=%$#");
			while (st.hasMoreTokens()) {
				String token = st.nextToken();
				if (token != null && token.length() > 0) {
					if (token.length() == 8)
						return token;
					else
						return null;
				}
			}
		}
		return null;
	}
	
	public static String[] splitAddresses(String val) {
		return splitValues(val);
	}
	
	public static String[] splitValues(String val) {
		return MessageHelper.splitValues(val);
	}
	
	public static String combineValues(String... vals) {
		return MessageHelper.combineValues(vals);
	}
	
}
