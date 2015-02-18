package org.javenstudio.lightning.logging;

public class ListenerConfig {
	
	private int mSize = 50;
	private String mThreshold = null;
	// Down the line, settings for URL/core to store logging
	
	public ListenerConfig(String threshold, int size) { 
		mThreshold = threshold;
		mSize = size;
	}
	
	public final int getSize() { return mSize; }
	public final String getThreshold() { return mThreshold; }
	
}
