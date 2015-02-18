package org.javenstudio.provider.app.anybox;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import org.javenstudio.android.app.ActivityHelper;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;
import org.javenstudio.android.data.IDownloadable;
import org.javenstudio.android.entitydb.content.ContentHelper;
import org.javenstudio.android.entitydb.content.DownloadData;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.library.select.ISelectCallback;
import org.javenstudio.provider.library.select.ISelectData;
import org.javenstudio.provider.library.select.LocalFolderItem;
import org.javenstudio.provider.library.select.SelectFolderItem;
import org.javenstudio.provider.library.select.SelectListItem;
import org.javenstudio.provider.library.select.SelectOperation;

public class AnyboxDownloader {
	private static final Logger LOG = Logger.getLogger(AnyboxDownloader.class);

	public static boolean download(final Activity activity, 
			final AnyboxAccount account, final IDownloadable[] files) { 
		if (activity == null || account == null || files == null || files.length == 0)
			return false;
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("download: account=" + account + " fileCount=" + files.length);
		
		final SelectOperation op = account.getSelectOperation();
		final ISelectData[] roots = listRoots(op);
		
		String savePath = sSavePath;
		LocalFolderItem saveItem = null;
		
		if (roots != null && savePath != null) {
			for (ISelectData root : roots) {
				if (root != null && root instanceof LocalFolderItem) {
					LocalFolderItem item = (LocalFolderItem)root;
					String path = item.getFile().getAbsolutePath();
					if (path.equals(savePath)) {
						saveItem = item;
						break;
					}
				}
			}
		}
		
		final LocalFolderItem selectedItem = saveItem;
		
		op.showSelectDialog(activity, 
			new ISelectCallback() {
				@Override
				public CharSequence getSelectTitle() {
					int titleRes = AppResources.getInstance().getStringRes(AppResources.string.section_select_store_folder_title);
					if (titleRes == 0) titleRes = R.string.select_store_folder_title;
					return AppResources.getInstance().getResources().getString(titleRes);
				}
				@Override
				public ISelectData[] getRootList(SelectOperation op) {
					return roots;
				}
				@Override
				public boolean isSelected(SelectListItem item) {
					return selectedItem == item;
				}
				@Override
				public boolean onItemSelect(Activity activity, SelectListItem item) {
					return false;
				}
				@Override
				public boolean onActionSelect(Activity activity, SelectOperation op) {
					if (activity == null || op == null) return false;
					SelectFolderItem item = op.getCurrentFolder();
					if (item == null) item = selectedItem;
					if (item != null) return downloadTo(activity, account, files, item.getData());
					return false;
				}
				@Override
				public int getRequestCode() { 
					return REQUEST_CODE_DOWNLOAD; 
				}
				@Override
				public boolean onActivityResult(ActivityHelper helper, 
						int requestCode, int resultCode, Intent data) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("onActivityResult: requestCode=" + requestCode 
								+ " resultCode=" + resultCode + " data=" + data);
					}
					return false;
				}
			});
		
		return true; 
	}
	
	static boolean downloadTo(final Activity activity, 
			final AnyboxAccount account, final IDownloadable[] files, 
			final ISelectData folderData) { 
		if (activity == null || account == null || files == null || folderData == null)
			return false;
		
		if (folderData instanceof LocalFolderItem) {
			LocalFolderItem folder = (LocalFolderItem)folderData;
			String savePath = folder.getFile().getAbsolutePath();
			sSavePath = savePath;
			
			for (IDownloadable file : files) {
				insertQueue(account, file, savePath, null);
			}
		}
		
		return true;
	}
	
	private static String sSavePath = null;
	
	static ISelectData[] listRoots(SelectOperation op) {
		ArrayList<ISelectData> list = new ArrayList<ISelectData>();
		
		String[] paths = null;
		String savePath = sSavePath;
		if (savePath != null && savePath.length() > 0)
			paths = new String[] {savePath};
		
		ISelectData[] localRoots = LocalFolderItem.listRoots(op, paths, true);
		if (localRoots != null) {
			for (ISelectData data : localRoots) {
				if (data != null) list.add(data);
			}
		}
		
		//ISelectData[] appRoots = SelectAppItem.queryPickApps(op);
		//if (appRoots != null) {
		//	for (ISelectData data : appRoots) {
		//		if (data != null) list.add(data);
		//	}
		//}
		
		return list.toArray(new ISelectData[list.size()]);
	}
	
	static boolean insertQueue(AnyboxAccount account, IDownloadable file, 
			String savePath, String saveApp) { 
		if (account == null || file == null) 
			return false;
		
		Uri uri = file.getContentUri();
		if (uri == null) return false;
		
		String uriString = uri.toString();
		DownloadData[] uploads = ContentHelper.getInstance().queryDownloadByUri(uriString);
		if (uploads != null && uploads.length > 0) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("insertQueue: already exists, account=" + account 
						+ " file=" + file);
			}
			return false;
		}
		
		if (savePath == null) savePath = "";
		if (saveApp == null) saveApp = "";
		if (savePath.length() == 0 && saveApp.length() == 0) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("insertQueue: no save path, account=" + account 
						+ " file=" + file);
			}
			return false;
		}
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("insertQueue: account=" + account + " file=" + file 
					+ " savePath=" + savePath + " saveApp=" + saveApp);
		}
		
		DownloadData entity = ContentHelper.getInstance().newDownload();
		entity.setContentName(file.getContentName());
		entity.setContentUri(uriString);
		entity.setContentType(file.getContentType());
		entity.setSaveApp(saveApp);
		entity.setSavePath(savePath);
		entity.setSourcePrefix(AnyboxApp.PREFIX);
		entity.setSourceAccount(account.getAccountName());
		entity.setSourceAccountId(Long.toString(account.getAccountData().getId()));
		entity.setSourceFile(file.getContentId());
		entity.setSourceFileId(file.getContentId());
		entity.setStatus(DownloadData.STATUS_ADDED);
		entity.setStartTime(System.currentTimeMillis());
		
		entity.commitUpdates();
		//DownloadHandler.actionReschedule(context); // start at AppActivity.start
		
		return true;
	}
	
}
