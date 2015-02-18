package org.javenstudio.android.entitydb.content;

public class ContentException extends Exception {
	public static final long serialVersionUID = -1L;

	public ContentException(String message) {
        super(message);
	}
	
	public ContentException(String message, Throwable throwable) {
		super(message, throwable);
	}
	
}
