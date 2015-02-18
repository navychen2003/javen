package org.javenstudio.android.data;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Looper;

import org.javenstudio.android.data.image.http.HttpResource;
import org.javenstudio.cocoka.worker.work.Scheduler;

public interface DataApp {

	public DataManager getDataManager();
	public CacheData getCacheData();
	public HttpResource getHttpResource();
	
	public Context getContext();
	public ContentResolver getContentResolver();
	public Activity getMainActivity();
	
	public Looper getMainLooper();
	public Scheduler getScheduler();
	
}
