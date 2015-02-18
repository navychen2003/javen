package org.javenstudio.falcon;

public class ErrorException extends Exception {
	private static final long serialVersionUID = 1L;

	public enum ErrorCode {
		BAD_REQUEST(400),
	    UNAUTHORIZED(401),
	    FORBIDDEN(403),
	    NOT_FOUND(404),
	    CONFLICT(409),
	    SERVER_ERROR(500),
	    SERVICE_UNAVAILABLE(503),
	    UNKNOWN(0);
		
	    private final int mCode;
	    
	    private ErrorCode(int code) {
	    	mCode = code;
	    }
	    
	    public final int getCode() { return mCode; }
	    
	    public static ErrorCode getErrorCode(int c){
	    	for (ErrorCode err : values()) {
	    		if (err.mCode == c) return err;
	    	}
	    	return UNKNOWN;
	    }
	};
	
	private final int mCode;
	
	public ErrorException(ErrorCode code, String msg) {
		super(msg);
		mCode = code.mCode;
	}
	
	public ErrorException(ErrorCode code, String msg, Throwable th) {
		super(msg, th);
		mCode = code.mCode;
	}

	public ErrorException(ErrorCode code, Throwable th) {
		super(th);
		mCode = code.mCode;
	}
	
	public final int getCode() { return mCode; }
	
}
