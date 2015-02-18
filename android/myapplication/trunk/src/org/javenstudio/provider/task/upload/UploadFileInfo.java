package org.javenstudio.provider.task.upload;

import java.io.File;

import org.javenstudio.android.StorageHelper;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;
import org.javenstudio.android.data.DataPath;
import org.javenstudio.android.data.image.FileInfo;
import org.javenstudio.cocoka.data.IMediaDetails;
import org.javenstudio.cocoka.util.MimeTypes;
import org.javenstudio.cocoka.util.Utilities;

import android.graphics.drawable.Drawable;

public class UploadFileInfo extends FileInfo {
	//private static Logger LOG = Logger.getLogger(UploadFileInfo.class);

	private final String mLocation; 
	private final String mFilePath;
	private final String mFileName; 
	private final String mContentType; 
	private final DataPath mPath;
	private final File mFile;
	
	public UploadFileInfo(String filepath, String contentType) { 
		if (filepath == null) throw new NullPointerException();
		mLocation = filepath; 
		mFile = new File(filepath);
		mPath = DataPath.fromString("/local/file/item/" + Utilities.toMD5(filepath));
		
		String location = filepath;
		String filename = filepath; 
		
		if (true) {
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
		
		if (contentType == null || contentType.length() == 0)
			contentType = MimeTypes.lookupFileContentType(filename); 
		
		mFilePath = filepath;
		mFileName = filename; 
		mContentType = contentType; 
	}
	
	public String getLocation() { return mLocation; }
	public String getContentType() { return mContentType; }
	public String getFilePath() { return mFilePath; }
	public String getFileName() { return mFileName; }
	
	public DataPath getDataPath() { return mPath; }
	public File getFile() { return mFile; }
	
	public boolean exists() { return mFile.exists(); }
	public long getFileLength() { return getFile().length(); }
	public long getModifiedTime() { return getFile().lastModified(); }
	
	public void getDetails(IMediaDetails details) { 
		details.add(R.string.details_filepath, getFilePath());
		//details.add(R.string.details_filename, getFileName());
		details.add(R.string.details_contenttype, getContentType());
		if (exists()) {
			details.add(R.string.details_filesize, Utilities.formatSize(getFile().length()));
			details.add(R.string.details_lastmodified, Utilities.formatDate(getFile().lastModified()));
		}
	}
	
	public void getExifs(IMediaDetails details) { 
		//MediaDetails.extractExifInfo(details, getFilePath());
	}
	
	public String getShareText() { 
		return AppResources.getInstance().getShareInformation(); 
	}
	
	public Drawable getTypeIcon() {
		return StorageHelper.getFileTypeIcon(getFileName(), getContentType(), null);
	}
	
}
