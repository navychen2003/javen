package org.javenstudio.provider.library.select;

import android.graphics.drawable.Drawable;
import android.net.Uri;

import org.javenstudio.android.StorageHelper;
import org.javenstudio.android.data.DataPath;
import org.javenstudio.android.data.IUploadable;
import org.javenstudio.android.data.image.FileImage;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.cocoka.storage.fs.LocalFile;
import org.javenstudio.cocoka.util.MimeTypes;
import org.javenstudio.provider.library.SectionHelper;

public class LocalFileItem extends SelectFileItem 
		implements ISelectData, IUploadable {

	private final LocalFile mFile;
	private final FileImage mImage;
	private final String mType;
	private final String mDataPath;
	private final Uri mUri;
	
	public LocalFileItem(SelectOperation op, 
			LocalFolderItem parent, LocalFile file) {
		super(op, parent);
		if (file == null) throw new NullPointerException();
		mFile = file;
		
		FileImage image = null;
		String mimeType = null;
		String datapath = file.getPath();
		
		if (file.isFile()) {
			mimeType = MimeTypes.lookupFileContentType(file.getName());
			if (mimeType != null && mimeType.startsWith("image/")) {
				image = new FileImage(op.getDataApp(), DataPath.fromString(datapath), 
						file.getAbsolutePath());
			}
		}
		
		if (mimeType == null || mimeType.length() == 0)
			mimeType = "application/*";
		
		mImage = image;
		mType = mimeType;
		mUri = Uri.fromFile(file.getFileImpl());
		mDataPath = datapath;
	}

	@Override
	public String getName() {
		return mFile.getName();
	}

	@Override
	public String getFileInfo() {
		return SectionHelper.getFileSizeInfo(mFile);
	}

	@Override
	public Drawable getFileIcon() {
		return StorageHelper.getFileTypeIcon(mFile.getName());
	}

	@Override
	public ISelectData getData() {
		return this;
	}
	
	@Override
	public Image getFirstImage(boolean existBitmap) { 
		return mImage;
	}
	
	@Override
	public Drawable getItemDrawable(final int width, final int height) {
		final int showCount = getShowImageCount();
		if (showCount > 0) {
			Image image = getFirstImage(false); 
			if (image != null) { 
				return image.getThumbnailDrawable(width, height);
			}
		}
		return null;
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{id=" + getIdentity() 
				+ ",file=" + mFile + "}";
	}

	@Override
	public Uri getContentUri() {
		return mUri;
	}

	@Override
	public String getContentId() {
		return Long.toString(getIdentity());
	}
	
	@Override
	public String getContentName() {
		return getName();
	}

	@Override
	public String getContentType() {
		return mType;
	}

	@Override
	public String getDataPath() {
		return mDataPath;
	}

	@Override
	public String getFilePath() {
		return mFile.getAbsolutePath();
	}
	
}
