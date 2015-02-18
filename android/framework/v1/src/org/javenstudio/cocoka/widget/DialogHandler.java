package org.javenstudio.cocoka.widget;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;

import org.javenstudio.cocoka.widget.Constants;
import org.javenstudio.common.util.Log;

@SuppressWarnings({"unused"})
public class DialogHandler implements DialogInterface.OnClickListener,
		DialogInterface.OnCancelListener, DialogInterface.OnDismissListener  {
	
	public static final int ACTION_CANCEL = 1; 
	public static final int ACTION_DISMISS = 2; 
	public static final int ACTION_SHOW = 3; 

	public static interface Callback {
		public void onClick(int which); 
		public void onDialogAction(int action, DialogInterface dialog); 
	}
	
	private final Activity mActivity; 
	private final Dialog mDialog; 
	private final int mDialogId; 
	private Callback mCallback = null; 
	
	public DialogHandler(Activity activity, Dialog dialog, int dialogId) {
		mActivity = activity; 
		mDialog = dialog; 
		mDialogId = dialogId; 
		
		dialog.setOnCancelListener(this);
        dialog.setOnDismissListener(this);
        //dialog.setOnShowListener(this);
	}
	
	public void setCallback(Callback callback) {
		mCallback = callback; 
	}
	
	public void onCancel(DialogInterface dialog) {
		if (mCallback != null) 
			mCallback.onDialogAction(ACTION_CANCEL, dialog); 
		
        cleanup();
    }

    public void onDismiss(DialogInterface dialog) {
    	if (mCallback != null) 
			mCallback.onDialogAction(ACTION_DISMISS, dialog); 
    }

    private void cleanup() {
        try {
        	mActivity.dismissDialog(mDialogId);
        	mActivity.removeDialog(mDialogId);
        } catch (Exception e) {
            // An exception is thrown if the dialog is not visible, which is fine
        	Log.w(Constants.getTag(), "cleanup dialog error", e); 
        }
    }
    
    public void onShow(DialogInterface dialog) {
    	if (mCallback != null) 
			mCallback.onDialogAction(ACTION_SHOW, dialog); 
    }
    
    /**
     * Handle the action clicked in the "Add to home" dialog.
     */
    public void onClick(DialogInterface dialog, int which) {
        cleanup();

        Callback callback = mCallback; 
        if (callback != null) 
        	callback.onClick(which); 
    }
}
