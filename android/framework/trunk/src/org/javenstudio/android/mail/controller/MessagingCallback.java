package org.javenstudio.android.mail.controller;

/**
 * Defines the interface that MessagingController will use to callback to requesters.
 */
public interface MessagingCallback {

	public void onMessagingEvent(MessagingEvent event); 
	
}
