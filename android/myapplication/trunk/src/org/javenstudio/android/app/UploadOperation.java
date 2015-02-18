package org.javenstudio.android.app;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.javenstudio.android.IntentHelper;
import org.javenstudio.android.data.media.MediaItem;
import org.javenstudio.cocoka.app.IMenuItem;
import org.javenstudio.cocoka.widget.ResultCallback;

public class UploadOperation extends MenuOperation.Operation 
		implements ActivityHelper.ActivityResultListener {

	public static interface Callback extends ResultCallback {
		public void onResult(Context context, int code, Object result); 
	}
	
	public static final int REQUEST_CODE_IMAGE_CAPTURE = 1501;
	public static final int REQUEST_CODE_IMAGE_PICK = 1502;
	public static final int REQUEST_CODE_VIDEO_PICK = 1503;
	public static final int REQUEST_CODE_AUDIO_PICK = 1504;
	
	private final Callback mCallback; 
	private File mOutputFile = null; 
	
	public UploadOperation(Callback callback, int itemId) { 
		super(itemId);
		mCallback = callback;
	}
	
	@Override
	public boolean isEnabled() { return true; }
	
	@Override
	public boolean onOptionsItemSelected(final Activity activity, IMenuItem item) { 
		if (activity == null || item == null) 
			return false;
		
		if (item.getItemId() != getItemId()) 
			return false;
		
		openOperation(activity);
		
		return true;
	}
	
	static class LongClickItem { 
		public final int mTextRes;
		public final int mIconRes;
		public LongClickItem(int textRes, int iconRes) { 
			mTextRes = textRes;
			mIconRes = iconRes;
		}
	}
	
	private boolean openOperation(final Activity activity) { 
		if (activity == null) return false;
		
		final LongClickItem[] items = new LongClickItem[] { 
				new LongClickItem(R.string.label_action_take_photo, 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_camera)), 
				new LongClickItem(R.string.label_action_pick_photo, 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_photo)),
				new LongClickItem(R.string.label_action_pick_video, 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_video))
			};
		
		ArrayAdapter<?> adapter = new ArrayAdapter<LongClickItem>(activity, 0, items) { 
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				final LayoutInflater inflater = LayoutInflater.from(getContext());
				
				if (convertView == null) 
					convertView = inflater.inflate(R.layout.dialog_item, null);
				
				LongClickItem item = getItem(position);
				TextView textView = (TextView)convertView.findViewById(R.id.dialog_item_text);
				
				if (item != null && textView != null) { 
					textView.setText(item.mTextRes);
					textView.setCompoundDrawablesWithIntrinsicBounds(item.mIconRes, 0, 0, 0);
				}
				
				return convertView;
			}
		};
		
		AlertDialogBuilder builder = AppResources.getInstance().createDialogBuilder(activity);
		builder.setCancelable(true);
		//builder.setTitle(R.string.label_select_operation);
		builder.setAdapter(adapter, 
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (which == 0) takePhoto(activity);
					else if (which == 1) pickPhoto(activity);
					else if (which == 2) pickVideo(activity);
					dialog.dismiss();
				}
			});
		
		builder.show(activity);
		
		return true; 
	}
	
	private void takePhoto(Activity activity) { 
		if (activity instanceof IActivity) { 
			IActivity a = (IActivity)activity;
			a.getActivityHelper().setActivityResultListener(this);
			
			RequestHelper helper = new RequestHelper(a);
			helper.requestImageCapture(mCallback);
		}
	}
	
	private void pickPhoto(Activity activity) { 
		if (activity instanceof IActivity) { 
			IActivity a = (IActivity)activity;
			a.getActivityHelper().setActivityResultListener(this);
			
			RequestHelper helper = new RequestHelper(a);
			helper.requestImagePick(mCallback);
		}
	}
	
	private void pickVideo(Activity activity) { 
		if (activity instanceof IActivity) { 
			IActivity a = (IActivity)activity;
			a.getActivityHelper().setActivityResultListener(this);
			
			RequestHelper helper = new RequestHelper(a);
			helper.requestVideoPick(mCallback);
		}
	}
	
	@Override
	public boolean onActivityResult(ActivityHelper helper, 
			int requestCode, int resultCode, Intent data) { 
		final Callback callback = mCallback; 
		helper.setActivityResultListener(null);
    	//mResultCallback = null; 
		
    	if (resultCode == Activity.RESULT_OK) {
    		switch (requestCode) {
        	case REQUEST_CODE_IMAGE_CAPTURE: 
        		if (callback != null) {
        			File outputFile = mOutputFile; 
        			if (outputFile != null) 
        				data.putExtra("outputfile", outputFile.getAbsolutePath()); 
		    	  	callback.onResult(helper.getActivity(), MediaItem.MEDIA_TYPE_IMAGE, data); 
		    	  	//actionTasks(this);
        		}
        		mOutputFile = null; 
        		return true; 
        		
        	case REQUEST_CODE_IMAGE_PICK: 
	    	  	if (callback != null) {
	    	  		callback.onResult(helper.getActivity(), MediaItem.MEDIA_TYPE_IMAGE, data); 
	    	  		//actionTasks(this);
	    	  	}
        		return true; 
        		
        	case REQUEST_CODE_VIDEO_PICK: 
	    	  	if (callback != null) {
	    	  		callback.onResult(helper.getActivity(), MediaItem.MEDIA_TYPE_VIDEO, data); 
	    	  		//actionTasks(this);
	    	  	}
        		return true; 
        		
        	case REQUEST_CODE_AUDIO_PICK: 
	    	  	if (callback != null) {
	    	  		callback.onResult(helper.getActivity(), MediaItem.MEDIA_TYPE_VIDEO, data); 
	    	  		//actionTasks(this);
	    	  	}
	    	  	return true; 
        	}
    	}
		
		return false;
	}
	
	private class RequestHelper implements IntentHelper.Callback {
		
		private final IActivity mActivity;
		
		public RequestHelper(IActivity activity) { 
			mActivity = activity;
		}
		
		@Override
		public Activity getActivity() {
			return mActivity.getActivity();
		}
		
		@Override
		public void startActivityForResult(Intent intent, int requestCode) {
			getActivity().startActivityForResult(intent, requestCode);
		}

		@Override
		public void requestImageCapture(ResultCallback callback) {
			//mResultCallback = callback; 
	    	mOutputFile = null; 
	    	
	    	IntentHelper.startImageCapture(this, REQUEST_CODE_IMAGE_CAPTURE); 
		}

		@Override
		public void requestImagePick(ResultCallback callback) {
			//mResultCallback = callback; 
	    	mOutputFile = null; 
	    	
	    	IntentHelper.startImagePickFromGallery(this, REQUEST_CODE_IMAGE_PICK); 
		}

		@Override
		public void requestVideoPick(ResultCallback callback) {
			//mResultCallback = callback; 
	    	mOutputFile = null; 
	    	
	    	IntentHelper.startVideoPick(this, REQUEST_CODE_VIDEO_PICK); 
		}
		
		@Override
		public void requestAudioPick(ResultCallback callback) {
			//mResultCallback = callback; 
	    	mOutputFile = null; 
	    	
	    	IntentHelper.startAudioPick(this, REQUEST_CODE_AUDIO_PICK); 
		}

		@Override
		public void showNoImageCaptureMessage() {
			mActivity.getActivityHelper().showWarningMessage(R.string.intent_no_image_capture);
		}

		@Override
		public void showNoImagePickerMessage() {
			mActivity.getActivityHelper().showWarningMessage(R.string.intent_no_image_picker);
		}

		@Override
		public void showNoImageCroperMessage() {
			mActivity.getActivityHelper().showWarningMessage(R.string.intent_no_image_croper);
		}
		
		@Override
		public void showNoVideoPickerMessage() {
			mActivity.getActivityHelper().showWarningMessage(R.string.intent_no_video_picker);
		}
		
		@Override
		public void showNoAudioPickerMessage() {
			mActivity.getActivityHelper().showWarningMessage(R.string.intent_no_audio_picker);
		}

		@Override
		public void setResultOutputFile(File file) {
			mOutputFile = file; 
		}

		@Override
		public File getResultOutputFile() {
			return mOutputFile;
		}
	};
	
}
