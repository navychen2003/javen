package org.javenstudio.android.data.image.http;

import java.io.File;

import android.graphics.BitmapRegionDecoder;
import android.graphics.drawable.Drawable;

import org.javenstudio.android.SourceHelper;
import org.javenstudio.android.app.R;
import org.javenstudio.android.data.CacheRequest;
import org.javenstudio.android.data.DataPath;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.image.ImageBitmap;
import org.javenstudio.android.data.image.ImageResource;
import org.javenstudio.cocoka.data.IMediaDetails;
import org.javenstudio.cocoka.graphics.BitmapCache;
import org.javenstudio.cocoka.graphics.media.ImageMediaFile;
import org.javenstudio.cocoka.net.http.fetch.FetchHelper;
import org.javenstudio.cocoka.storage.StorageFile;
import org.javenstudio.cocoka.storage.fs.IFile;
import org.javenstudio.cocoka.storage.fs.LocalFile;
import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.cocoka.util.MimeType;
import org.javenstudio.cocoka.util.Utilities;
import org.javenstudio.cocoka.worker.job.Job;
import org.javenstudio.common.util.Logger;

public final class HttpImage extends Image {
	private static Logger LOG = Logger.getLogger(HttpImage.class);
	
	public enum DownloadStatus { 
		UNKNOWN, DOWNLOADED, NOT_DOWNLOAD, NOT_FOUND, ERROR, TIMEOUT
	}
	
	public enum RequestType { 
		NONE, DOWNLOAD, FORCE
	}
	
	private DownloadStatus mStatus = DownloadStatus.UNKNOWN;
	
	private final HttpResource mResource; 
	private final ImageBitmap mBitmap; 
	private ImageMediaFile mCacheFile = null;
	
	private final String mLocation; 
	private final HttpImageInfo mInfo;
	private final DataPath mPath;
	
	HttpImage(HttpResource res, String location) { 
		if (res == null || location == null) throw new NullPointerException();
		mResource = res; 
		mLocation = location; 
		mInfo = new HttpImageInfo(res, location);
		mPath = DataPath.fromString("/download/image/item/" 
				+ Utilities.toMD5(location));
		mBitmap = new ImageBitmap(this);
	}
	
	public String getLocation() { return mLocation; }
	public String getContentType() { return mInfo.getContentType(); }
	public String getFilePath() { return mInfo.getFilePath(); }
	public String getFileName() { return mInfo.getFileName(); }
	
	public DataPath getPath() { return mPath; }
	public HttpImageInfo getImageInfo() { return mInfo; }
	public ImageResource getImageProvider() { return mResource; }
	public ImageMediaFile getCacheFile() { return mCacheFile; }
	
	@Override
	public boolean isLocalItem() { return false; }
	
	@Override 
	public long getContentLength() { 
		checkDownload(RequestType.NONE, true);
		ImageMediaFile file = getCacheFile();
		if (file != null) 
			return file.getContentLength();
		return 0; 
	}
	
	@Override 
	public long getDateInMs() { 
		checkDownload(RequestType.NONE, true);
		ImageMediaFile file = getCacheFile();
		if (file != null) 
			return file.getFile().lastModified();
		return 0; 
	}
	
	private File loadCacheFile() {
		checkDownload(RequestType.NONE, true);
		ImageMediaFile cacheFile = getCacheFile();
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("loadCacheFile: cacheFile=" + cacheFile 
					+ " location=" + getLocation());
		}
		
		IFile f = cacheFile != null ? cacheFile.getFile() : null;
		if (f != null && f instanceof LocalFile) {
			LocalFile lf = (LocalFile)f;
			File file = lf.getFileImpl();
			if (file.exists()) return file;
		}
		
		return null;
	}
	
	@Override
	public Job<BitmapRef> requestImage(BitmapHolder holder, int type) {
		return new CacheRequest.LocalRequest(mResource.getApplication().getCacheData(), 
        		holder, mPath, type, getFilePath()) { 
				@Override
	    		protected File loadFile(String filePath) { 
	    			File file = loadCacheFile();
	    			if (file != null) return file;
	    			return super.loadFile(filePath); 
	    		}
			};
	}
	
	@Override
    public Job<BitmapRegionDecoder> requestLargeImage(BitmapHolder holder) {
    	return new CacheRequest.LocalLargeRequest(getFilePath()) { 
	    		@Override
	    		protected File loadFile(String filePath) { 
	    			File file = loadCacheFile();
	    			if (file != null) return file;
	    			return super.loadFile(filePath); 
	    		}
	    	};
    }
	
	@Override 
	public BitmapCache.CacheFile getBitmapFile() { 
		return BitmapCache.getInstance().getOrCreate(getLocation(), mBitmap); 
	}
	
	@Override 
	public void setBitmapVisible(boolean visible) { 
		mBitmap.setBitmapVisible(visible);
	}
	
	public synchronized boolean checkDownload(RequestType type, boolean schedule) { 
		ImageMediaFile file = mCacheFile;
		if (file != null && mStatus == DownloadStatus.DOWNLOADED) 
			return false;
		
		switch (mStatus) { 
		case UNKNOWN: 
			break;
		case DOWNLOADED: 
			return false;
		case NOT_DOWNLOAD: 
			break;
		case NOT_FOUND: 
			if (type == RequestType.NONE) 
				return false;
			break;
		default:
			break;
		}
		
		file = openOrFetch(type, schedule);
		mCacheFile = file;
		
		return file == null;
	}
	
	@Override
	public boolean existBitmap() { 
		checkDownload(RequestType.NONE, true);
		return getDownloadStatus() == DownloadStatus.DOWNLOADED;
	}
	
	@Override 
	public void getDetails(IMediaDetails details) { 
		mInfo.getDetails(details);
		details.add(R.string.details_filesize, Utilities.formatSize(getContentLength()));
		details.add(R.string.details_lastmodified, Utilities.formatDate(getDateInMs()));
		super.getDetails(details);
	}
	
	@Override 
	public void getExifs(IMediaDetails details) { 
		mInfo.getExifs(details);
	}
	
	@Override 
	public String getShareText() { 
		ImageDetails details = getImageDetails();
		if (details != null) return details.getShareText();
		return mInfo.getShareText();
	}
	
	@Override 
	public Drawable getProviderIcon() { 
		return SourceHelper.getSourceIcon(mInfo.getSourceName());
	}
	
	void resetCacheFile() { 
		mCacheFile = openOrFetch(RequestType.NONE, true); 
	}
	
	public DownloadStatus getDownloadStatus() { return mStatus; }
	void setDownloadStatus(DownloadStatus status) { mStatus = status; }
	
	HttpResource getProvider() { return mResource; }
    String getDownloadSource() { return mLocation; }
    
    final void dispatchEvent(HttpEvent.EventType type) { 
    	dispatchEvent(type, null);
    }
    
    final void dispatchEvent(HttpEvent.EventType type, Throwable e) { 
    	dispatchEvent(new HttpEvent(type, e));
    }
    
	final synchronized ImageMediaFile openOrFetch(RequestType type, boolean schedule) { 
		boolean fetch = type != RequestType.NONE;
		String source = getLocation();
		String fetchSource = getDownloadSource();
		
		//if (getDownloadStatus() == HttpImage.DownloadStatus.FAILED) 
		//	fetch = false;
		
		if (type == RequestType.FORCE) 
			FetchHelper.removeFailed(source);
		
		//if (!NetworkHelper.getInstance().isNetworkAvailable())
		//	fetch = false;
		
		StorageFile file = FetchHelper.openOrSchedule(
				getProvider().getFetchCache(), source, fetchSource, 
				MimeType.TYPE_IMAGE, fetch, schedule); 
		
		if (file != null && file.getFile().length() > 0 && file instanceof ImageMediaFile) { 
			ImageMediaFile imageFile = (ImageMediaFile)file; 
			setDownloadStatus(HttpImage.DownloadStatus.DOWNLOADED);
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("openOrFetch: found imageFile: " + file.getFilePath() 
						+ " length=" + file.getFile().length());
			}
			
			return imageFile;
		} else
			setDownloadStatus(HttpImage.DownloadStatus.NOT_DOWNLOAD);
		
		return null;
	}
    
    @Override
    public String toString() { 
    	return "HttpImage{" + mLocation + "}";
    }
    
}
