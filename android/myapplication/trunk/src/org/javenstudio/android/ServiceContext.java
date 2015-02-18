package org.javenstudio.android;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.media.AudioManager;

public interface ServiceContext {

	public AlarmManager getAlarmManager(); 
	public AudioManager getAudioManager(); 
	public NotificationManager getNotificationManager(); 
	
}
