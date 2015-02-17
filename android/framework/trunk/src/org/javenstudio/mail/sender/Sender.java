package org.javenstudio.mail.sender;

import java.util.HashMap;

import org.javenstudio.common.util.Logger;
import org.javenstudio.mail.MessagingException;
import org.javenstudio.mail.content.MessageData;

public abstract class Sender {
	private static Logger LOG = Logger.getLogger(Sender.class); 

	/**
     * String constants for known store schemes.
     */
    public static final String SENDER_SCHEME_SMTP = "smtp";
    
	
	private static HashMap<String, Sender> mSenders = new HashMap<String, Sender>();
	
	/**
     * Static named constructor.  It should be overrode by extending class.
     * Because this method will be called through reflection, it can not be protected.
     */
    public static Sender newInstance(String uri)
            throws MessagingException {
        throw new MessagingException("Sender.newInstance: Unknown scheme in " + uri);
    }

    private static Sender instantiateSender(String className, String uri)
        throws MessagingException {
        Object o = null;
        try {
            Class<?> c = Class.forName(className);
            // and invoke "newInstance" class method and instantiate sender object.
            java.lang.reflect.Method m = c.getMethod("newInstance", String.class);
            o = m.invoke(null, uri);
        } catch (Exception e) {
            LOG.error(String.format(
            		"exception invoking %s.newInstance.(Context, String) method for %s",
                    className, uri), e);
            throw new MessagingException("can not instantiate Sender object for " + uri, e);
        }
        if (!(o instanceof Sender)) {
            throw new MessagingException(
                    uri + ": " + className + " create incompatible object");
        }
        return (Sender) o;
    }
    
    public synchronized static Sender getInstance(String uri)
            throws MessagingException {
       Sender sender = mSenders.get(uri);
       if (sender == null) {
           SenderInfo info = SenderInfo.getSenderInfo(uri); 
           if (info != null) { 
        	   sender = instantiateSender(info.getClassName(), uri);
        	   if (sender != null) 
        		   mSenders.put(uri, sender);
           }
       }

       if (sender == null) 
            throw new MessagingException("Unable to locate an applicable Transport for " + uri);

       return sender;
    }

    public abstract void open() throws MessagingException;
    
    public String validateSenderLimit(long messageId) {
        return null;
    }

    /**
     * Check message has any limitation of Sender or not.
     * 
     * @param messageId the message that will be checked.
     * @throws LimitViolationException
     */
    public void checkSenderLimitation(long messageId) throws LimitViolationException {
    	// do nothing
    }
    
    public static class LimitViolationException extends MessagingException {
    	private static final long serialVersionUID = 1L;
    	
        public final int mMsgResourceId;
        public final long mActual;
        public final long mLimit;
        
        private LimitViolationException(int msgResourceId, long actual, long limit) {
            super("UNSPECIFIED_EXCEPTION");
            mMsgResourceId = msgResourceId;
            mActual = actual;
            mLimit = limit;
        }
        
        public static void check(int msgResourceId, long actual, long limit)
            throws LimitViolationException {
            if (actual > limit) {
                throw new LimitViolationException(msgResourceId, actual, limit);
            }
        }
    }
    
    public abstract void sendMessage(MessageData message) throws MessagingException;

    public abstract void close() throws MessagingException;
	
}
