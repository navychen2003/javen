package org.javenstudio.android.data.image;

import java.io.File;

import android.graphics.BitmapRegionDecoder;

import org.javenstudio.android.data.CacheRequest;
import org.javenstudio.android.data.DataApp;
import org.javenstudio.android.data.DataPath;
import org.javenstudio.cocoka.data.IMediaDetails;
import org.javenstudio.cocoka.graphics.BitmapCache;
import org.javenstudio.cocoka.util.BitmapFile;
import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.cocoka.worker.job.Job;

public class FileImage extends Image {

	private final DataApp mApp;
	private final ImageBitmap mBitmap;
	private final FileImageInfo mInfo;
	private final DataPath mPath;
	private final String mLocation;
	
	public FileImage(DataApp app, DataPath path, String filepath) { 
		if (app == null || path == null || filepath == null)
			throw new NullPointerException();
		mApp = app;
		mLocation = filepath;
		mPath = path;
		mInfo = new FileImageInfo(filepath);
		mBitmap = new ImageBitmap(this);
	}
	
	@Override
	public BitmapFile getBitmapFile() {
		return BitmapCache.getInstance().getOrCreate(mLocation, mBitmap);
	}

	public DataApp getDataApp() { return mApp; }
	public String getLocation() { return mLocation; }
	
	public FileInfo getFileInfo() { return mInfo; }
	public String getFilePath() { return mInfo.getFilePath(); }
	public String getFileName() { return mInfo.getFileName(); }
	public String getContentType() { return mInfo.getContentType(); }

	public DataPath getPath() { return mPath; }
	public File getFile() { return mInfo.getFile(); }
	
	@Override 
	public boolean existBitmap() { 
		return mInfo.exists();
	}
	
	@Override 
	public void setBitmapVisible(boolean visible) { 
		mBitmap.setBitmapVisible(visible);
	}
	
	@Override
	public boolean isLocalItem() { return true; }
	
	@Override
	public Job<BitmapRef> requestImage(BitmapHolder holder, int type) {
		return new CacheRequest.LocalRequest(mApp.getCacheData(), 
        		holder, mPath, type, getFilePath());
	}

	@Override
    public Job<BitmapRegionDecoder> requestLargeImage(BitmapHolder holder) {
    	return new CacheRequest.LocalLargeRequest(getFilePath());
    }
	
	@Override 
	public long getContentLength() { 
		return getFile().length(); 
	}
	
	@Override 
	public long getDateInMs() { 
		return getFile().lastModified(); 
	}
	
	@Override 
	public void getDetails(IMediaDetails details) { 
		mInfo.getDetails(details);
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
	
}
