package org.javenstudio.provider.app.anybox;

import android.app.Activity;
import android.net.Uri;

import org.javenstudio.android.data.IUploadable;
import org.javenstudio.android.entitydb.content.ContentHelper;
import org.javenstudio.android.entitydb.content.UploadData;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.library.ISectionFolder;

public class AnyboxUploader {
	private static final Logger LOG = Logger.getLogger(AnyboxUploader.class);

	public static boolean upload(Activity activity, 
			AnyboxAccount account, IUploadable[] files, ISectionFolder folder) { 
		if (activity == null || account == null || folder == null || 
			files == null || files.length == 0)
			return false;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("upload: account=" + account + " fileCount=" + files.length
					+ " folder=" + folder);
		}
		
		for (IUploadable file : files) {
			insertQueue(account, file, folder);
		}
		
		return true; 
	}
	
	static boolean insertQueue(AnyboxAccount account, IUploadable file, 
			ISectionFolder folder) { 
		if (account == null || file == null || folder == null) 
			return false;
		
		Uri uri = file.getContentUri();
		if (uri == null) return false;
		
		String uriString = uri.toString();
		UploadData[] uploads = ContentHelper.getInstance().queryUploadByUri(uriString);
		if (uploads != null && uploads.length > 0) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("insertQueue: already exists, account=" + account 
						+ " file=" + file);
			}
			return false;
		}
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("insertQueue: account=" + account + " file=" + file);
		
		UploadData entity = ContentHelper.getInstance().newUpload();
		entity.setContentName(file.getContentName());
		entity.setContentUri(uriString);
		entity.setContentType(file.getContentType());
		entity.setDataPath(file.getDataPath());
		entity.setFilePath(file.getFilePath());
		entity.setDestPrefix(AnyboxApp.PREFIX);
		entity.setDestAccount(account.getAccountName());
		entity.setDestAccountId(Long.toString(account.getAccountData().getId()));
		entity.setDestPath(folder.getId());
		entity.setDestPathId(folder.getId());
		entity.setStatus(UploadData.STATUS_ADDED);
		entity.setStartTime(System.currentTimeMillis());
		
		entity.commitUpdates();
		//UploadHandler.actionReschedule(context); // start at AppActivity.start
		
		return true;
	}
	
}
