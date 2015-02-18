package org.javenstudio.falcon.message;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicLong;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.util.IdentityUtils;

public class MessageHelper {
	private static final Logger LOG = Logger.getLogger(MessageHelper.class);

	private static final AtomicLong sCounter1 = new AtomicLong(1);
	private static final AtomicLong sCounter2 = new AtomicLong(1);
	
	public static String newStreamKey() throws ErrorException { 
		return IdentityUtils.newKey("" 
				+ System.currentTimeMillis() + "-" + sCounter1.getAndIncrement(), 7) 
				+ '0';
	}
	
	public static String newMessageKey() throws ErrorException { 
		return IdentityUtils.newKey("" 
				+ System.currentTimeMillis() + "-" 
				+ sCounter1.getAndIncrement(), 7) 
				+ '1';
	}
	
	public static boolean isStreamKeyOkay(String streamKey) {
		if (streamKey == null || streamKey.length() != 8) return false;
		return true;
	}
	
	public static String newMessageId(String streamKey, 
			String messageKey, String messageType) throws ErrorException { 
		if (streamKey == null || streamKey.length() != 8) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Wrong message stream key: " + streamKey);
		}
		if (messageKey == null || messageKey.length() != 8) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Wrong message key: " + messageKey);
		}
		
		return streamKey + "-" + messageKey + "-lightning-" 
			+ messageType + "-" + sCounter2.getAndIncrement();
	}
	
	public static String getStreamKey(String messageId) {
		if (messageId != null && messageId.indexOf("-lightning-") >= 0) {
			StringTokenizer st = new StringTokenizer(messageId, " \t\r\n-_,.;/\\\'\"[]{}()+&@|?<>`~=%$#");
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
		if (val == null) return null;
		
		StringTokenizer st = new StringTokenizer(val, " \t\r\n,;");
		ArrayList<String> list = new ArrayList<String>();
		
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (token != null && token.length() > 0)
				list.add(token);
		}
		
		return list.toArray(new String[list.size()]);
	}
	
	public static String combineValues(String... vals) {
		if (vals == null) return null;
		
		StringBuilder sbuf = new StringBuilder();
		
		for (String val : vals) {
			if (val != null && val.length() > 0) {
				if (sbuf.length() > 0) sbuf.append(", ");
				sbuf.append(val);
			}
		}
		
		return sbuf.toString();
	}
	
	public static void notifyDef(String userKey, String subject) { 
		notifyDef(userKey, subject, null);
	}
	
	public static void notifyDef(String userKey, String subject, String body) { 
		notify(IMessage.DEFAULT, IUser.SYSTEM, userKey, subject, body);
	}
	
	public static void notifyLog(String userKey, String subject) { 
		notifyLog(userKey, subject, null);
	}
	
	public static void notifyLog(String userKey, String subject, String body) { 
		notify(IMessage.LOGON, IUser.SYSTEM, userKey, subject, body);
	}
	
	public static void notifySys(String userKey, String subject) { 
		notifySys(userKey, subject, null);
	}
	
	public static void notifySys(String userKey, String subject, String body) { 
		notify(IMessage.SYSTEM, IUser.SYSTEM, userKey, subject, body);
	}
	
	private static void notify(String folderName, String from, 
			String userKey, String subject, String body) { 
		if (folderName == null || from == null || userKey == null || subject == null) 
			return;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("notify: user=" + userKey + " folder=" + folderName + 
					" from=" + from + " subject=" + subject + " body=" + body);
		}
		
		try {
			IUser user = UserHelper.getLocalUserByKey(userKey);
			if (user == null) user = UserHelper.getLocalUserByName(userKey);
			if (user == null) return;
			
			MessageManager manager = user.getMessageManager();
			if (manager == null) return;
			
			IMessageService service = manager.getService(MessageManager.TYPE_NOTICE);
			if (service == null) return;
			
			IMessage.Builder builder = service.newMessage(folderName, null);
			builder.setFrom(from);
			builder.setTo(user.getUserName());
			builder.setSubject(subject);
			builder.setBody(body);
			builder.setContentType("text/plain");
			
			if (builder.save() != null) 
			  service.flushMessages();
			
		} catch (Throwable e) { 
			if (LOG.isWarnEnabled())
				LOG.warn("notify: error: " + e 
						+ ", userkey=" + userKey + ", folder=" + folderName 
						+ ", from=" + from + ", subject=" + subject 
						+ ", body=" + body, 
						e);
		}
	}
	
}
