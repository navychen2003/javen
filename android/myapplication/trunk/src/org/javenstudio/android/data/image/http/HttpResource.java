package org.javenstudio.android.data.image.http;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.android.data.DataApp;
import org.javenstudio.android.data.image.ImageResource;
import org.javenstudio.cocoka.net.http.fetch.FetchCache;
import org.javenstudio.cocoka.net.http.fetch.FetchHelper;
import org.javenstudio.cocoka.storage.Storage;
import org.javenstudio.cocoka.storage.StorageManager;
import org.javenstudio.cocoka.storage.fs.IFile;
import org.javenstudio.common.util.Logger;

public abstract class HttpResource extends ImageResource {
	private static final Logger LOG = Logger.getLogger(HttpResource.class);

	private static HttpResource sInstance = null;
	public static HttpResource getInstance() { 
		synchronized (HttpResource.class) { 
			if (sInstance == null) 
				throw new RuntimeException("HttpImageResource instance not initialized");
			
			return sInstance;
		}
	}
	
	private final Map<String, HttpImage> mImages = new HashMap<String, HttpImage>();
	private FetchCache mFetchCache = null;
	
	private final DataApp mApplication;
	@SuppressWarnings("unused")
	private final HttpListener mListener;
	
	public HttpResource(DataApp app) { 
		synchronized (HttpResource.class) { 
			if (sInstance != null) 
				throw new RuntimeException("HttpImageResource already initialized");
			
			sInstance = this;
		}
		
		mApplication = app;
		mListener = createListener();
	}
	
	@Override
	public DataApp getApplication() { 
		return mApplication;
	}
	
	@Override
	public final FetchCache getStorageProvider() { 
		return getFetchCache();
	}
	
	@Override
	public final HttpImage getImage(String source) { 
		if (source == null || source.length() == 0) 
			return null;
		
		synchronized (mImages) { 
			HttpImage image = mImages.get(source);
			if (image == null) {
				image = new HttpImage(this, source);
				mImages.put(source, image);
			}
			
			return image;
		}
	}
	
	private static final String IMAGE_NAME = "image";
	private static final String IMAGE_VERSION_FILE = "version.0";
	private static boolean sImageDiirectoryChecked = false;
	private static Object sImageLock = new Object();
	
	protected FetchCache createFetchCache(StorageManager manager) throws IOException { 
		synchronized (sImageLock) { 
			if (!sImageDiirectoryChecked) { 
				Storage storage = manager.getStorage(IMAGE_NAME);
				IFile versionFile = storage.newFsFileByName(IMAGE_VERSION_FILE);
				if (!versionFile.exists()) { 
					if (LOG.isInfoEnabled()) 
						LOG.info("clear old files in " + storage.getDirectory().getAbsolutePath());
					
					storage.clearDirectory();
					storage.getFileSystem().createNewFile(versionFile);
				}
				sImageDiirectoryChecked = true;
			}
		}
		return createHttpCache(manager.getStorage(IMAGE_NAME + "/download"));
	}
	
	protected abstract HttpCache createHttpCache(Storage store) throws IOException;
	
	public final FetchCache getFetchCache() { 
		synchronized (this) { 
			if (mFetchCache == null) {
				try {
					StorageManager manager = FetchHelper.getFetchManager().getStorageManager();
					mFetchCache = createFetchCache(manager);
				} catch (IOException e) { 
					throw new RuntimeException("gallery storage init error", e);
				}
			}
			return mFetchCache;
		}
	}
	
	private HttpListener createListener() { 
		return new HttpListener() { 
			@Override
			protected void onStarted(String location) { 
				HttpImage image = null;
				synchronized (mImages) { 
					image = mImages.get(location);
				}
				if (image != null) 
					image.dispatchEvent(HttpEvent.EventType.FETCH_START);
			}
			
			@Override
			protected void onFetched(String location) { 
				HttpImage image = null;
				synchronized (mImages) { 
					image = mImages.get(location);
				}
				if (image != null) {
					image.resetCacheFile();
					image.setDownloadStatus(HttpImage.DownloadStatus.DOWNLOADED);
					image.dispatchEvent(HttpEvent.EventType.FETCH_FINISH);
				}
			}
			
			@Override
			protected void onNotFound(String location) { 
				HttpImage image = null;
				synchronized (mImages) { 
					image = mImages.get(location);
				}
				if (image != null) {
					image.setDownloadStatus(HttpImage.DownloadStatus.NOT_FOUND);
					image.dispatchEvent(HttpEvent.EventType.NOT_FOUND);
				}
			}
			
			@Override
			protected void onFailed(String location, Throwable e) { 
				HttpImage image = null;
				synchronized (mImages) { 
					image = mImages.get(location);
				}
				if (image != null) {
					if (LOG.isDebugEnabled())
						LOG.debug("onFailed: image=" + image + " exception=" + e);
					
					boolean socketTimeout = false;
					if (e != null) {
						String msg = e.toString();
						if (msg != null) {
							msg = msg.toLowerCase();
							if (msg.indexOf("timeout") >= 0)
								socketTimeout = true;
						}
					}
					
					if (socketTimeout) {
						image.setDownloadStatus(HttpImage.DownloadStatus.TIMEOUT);
						image.dispatchEvent(HttpEvent.EventType.FETCH_TIMEOUT, e);
					} else {
						image.setDownloadStatus(HttpImage.DownloadStatus.ERROR);
						image.dispatchEvent(HttpEvent.EventType.FETCH_ERROR, e);
					}
				}
			}
		};
	}
	
}
