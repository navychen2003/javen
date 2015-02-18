package org.javenstudio.android.mail;

import org.javenstudio.mail.MessagingException;

/**
 * This exception is used for most types of failures that occur during server interactions.
 * 
 * Data passed through this exception should be considered non-localized.  Any strings should
 * either be internal-only (for debugging) or server-generated.
 * 
 * TO DO: Does it make sense to further collapse AuthenticationFailedException and
 * CertificateValidationException and any others into this?
 */
public class MessageActionException extends MessagingException {
    public static final long serialVersionUID = -1L;
    
    public static final int NO_ERROR = -1;
    /** Any exception that does not specify a specific issue */
    public static final int UNSPECIFIED_EXCEPTION = 0;
    /** Connection or IO errors */
    public static final int IOERROR = 1;
    /** The configuration requested TLS but the server did not support it. */
    public static final int TLS_REQUIRED = 2;
    /** Authentication is required but the server did not support it. */
    public static final int AUTH_REQUIRED = 3;
    /** General security failures */
    public static final int GENERAL_SECURITY = 4;
    /** Authentication failed */
    public static final int AUTHENTICATION_FAILED = 5;
    /** Attempt to create duplicate account */
    public static final int DUPLICATE_ACCOUNT = 6;
    /** Required security policies reported - advisory only */
    public static final int SECURITY_POLICIES_REQUIRED = 7;
   /** Required security policies not supported */
    public static final int SECURITY_POLICIES_UNSUPPORTED = 8;
   /** The protocol (or protocol version) isn't supported */
    public static final int PROTOCOL_VERSION_UNSUPPORTED = 9;
    
    protected int mExceptionType;
    
    protected long mAccountKey = -1; 
    protected long mMailboxKey = -1; 
    protected long mMessageKey = -1; 
    protected long mAttachmentKey = -1; 
    
    public long getAccountKey() { return mAccountKey; }
    public long getMailboxKey() { return mMailboxKey; }
    public long getMessageKey() { return mMessageKey; }
    public long getAttachmentKey() { return mAttachmentKey; }
    
    public void setAccountKey(long id) { mAccountKey = id; }
    public void setMailboxKey(long id) { mMailboxKey = id; }
    public void setMessageKey(long id) { mMessageKey = id; }
    public void setAttachmentKey(long id) { mAttachmentKey = id; }
    
    public MessageActionException(String message) {
        super(message);
        mExceptionType = UNSPECIFIED_EXCEPTION;
    }

    public MessageActionException(String message, Throwable throwable) {
        super(message, throwable);
        mExceptionType = UNSPECIFIED_EXCEPTION;
    }
    
    /**
     * Constructs a MessagingException with an exceptionType and a null message.
     * @param exceptionType The exception type to set for this exception.
     */
    public MessageActionException(int exceptionType) {
        super("exception type "+exceptionType);
        mExceptionType = exceptionType;
    }
    
    /**
     * Constructs a MessagingException with an exceptionType and a message.
     * @param exceptionType The exception type to set for this exception.
     */
    public MessageActionException(int exceptionType, String message) {
        super(message);
        mExceptionType = exceptionType;
    }
    
    public MessageActionException(int exceptionType, String message, Throwable throwable) {
        super(message, throwable);
        mExceptionType = exceptionType;
    }
    
    /**
     * Return the exception type.  Will be OTHER_EXCEPTION if not explicitly set.
     * 
     * @return Returns the exception type.
     */
    public int getExceptionType() {
        return mExceptionType;
    }
}