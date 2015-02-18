package org.javenstudio.provider.task.upload;

import android.graphics.drawable.Drawable;

public interface IUploader {

	public String getPrefix();
	public Drawable getProviderIcon();
	public boolean startUploadThread(UploadDataInfo info);
	
}
