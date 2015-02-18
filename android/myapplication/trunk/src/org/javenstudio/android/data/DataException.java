package org.javenstudio.android.data;

public class DataException extends Exception {

	private static final long serialVersionUID = 1L; 
	
	public static final int CODE_ALBUM_NOTEMPTY = -1000;
	
	private final int mStatusCode; 
	private DataAction mAction = null;
	
	public DataException(int sc, String message) { 
		this(sc, message, null); 
	}
	
	public DataException(int sc, String message, Throwable e) { 
		super(message, e); 
		mStatusCode = sc; 
	}
	
	public int getStatusCode() { return mStatusCode; }
	
	public DataAction getAction() { return mAction; }
	public void setAction(DataAction action) { mAction = action; }
	
	@Override 
	public String toString() { 
		StringBuilder sbuf = new StringBuilder(); 
		String message = getMessage(); 
		Throwable cause = getCause(); 
		
		sbuf.append(getClass().getSimpleName()); 
		sbuf.append("(code=").append(Integer.toString(mStatusCode)); 
		sbuf.append(", action=").append(mAction).append(")"); 
		
		if (message != null && message.length() > 0) {
			sbuf.append(": "); 
			sbuf.append(message); 
		}
		if (cause != null) {
			sbuf.append(": "); 
			sbuf.append(cause); 
		}
		
		return sbuf.toString(); 
	}
	
}
