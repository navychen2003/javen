package org.javenstudio.cocoka.net.http;

import org.javenstudio.cocoka.net.http.HttpTask.Result;

public class HttpException extends Exception implements Result {
	
	private static final long serialVersionUID = 1L; 
	
	private final int mStatusCode; 
	private String mLocation = null; 
	
	public HttpException(int sc, String message) { 
		this(sc, message, null); 
	}
	
	public HttpException(int sc, String message, Throwable e) { 
		super(message, e); 
		
		mStatusCode = sc; 
	}
	
	public void setLocation(String location) { 
		mLocation = location; 
	}
	
	public String getLocation() { 
		return mLocation; 
	}
	
	public int getStatusCode() { 
		return mStatusCode; 
	}
	
	@Override 
	public Object getData() { 
		return getCause(); 
	}
	
	@Override 
	public String toString() { 
		StringBuilder sbuf = new StringBuilder(); 
		String message = getMessage(); 
		Throwable cause = getCause(); 
		
		sbuf.append("HttpException("+mStatusCode+")"); 
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
