package org.javenstudio.android;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.javenstudio.android.app.ActivityHelper;
import org.javenstudio.android.app.AlertDialogBuilder;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.widget.ResultCallback;
import org.javenstudio.common.util.Logger;

public final class IntentHelper {
	private static Logger LOG = Logger.getLogger(IntentHelper.class);

	public static class AppInfo {
		private final ResolveInfo mResolveInfo;
		private final PackageInfo mPackageInfo;
		
		public AppInfo(PackageInfo pkg, ResolveInfo rsl) {
			mResolveInfo = rsl;
			mPackageInfo = pkg;
		}
		
		public ResolveInfo getResolveInfo() { return mResolveInfo; }
		public PackageInfo getPackageInfo() { return mPackageInfo; }
		
		public ComponentName getComponentName() {
			ComponentName componentName = new ComponentName(
					getResolveInfo().activityInfo.applicationInfo.packageName,
					getResolveInfo().activityInfo.name);
			return componentName;
		}
		
		public Intent getFilePickIntent() {
			Intent intent = IntentHelper.getFilePickIntent();
			intent.setComponent(getComponentName());
			return intent;
		}
		
		public Intent getImagePickIntent() {
			Intent intent = IntentHelper.getImagePickIntent();
			intent.setComponent(getComponentName());
			return intent;
		}
		
		public CharSequence getResolveLabel() {
			PackageManager packageManager = ResourceHelper.getContext().getPackageManager();
			return getResolveInfo().loadLabel(packageManager);
		}
		
		public Drawable getPackageIcon() {
			PackageManager packageManager = ResourceHelper.getContext().getPackageManager();
			return getResolveInfo().activityInfo.loadIcon(packageManager);
		}
		
		@Override
		public String toString() {
			return "AppInfo{resolveInfo=" + mResolveInfo 
					+ ",packageInfo=" + mPackageInfo + "}";
		}
	}
	
	public static AppInfo[] queryApps(Context context, Intent intent) {
		if (context == null || intent == null) return null;
		
		//Intent intent = IntentHelper.getFilePickIntent();
		PackageManager packageManager = context.getPackageManager();
		List<ResolveInfo> apps = packageManager.queryIntentActivities(intent, 0);
		
		if (apps != null) {
			ArrayList<AppInfo> list = new ArrayList<AppInfo>();
			
			for (ResolveInfo info : apps) {
				if (info == null) continue;
				if (LOG.isDebugEnabled()) 
					LOG.debug("queryApps: resolveInfo=" + info);
				
				String packageName = info.activityInfo.applicationInfo.packageName;
				//ComponentName componentName = new ComponentName(
		        //        info.activityInfo.applicationInfo.packageName,
		        //        info.activityInfo.name);
				
				//CharSequence title = info.loadLabel(packageManager); 
				//Drawable icon = info.activityInfo.loadIcon(packageManager); 
				
				PackageInfo pkg = null; 
				try {
					pkg = packageManager.getPackageInfo(packageName, 0);
				} catch (Throwable e) { 
					if (LOG.isWarnEnabled())
						LOG.warn("queryApps: getPackageInfo(" + packageName+ ") error: " + e, e);
					
					continue;
				}
				
				if (pkg != null) {
					AppInfo app = new AppInfo(pkg, info);
					list.add(app);
				}
			}
			
			return list.toArray(new AppInfo[list.size()]);
		}
		
		return null;
	}
	
	private static final int PHOTO_WIDTH = 480;
	private static final int PHOTO_HEIGHT = 854;
	
	private static final File PHOTO_DIR = new File(
            Environment.getExternalStorageDirectory() + "/DCIM/Camera");
	
	public static interface Callback { 
		public Activity getActivity();
		
		public void setResultOutputFile(File file);
		public File getResultOutputFile();
		
		public void startActivityForResult(Intent intent, int requestCode);
		
		public void requestImageCapture(ResultCallback callback);
		public void requestImagePick(ResultCallback callback);
		public void requestVideoPick(ResultCallback callback);
		public void requestAudioPick(ResultCallback callback);
		
		public void showNoImageCaptureMessage();
		public void showNoImagePickerMessage();
		public void showNoImageCroperMessage();
		public void showNoVideoPickerMessage();
		public void showNoAudioPickerMessage();
	}
	
	/**
     * Create a file name for the icon photo using current time.
     */
    public static String getPhotoFileName() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat("'IMG'_yyyyMMdd_HHmmss");
        return dateFormat.format(date) + ".jpg";
    }
	
    /**
     * Launches Camera to take a picture and store it in a file.
     */
    public static void startImageCapture(Callback activity, int requestCode) {
        try {
            // Launch camera to take photo for selected contact
            PHOTO_DIR.mkdirs();
            activity.setResultOutputFile(new File(PHOTO_DIR, getPhotoFileName()));
            final Intent intent = getImageCaptureIntent(activity.getResultOutputFile());
            activity.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            activity.showNoImageCaptureMessage();
        }
    }
    
    public static void startVideoPick(Callback activity, int requestCode) {
    	try {
    		final Intent intent = getVideoPickIntent(); 
    		activity.startActivityForResult(intent, requestCode);
	    } catch (ActivityNotFoundException e) {
	        activity.showNoVideoPickerMessage();
	    }
    }
    
    public static void startAudioPick(Callback activity, int requestCode) {
    	try {
    		final Intent intent = getAudioPickIntent(); 
    		activity.startActivityForResult(intent, requestCode);
	    } catch (ActivityNotFoundException e) {
	        activity.showNoAudioPickerMessage();
	    }
    }
    
    /**
     * Launches Gallery to pick a photo.
     */
    public static void startImagePickFromGallery(Callback activity, int requestCode) {
        try {
            // Launch picker to choose photo for selected contact
            final Intent intent = getImagePickIntent();
            activity.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
        	activity.showNoImagePickerMessage();
        }
    }
    
	/**
     * Constructs an intent for picking a photo from Gallery, cropping it and returning the bitmap.
     */
    public static Intent getImageCropPickIntent(boolean crop) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
        intent.setType("image/*");
        intent.putExtra("crop", crop?"true":"false");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", PHOTO_WIDTH);
        intent.putExtra("outputY", PHOTO_HEIGHT);
        intent.putExtra("return-data", true);
        return intent;
    }
    
    public static Intent getImagePickIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
        intent.setType("image/*");
        return intent;
    }
	
    public static Intent getAudioPickIntent() {
    	Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null); 
		intent.setType("audio/*");
		return intent; 
    }
    
    public static Intent getVideoPickIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
        intent.setType("video/*");
        return intent;
    }
    
    public static Intent getFilePickIntent() {
    	Intent intent = new Intent(Intent.ACTION_GET_CONTENT); 
		intent.setType("file/*");
		return intent; 
    }
    
    /**
     * Constructs an intent for image cropping.
     */
    public static Intent getImageCropIntent(Uri photoUri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(photoUri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", PHOTO_WIDTH);
        intent.putExtra("outputY", PHOTO_HEIGHT);
        intent.putExtra("return-data", true);
        return intent;
    }
    
    /**
     * Constructs an intent for capturing a photo and storing it in a temporary file.
     */
    public static Intent getImageCaptureIntent(File f) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE, null);
        //intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
        return intent;
    }
    
    /**
     * Sends a newly acquired photo to Gallery for cropping
     */
    public static void doCropPhoto(Callback activity, File f, int requestCode) {
        try {
            // Add the image to the media store, 2.1 not support
            MediaScannerConnection.scanFile(
            		activity.getActivity(),
                    new String[] { f.getAbsolutePath() },
                    new String[] { null },
                    null);

            // Launch gallery to crop the photo
            final Intent intent = getImageCropIntent(Uri.fromFile(f));
            activity.startActivityForResult(intent, requestCode);
        } catch (Exception e) {
        	LOG.error("Cannot crop image", e);
        	activity.showNoImageCroperMessage();
        }
    }
    
    public static boolean showChooserDialog(final Activity activity, 
    		Intent intent, CharSequence title) {
		if (activity == null || intent == null) 
			return false;
		
		AppInfo[] apps = queryApps(activity, intent);
		AppAdapter adapter = new AppAdapter(activity, apps, intent);
		
		AlertDialogBuilder builder = AppResources.getInstance().createDialogBuilder(activity);
		builder.setCancelable(true);
		builder.setTitle(title);
		builder.setAdapter(adapter, null);
		
		//builder.setNegativeButton(R.string.dialog_cancel_button, 
		//		new DialogInterface.OnClickListener() {
		//			@Override
		//			public void onClick(DialogInterface dialog, int which) {
		//				dialog.dismiss();
		//			}
		//		});
		
		//builder.setPositiveButton(R.string.dialog_select_button, 
		//		new DialogInterface.OnClickListener() {
		//			@Override
		//			public void onClick(DialogInterface dialog, int which) {
		//				dialog.dismiss();
		//				//onDialogSelectClick(dialog);
		//			}
		//		});
		
		builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
				}
			});
		
		AlertDialog dialog = builder.show(activity);
		adapter.mDialog = dialog;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("showChooserDialog: activity=" + activity 
					+ " dialog=" + dialog);
		}
		
		return true; 
	}
    
    private static class AppAdapter extends ArrayAdapter<AppInfo> {
    	private final Activity mActivity;
    	private final Intent mIntent;
    	private AlertDialog mDialog = null;
    	
    	public AppAdapter(Activity activity, AppInfo[] items, Intent intent) {
    		super(activity, 0, items);
    		mActivity = activity;
    		mIntent = intent;
    	}
    	
    	@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			AppInfo item = getItem(position);
			return getAppItemView(this, item, convertView, parent);
		}
    	
		@Override
		public boolean isEnabled(int position) {
			return false;
		}
    }
    
    private static View getAppItemView(final AppAdapter adapter, 
    		final AppInfo item, View convertView, ViewGroup parent) {
    	if (adapter == null || item == null) return null;
    	
		final LayoutInflater inflater = LayoutInflater.from(adapter.mActivity);
		final View view = inflater.inflate(R.layout.select_list_file_item, null);
		
		final TextView titleView = (TextView)view.findViewById(R.id.select_list_item_title);
		if (titleView != null) {
			titleView.setText(item.getResolveLabel());
			titleView.setVisibility(View.VISIBLE);
		}
		
		final TextView subtitleView = (TextView)view.findViewById(R.id.select_list_item_subtitle);
		if (subtitleView != null) {
			subtitleView.setText(item.getPackageInfo().packageName);
			subtitleView.setVisibility(View.VISIBLE);
		}
		
		final ImageView iconView = (ImageView)view.findViewById(R.id.select_list_item_poster_image);
		if (iconView != null) {
			Drawable icon = item.getPackageIcon();
			if (icon != null) iconView.setImageDrawable(icon);
			iconView.setVisibility(View.VISIBLE);
		}
		
		View layoutView = view;
		if (layoutView != null) {
			int itembgRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.section_item_background);
			if (itembgRes != 0) layoutView.setBackgroundResource(itembgRes);
			
			layoutView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						AlertDialog dialog = adapter.mDialog;
						if (dialog != null) dialog.dismiss();
						onAppItemClick(adapter, item);
					}
				});
		}
		
		return view;
    }
    
    private static void onAppItemClick(AppAdapter adapter, AppInfo item) {
    	if (adapter == null || item == null) return;
    	
    	Intent itemIntent = new Intent(adapter.mIntent);
    	itemIntent.setComponent(item.getComponentName());
    	
    	try {
    		adapter.mActivity.startActivity(itemIntent);
    		
    	} catch (Throwable e) {
    		if (adapter.mActivity instanceof ActivityHelper.HelperApp) {
    			ActivityHelper helper = ((ActivityHelper.HelperApp)
    					adapter.mActivity).getActivityHelper();
    			if (helper != null) {
    				helper.onActionError(new ActionError(
    						ActionError.Action.START_ACTIVITY, e));
    			}
    		}
    	}
    }
    
}
