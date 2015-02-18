package org.javenstudio.provider.library.select;

import android.app.Activity;
import android.content.Intent;

import org.javenstudio.android.app.ActivityHelper;

public interface ISelectCallback extends ActivityHelper.ActivityResultListener {

	public static final int REQUEST_CODE_UPLOAD = 3001;
	public static final int REQUEST_CODE_DOWNLOAD = 3002;
	public static final int REQUEST_CODE_IMAGE_PICK = 3003;
	public static final int REQUEST_CODE_IMAGE_CAPTURE = 3004;
	
	public CharSequence getSelectTitle();
	public ISelectData[] getRootList(SelectOperation op);
	
	public boolean isSelected(SelectListItem item);
	public boolean onItemSelect(Activity activity, SelectListItem item);
	public boolean onActionSelect(Activity activity, SelectOperation op);
	
	public int getRequestCode();
	public boolean onActivityResult(ActivityHelper helper, int requestCode, 
			int resultCode, Intent data);
	
}
