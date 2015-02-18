package org.javenstudio.android.data.media.download;

import android.graphics.BitmapRegionDecoder;
import android.graphics.drawable.Drawable;

import org.javenstudio.android.SourceHelper;
import org.javenstudio.android.app.R;
import org.javenstudio.android.data.DataAction;
import org.javenstudio.android.data.DataApp;
import org.javenstudio.android.data.DataException;
import org.javenstudio.android.data.DataPath;
import org.javenstudio.android.data.image.FileImage;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.cocoka.data.IMediaDetails;
import org.javenstudio.cocoka.storage.fs.IFile;
import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.cocoka.worker.job.Job;
import org.javenstudio.common.util.Logger;

final class DownloadImage extends DownloadMediaItem {
	private static final Logger LOG = Logger.getLogger(DownloadImage.class);
	
	private final DownloadMediaSource mSource;
	private final DownloadAlbumSet mAlbumSet;
	private final FileImage mImage;
	private final IFile mFile;
	private final String mSourceName;
	
	public DownloadImage(DownloadMediaSource source, 
			DownloadAlbumSet albumSet, DataPath path, 
			IFile file, String sourceName) { 
		super(path, nextVersionNumber());
		mSource = source;
		mAlbumSet = albumSet;
		mFile = file;
		mSourceName = sourceName;
		mImage = new FileImage(source.getDataApp(), 
				path, file.getAbsolutePath());
		
		dateTakenInMs = file.lastModified();
		fileSize = file.length();
	}

	public final DownloadMediaSource getMediaSource() { return mSource; }
	public final DownloadAlbumSet getAlbumSet() { return mAlbumSet; }
	
	public final IFile getFile() { return mFile; }
	public final String getSourceName() { return mSourceName; }
	
	@Override
	public String getName() {
		return getFile().getName();
	}

	@Override
	public DataApp getDataApp() { 
		return mSource.getDataApp();
	}
	
    @Override
    public String getLocation() { 
    	return getFile().getAbsolutePath();
    }
	
    @Override
    public int getMediaType() {
        return MEDIA_TYPE_IMAGE;
    }
    
	@Override
	public Job<BitmapRef> requestImage(BitmapHolder holder, int type) {
		return mImage.requestImage(holder, type);
	}

	@Override
	public Job<BitmapRegionDecoder> requestLargeImage(BitmapHolder holder) {
		return mImage.requestLargeImage(holder);
	}

	@Override
	public Image getBitmapImage() { return mImage; }
	
	@Override
	public void getDetails(IMediaDetails details) { 
		super.getDetails(details);
		details.add(R.string.details_source, getSourceName());
	}
	
	@Override
	public Drawable getProviderIcon() { 
		return SourceHelper.getSourceIcon(getSourceName());
	}
	
	@Override
    public boolean delete() throws DataException {
		final IFile file = mFile;
		if (file != null) { 
			boolean existed = file.exists();
			boolean deleted = false;
			
			try {
				if (existed) { 
					deleted = file.getFileSystem().delete(file); 
					if (deleted) 
						mAlbumSet.removeMediaItem(this);
				}
			} catch (Throwable e) {
				if (LOG.isWarnEnabled()) 
					LOG.warn("delete: " + file + " error: " + e.toString(), e);
				
				DataException ex = new DataException(-1, e.getMessage(), e);
				ex.setAction(DataAction.DELETE);
				throw ex;
				
				//return deleted;
			}
			
			if (LOG.isDebugEnabled())
				LOG.debug("delete: " + file + " existed=" + existed + " deleted=" + deleted);
			
			return deleted;
		}
		
		return false;
	}
	
}
