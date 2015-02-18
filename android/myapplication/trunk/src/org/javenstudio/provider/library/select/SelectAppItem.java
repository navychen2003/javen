package org.javenstudio.provider.library.select;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;

import org.javenstudio.android.ActionError;
import org.javenstudio.android.IntentHelper;
import org.javenstudio.android.app.ActivityHelper;
import org.javenstudio.common.util.Logger;

public class SelectAppItem extends SelectFileItem 
		implements ISelectData {
	private static final Logger LOG = Logger.getLogger(SelectAppItem.class);
	
	private final IntentHelper.AppInfo mAppInfo;
	
	public SelectAppItem(SelectOperation op, 
			SelectFolderItem parent, IntentHelper.AppInfo info) {
		super(op, parent);
		if (info == null) throw new NullPointerException();
		mAppInfo = info;
	}
	
	public IntentHelper.AppInfo getAppInfo() { return mAppInfo; }
	
	public static ISelectData[] queryPickApps(SelectOperation op) {
		if (op == null) return null;
		
		Intent intent = IntentHelper.getFilePickIntent();
		IntentHelper.AppInfo[] apps = IntentHelper.queryApps(op.getDataApp().getContext(), intent);
		
		if (apps != null) {
			ArrayList<ISelectData> list = new ArrayList<ISelectData>();
			
			for (IntentHelper.AppInfo info : apps) {
				if (info != null) 
					list.add(new SelectAppItem(op, null, info));
			}
			
			return list.toArray(new ISelectData[list.size()]);
		}
		
		return null;
	}

	@Override
	public String getName() {
		return getAppInfo().getResolveInfo().activityInfo.name;
	}
	
	@Override
	public CharSequence getTitle() {
		return getAppInfo().getResolveLabel();
	}

	@Override
	public String getFileInfo() {
		return getAppInfo().getPackageInfo().packageName;
	}

	@Override
	public Drawable getFileIcon() {
		return null;
	}
	
	@Override
	public Drawable getItemDrawable() {
		return getAppInfo().getPackageIcon();
	}
	
	@Override
	public ISelectData getData() {
		return this;
	}
	
	@Override
	public void onItemClick(Activity activity, final ISelectCallback callback) {
		if (activity == null || callback == null) return;
		if (LOG.isDebugEnabled()) LOG.debug("onItemClick: item=" + this);
		
		getOperation().dismissDialog();
		
		if (activity instanceof ActivityHelper.HelperApp) {
			ActivityHelper.HelperApp app = (ActivityHelper.HelperApp)activity;
			try {
				Intent intent = getAppInfo().getFilePickIntent();
				if (intent != null) {
					intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
					app.getActivityHelper().setActivityResultListener(callback);
					app.getActivityHelper().startActivityForResultSafely(intent, 
							callback.getRequestCode());
				}
			} catch (Throwable e) {
				app.getActivityHelper().onActionError(
						new ActionError(ActionError.Action.START_ACTIVITY, e));
			}
		}
	}

}
