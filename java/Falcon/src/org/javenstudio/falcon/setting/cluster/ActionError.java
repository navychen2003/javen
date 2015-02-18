package org.javenstudio.falcon.setting.cluster;

public class ActionError {

	public static enum Action {
		EXCEPTION, HTTP_EXCEPTION, 
		JOIN, ATTACH, PONG, AUTH, LOGIN, 
		GETUSER, PUTUSER, RMUSER, 
		GETNAME, PUTNAME, RMNAME
	}
	
	private final Action mAction;
	private final int mCode;
	private final String mMessage;
	private final String mTrace;
	
	private final Throwable mException;
	
	public ActionError(Action action, Throwable exception) {
		this(action, -1, null, null, exception);
	}
	
	public ActionError(Action action, 
			int code, String message, String trace) {
		this(action, code, message, trace, null);
	}
	
	public ActionError(Action action, 
			int code, String message, String trace, Throwable e) {
		mAction = action;
		mCode = code;
		mMessage = message;
		mTrace = trace;
		mException = e;
	}
	
	public Action getAction() { return mAction; }
	public int getCode() { return mCode; }
	public String getMessage() { return mMessage; }
	public String getTrace() { return mTrace; }
	public Throwable getException() { return mException; }
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + 
				"{action=" + mAction + ",code=" + mCode 
				+ ",message=" + mMessage + ",trace=" + mTrace 
				+ ",exception=" + mException + "}";
	}
	
}
