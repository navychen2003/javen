package org.javenstudio.cocoka.net;

import org.javenstudio.cocoka.net.http.SimpleHttpClient;
import org.javenstudio.cocoka.net.http.download.DownloadHelper;
import org.javenstudio.cocoka.storage.StorageManager;
import org.javenstudio.cocoka.worker.work.Scheduler;

public final class Constants {
	
	public static final String DEFAULT_USER_AGENT = "Android";
	
	public static void setSocketFactoryCreator(DelegatedSSLSocketFactory.SocketFactoryCreator creator) { 
		DelegatedSSLSocketFactory.setSocketFactoryCreator(creator);
	}
	
	public static void setHttpParamsInitializer(SimpleHttpClient.HttpParamsInitializer initializer) { 
		SimpleHttpClient.setHttpParamsInitializer(initializer);
	}
	
	public static void initDownloadManager(StorageManager manager, Scheduler scheduler) { 
		DownloadHelper.initDownloadManager(manager, scheduler);
	}
	
}
