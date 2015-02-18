package org.javenstudio.provider.app.anybox.library;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;

import org.javenstudio.android.ActionError;
import org.javenstudio.android.IntentHelper;
import org.javenstudio.android.app.ActivityHelper;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.app.anybox.AnyboxAccount;
import org.javenstudio.provider.library.ISectionFolder;
import org.javenstudio.provider.library.SelectDialogHelper;
import org.javenstudio.provider.library.select.ISelectCallback;

public class AnyboxSelectScan extends SelectDialogHelper 
		implements ActivityHelper.ActivityResultListener {
	private static final Logger LOG = Logger.getLogger(AnyboxSelectScan.class);

	public abstract class AppItem extends SelectItem {
		private final IntentHelper.AppInfo mAppInfo;
		
		public AppItem(IntentHelper.AppInfo app) {
			if (app == null) throw new NullPointerException();
			mAppInfo = app;
		}
		
		public IntentHelper.AppInfo getAppInfo() { return mAppInfo; }
		
		@Override
		public CharSequence getTitle() {
			return getAppInfo().getResolveLabel();
		}
		
		@Override
		public CharSequence getSubTitle() {
			return getAppInfo().getPackageInfo().packageName;
		}
		
		@Override
		public Drawable getItemDrawable() {
			return getAppInfo().getPackageIcon();
		}
	}
	
	public class PickAppItem extends AppItem {
		public static final int REQUEST_CODE = ISelectCallback.REQUEST_CODE_IMAGE_PICK;
		
		public PickAppItem(IntentHelper.AppInfo app) {
			super(app);
		}
		
		@Override
		public void onItemClick(Activity activity) {
			dismissDialog();
			if (activity == null) return;
			
			if (activity instanceof ActivityHelper.HelperApp) {
				ActivityHelper.HelperApp app = (ActivityHelper.HelperApp)activity;
				try {
					ActivityHelper.ActivityResultListener callback = AnyboxSelectScan.this;
					Intent intent = getAppInfo().getImagePickIntent();
					if (intent != null) {
						intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
						app.getActivityHelper().setActivityResultListener(callback);
						app.getActivityHelper().startActivityForResultSafely(intent, 
								REQUEST_CODE);
					}
				} catch (Throwable e) {
					app.getActivityHelper().onActionError(
							new ActionError(ActionError.Action.START_ACTIVITY, e));
				}
			}
		}
	}
	
	public class CaptureAppItem extends AppItem {
		public static final int REQUEST_CODE = ISelectCallback.REQUEST_CODE_IMAGE_CAPTURE;
		
		public CaptureAppItem(IntentHelper.AppInfo app) {
			super(app);
		}
		
		@Override
		public void onItemClick(Activity activity) {
			dismissDialog();
		}
	}
	
	private final AnyboxAccount mAccount;
	
	public AnyboxSelectScan(AnyboxAccount account) {
		if (account == null) throw new NullPointerException();
		mAccount = account;
	}
	
	public AnyboxAccount getAccountUser() { return mAccount; }
	
	public ISelectItem[] getSelectItems(IActivity activity, ISectionFolder folder) {
		if (activity == null) return null;
		
		ArrayList<ISelectItem> list = new ArrayList<ISelectItem>();
		
		addPickItem(list, IntentHelper.queryApps(activity.getActivity(), 
				IntentHelper.getImagePickIntent()));
		
		addCaptureItem(list, IntentHelper.queryApps(activity.getActivity(), 
				IntentHelper.getImageCaptureIntent(null)));
		
		return list.toArray(new ISelectItem[list.size()]);
	}
	
	private void addPickItem(List<ISelectItem> list, IntentHelper.AppInfo[] apps) {
		if (list == null || apps == null) return;
		if (apps != null && apps.length > 0) {
			list.add(new SelectCategory(R.string.select_photo_pick_category));
			
			for (IntentHelper.AppInfo app : apps) {
				if (app == null) continue;
				list.add(new PickAppItem(app));
			}
		}
	}
	
	private void addCaptureItem(List<ISelectItem> list, IntentHelper.AppInfo[] apps) {
		if (list == null || apps == null) return;
		if (apps != null && apps.length > 0) {
			list.add(new SelectCategory(R.string.select_photo_capture_category));
			
			for (IntentHelper.AppInfo app : apps) {
				if (app == null) continue;
				list.add(new CaptureAppItem(app));
			}
		}
	}
	
	public void showSelectDialog(IActivity activity, ISectionFolder folder) {
		if (activity == null) return;
		
		showSelectDialog(activity.getActivity(), 
				getSelectItems(activity, folder), R.string.select_photo_title);
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
