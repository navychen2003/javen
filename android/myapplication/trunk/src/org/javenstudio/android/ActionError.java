package org.javenstudio.android;

import org.javenstudio.cocoka.net.http.HttpException;

public class ActionError {

	public static enum Action {
		EXCEPTION, HTTP_EXCEPTION, HOST_INIT, 
		ACCOUNT_AUTH, ACCOUNT_LOGIN, ACCOUNT_REGISTER, ACCOUNT_CHECK,
		ACCOUNT_HEARTBEAT, ACCOUNT_INFO, ACCOUNT_PROFILE, ACCOUNT_SPACE, 
		ACCOUNT_DASHBOARD, ACCOUNT_LOGOUT, SECTION_LIST, SECTION_PROPERTY, 
		ALBUM_PHOTOSET, ALBUMSET, CONTACT, FAVORITE, GROUPLIST, 
		PHOTOSET, TOPIC, TOPIC_REPLY, START_ACTIVITY
	}
	
	private final Action mAction;
	private final int mCode;
	private final String mMessage;
	private final String mTrace;
	
	private final Throwable mException;
	
	public ActionError(Action action, Throwable exception) {
		this(action, -1, null, null, exception);
	}
	
	public ActionError(Action action, HttpException exception) {
		this(action, exception.getStatusCode(), exception.getMessage(), null, exception);
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
