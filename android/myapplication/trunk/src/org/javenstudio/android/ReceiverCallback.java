package org.javenstudio.android;

public interface ReceiverCallback {

	public int getResultCode(); 
	public boolean isResultOk(int resultCode); 
	
	public void abortBroadcast(); 
	
}
