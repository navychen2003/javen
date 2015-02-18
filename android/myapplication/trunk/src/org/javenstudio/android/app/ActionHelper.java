package org.javenstudio.android.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.app.IActionBar;
import org.javenstudio.cocoka.app.BaseActionHelper;
import org.javenstudio.cocoka.app.IActionMode;

public class ActionHelper extends BaseActionHelper {
	//private static final Logger LOG = Logger.getLogger(ActionHelper.class);
    
	public static interface HelperApp {
		public ActionHelper getActionHelper();
	}
	
	private final Activity mActivity;
	private final ActionExecutor mExecutor;
	private SelectMode mSelectHandler = null;
	
	public ActionHelper(Activity activity, IActionBar actionBar) { 
		super(actionBar);
		mActivity = activity;
		mExecutor = createActionExecutor();
	}
	
	public final Activity getActivity() { 
		return mActivity; 
	}
	
	public IActivity getIActivity() { 
		Activity activity = getActivity();
		if (activity != null && activity instanceof IActivity) 
			return (IActivity)activity;
		
		return null;
	}
	
	public final ActionExecutor getActionExecutor() {
		return mExecutor;
	}
	
	protected ActionExecutor createActionExecutor() {
		return new ActionExecutor(this);
	}
	
	//protected SelectMode createSelectMode() {
	//	return new SelectMode(this);
	//}
	
	public IActionMode startActionMode(IActionMode.Callback callback) { 
		return getActionBar().startActionMode(callback);
	}
	
	public boolean isActionMode() { 
		return isSelectMode();
	}
	
	//public synchronized SelectMode getSelectMode() { 
	//	if (mSelectHandler == null) mSelectHandler = createSelectMode();
	//	return mSelectHandler;
	//}
	
	public synchronized boolean startSelectMode(SelectMode mode, 
			SelectMode.Callback callback) { 
		if (mode != null) {
			mSelectHandler = mode;
			return mode.startActionMode(callback);
		}
		return false;
	}
	
	public synchronized boolean isSelectMode() { 
		SelectMode mode = mSelectHandler;
		if (mode != null) return mode.isActionMode();
		return false;
	}
	
	public synchronized void finishSelectMode() { 
		SelectMode mode = mSelectHandler;
		if (mode != null) mode.finishActionMode();
		mSelectHandler = null;
	}
    
	public synchronized void onActionModeDestroy() { 
		mSelectHandler = null;
	}
	
    public void postHideProgressDialog() { 
    	ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					hideProgressDialog();
				}
	    	});
    }
    
    public void postShowProgressDialog(final CharSequence message) { 
    	if (message == null) return;
    	
    	ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					showProgressDialog(message);
				}
	    	});
    }
    
    private ProgressDialog mActionProgressDialog = null;
    
    public synchronized void hideProgressDialog() { 
    	if (mActionProgressDialog != null) { 
        	mActionProgressDialog.dismiss();
        	mActionProgressDialog = null;
        }
    }
    
	public synchronized void showProgressDialog(CharSequence message) { 
		if (mActionProgressDialog == null) { 
			mActionProgressDialog = createProgressDialog(getActivity(), message, 0);
			mActionProgressDialog.show();
		} else { 
			mActionProgressDialog.setMessage(message);
		}
	}
    
	protected ProgressDialog createProgressDialog(Context context, 
			CharSequence message, int progressMax) {
		if (progressMax < 0) progressMax = 0;
		
        ProgressDialog dialog = new ProgressDialog(context);
        //dialog.setTitle(titleId);
        dialog.setMessage(message);
        dialog.setMax(progressMax);
        dialog.setCancelable(false);
        dialog.setIndeterminate(false);
        
        if (progressMax > 1) 
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        
        return dialog;
    }
	
}
