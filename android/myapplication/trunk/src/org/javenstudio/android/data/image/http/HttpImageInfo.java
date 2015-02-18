package org.javenstudio.android.data.image.http;

import java.io.IOException;
import java.net.URI;

import org.javenstudio.android.SourceHelper;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;
import org.javenstudio.android.data.media.local.MediaDetails;
import org.javenstudio.cocoka.data.IMediaDetails;
import org.javenstudio.cocoka.storage.StorageFile;
import org.javenstudio.common.util.Logger;

final class HttpImageInfo {
	private static Logger LOG = Logger.getLogger(HttpImageInfo.class);

	private final HttpResource mResource; 
	
	private final String mLocation; 
	private final String mFilePath;
	private final String mFileName; 
	private final String mContentType; 
	private final String mSourceName;
	
	public HttpImageInfo(String location) { 
		this(HttpResource.getInstance(), location);
	}
	
	public HttpImageInfo(HttpResource res, String location) { 
		mResource = res; 
		mLocation = location; 
		
		String filepath = location;
		String filename = filepath; 
		
		boolean success = false;
		try {
			StorageFile file = mResource.getFetchCache().openCacheFile(location);
			if (file != null) { 
				filepath = file.getFilePath();
				filename = file.getFileName();
				success = true;
			}
		} catch (IOException e) { 
			if (LOG.isWarnEnabled())
				LOG.warn("open saved HttpImage: " + location + " error", e);
			
			filepath = location;
			filename = filepath; 
		}
		
		if (!success) {
			int pos1 = location.lastIndexOf('/'); 
			int pos2 = location.lastIndexOf('\\'); 
			
			filename = location; 
			if (pos1 >= 0 || pos2 >= 0) { 
				int pos = pos1 > pos2 ? pos1 : pos2; 
				filename = location.substring(pos+1); 
				if (filename == null || filename.length() == 0) 
					filename = location; 
			}
		}
		
		String contentType = "image/*"; 
		int pos = filename.lastIndexOf('.'); 
		if (pos >= 0) { 
			String ext = filename.substring(pos+1); 
			if (ext != null && ext.length() > 0) { 
				ext = ext.toLowerCase(); 
				contentType = "image/" + ext; 
			}
		}
		
		String sourceName = null;
		try {
			URI uri = new URI(location);
			String host = uri.getHost();
			
			sourceName = SourceHelper.toSourceName(host);
		} catch (Throwable e) { 
			if (LOG.isWarnEnabled())
				LOG.warn(e.toString(), e);
			
			sourceName = null;
		}
		
		mFilePath = filepath;
		mFileName = filename; 
		mContentType = contentType; 
		mSourceName = sourceName;
	}
	
	public HttpResource getResource() { return mResource; }
	
	public String getLocation() { return mLocation; }
	public String getContentType() { return mContentType; }
	public String getFilePath() { return mFilePath; }
	public String getFileName() { return mFileName; }
	public String getSourceName() { return mSourceName; }
	
	public void getDetails(IMediaDetails details) { 
		details.add(R.string.details_location, getLocation());
		details.add(R.string.details_source, getSourceName());
		//details.add(R.string.details_filename, getFileName());
		details.add(R.string.details_contenttype, getContentType());
	}
	
	public void getExifs(IMediaDetails details) { 
		MediaDetails.extractExifInfo(details, getFilePath());
	}
	
	public String getShareText() { 
		return AppResources.getInstance().getShareInformation(); 
	}
	
}
