package org.javenstudio.android.data.image.http;

import android.os.SystemClock;
import org.apache.http.HttpStatus;

import org.javenstudio.cocoka.net.http.HttpException;
import org.javenstudio.cocoka.net.http.fetch.FetchHelper;
import org.javenstudio.cocoka.net.http.fetch.FetchManager;
import org.javenstudio.cocoka.net.http.fetch.Fetcher;

public class HttpListener implements FetchManager.FetcherListener {
	
	public static boolean shouldCheckFetch(long lastcheck) { 
		return sLastFetchTime > lastcheck; 
	}
	
	private static long sLastFetchTime = 1; 
	
	public HttpListener() { 
		FetchHelper.getFetchManager().addListener(this); 
	} 
	
	@Override
	public final void onStarted(Fetcher fetcher) { 
		if (fetcher != null) { 
			final String source = fetcher.getTask().getSource(); 
			
			if (source != null) 
				onStarted(source);
		}
	}
	
	@Override
	public final void onCanceled(Fetcher fetcher) { 
		if (fetcher != null) { 
			final String source = fetcher.getTask().getSource(); 
			
			if (source != null) 
				onCanceled(source);
		}
	}
	
	@Override
	public final void onClosed(Fetcher fetcher) { 
		if (fetcher != null) { 
			final String source = fetcher.getTask().getSource(); 
			
			if (source != null) 
				onClosed(source);
		}
	}
	
	@Override
	public final void onFinished(Fetcher fetcher) {
		sLastFetchTime = SystemClock.elapsedRealtime(); 
		
		if (fetcher != null) { 
			final String source = fetcher.getTask().getSource(); 
			final Throwable exception = fetcher.getTask().getException(); 
			
			if (source != null) { 
				if (exception != null) { 
					if (exception instanceof HttpException) { 
						HttpException exp = (HttpException)exception; 
						if (exp.getStatusCode() == HttpStatus.SC_NOT_FOUND) { 
							onNotFound(source); return; 
						}
					}
					onFailed(source, exception); 
				} else 
					onFetched(source); 
			}
		}
	}
	
	protected void onStarted(String location) {}
	protected void onFetched(String location) {}
	protected void onCanceled(String location) {}
	protected void onClosed(String location) {}
	protected void onNotFound(String location) {}
	protected void onFailed(String location, Throwable e) {}
	
}
