package org.javenstudio.android.data.image.http;

import org.javenstudio.android.data.image.ImageEvent;

public class HttpEvent extends ImageEvent {

	public enum EventType { 
		FETCH_START, FETCH_FINISH, NOT_FOUND, FETCH_ERROR, FETCH_TIMEOUT
	}
	
	private final EventType mType;
	
	HttpEvent(EventType type) { 
		this(type, null);
	}
	
	public HttpEvent(EventType type, Throwable e) { 
		super(type.toString(), e);
		mType = type;
	}
	
	public final EventType getEventType() { return mType; }
	
}
