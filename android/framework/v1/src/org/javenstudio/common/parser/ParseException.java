package org.javenstudio.common.parser;

public class ParseException extends Exception {

	private static final long serialVersionUID = 1L; 
	
	public ParseException(String msg) { 
		this(msg, (Throwable)null); 
	}
	
	public ParseException(Throwable e) { 
		this((String)null, e); 
	}
	
	public ParseException(String msg, Throwable e) { 
		super(msg, e); 
	}
	
}
