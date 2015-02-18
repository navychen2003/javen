package org.javenstudio.provider.app.anybox.library;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.Intent;

import org.javenstudio.android.app.ActivityHelper;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;
import org.javenstudio.android.data.IUploadable;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.app.anybox.AnyboxAccount;
import org.javenstudio.provider.app.anybox.AnyboxUploader;
import org.javenstudio.provider.library.ISectionFolder;
import org.javenstudio.provider.library.select.ISelectCallback;
import org.javenstudio.provider.library.select.ISelectData;
import org.javenstudio.provider.library.select.LocalFolderItem;
import org.javenstudio.provider.library.select.SelectAppItem;
import org.javenstudio.provider.library.select.SelectListItem;
import org.javenstudio.provider.library.select.SelectOperation;

public class AnyboxSelectUpload implements ISelectCallback {
	private static final Logger LOG = Logger.getLogger(AnyboxSelectUpload.class);

	private final AnyboxAccount mAccount;
	private final ISectionFolder mFolder;
	private final Set<SelectListItem> mSelected;
	private ISelectData[] mRoots = null;
	
	public AnyboxSelectUpload(AnyboxAccount account, 
			ISectionFolder folder) {
		if (account == null || folder == null) throw new NullPointerException();
		mAccount = account;
		mFolder = folder;
		mSelected = new HashSet<SelectListItem>();
	}
	
	public AnyboxAccount getAccountUser() { return mAccount; }
	public ISectionFolder getFolder() { return mFolder; }
	
	@Override
	public synchronized ISelectData[] getRootList(SelectOperation op) {
		if (mRoots != null) return mRoots;
		
		ArrayList<ISelectData> list = new ArrayList<ISelectData>();
		
		//addSelectData(list, getAccountUser().getLibraries());
		addSelectData(list, LocalFolderItem.listRoots(op, null, true));
		addSelectData(list, SelectAppItem.queryPickApps(op));
		
		mRoots = list.toArray(new ISelectData[list.size()]);
		return mRoots;
	}
	
	private void addSelectData(List<ISelectData> list, ISelectData[] datas) {
		if (list == null || datas == null) return;
		if (datas != null) {
			for (ISelectData data : datas) {
				if (data != null) list.add(data);
			}
		}
	}
	
	@Override
	public CharSequence getSelectTitle() {
		int titleRes = AppResources.getInstance().getStringRes(AppResources.string.section_select_upload_title);
		if (titleRes == 0) titleRes = R.string.select_upload_title;
		return AppResources.getInstance().getResources().getString(titleRes);
	}

	@Override
	public boolean isSelected(SelectListItem item) {
		return item != null && mSelected.contains(item);
	}

	@Override
	public boolean onItemSelect(Activity activity, SelectListItem item) {
		if (item == null) return false;
		if (mSelected.contains(item)) mSelected.remove(item);
		else mSelected.add(item);
		return true;
	}

	@Override
	public boolean onActionSelect(Activity activity, SelectOperation op) {
		if (activity == null || activity.isDestroyed()) return false;
		SelectListItem[] selectedList = mSelected.toArray(new SelectListItem[mSelected.size()]);
		if (LOG.isDebugEnabled()) {
			LOG.debug("onActionSelect: selectedCount=" 
					+ (selectedList != null ? selectedList.length : 0));
		}
		
		if (selectedList != null) {
			ArrayList<IUploadable> files = new ArrayList<IUploadable>();
			
			for (SelectListItem selected : selectedList) {
				if (selected == null) continue;
				ISelectData data = selected.getData();
				if (data != null && data instanceof IUploadable) {
					files.add((IUploadable)data);
				}
			}
			
			AnyboxUploader.upload(activity, getAccountUser(), 
					files.toArray(new IUploadable[files.size()]), getFolder());
		}
		
		return true;
	}
	
	@Override
	public int getRequestCode() { 
		return REQUEST_CODE_UPLOAD; 
	}
	
	@Override
	public boolean onActivityResult(ActivityHelper helper, int requestCode, 
			int resultCode, Intent data) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("onActivityResult: requestCode=" + requestCode 
					+ " resultCode=" + resultCode + " data=" + data);
		}
		return false;
	}
	
}
