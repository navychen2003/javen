package org.javenstudio.cocoka.widget.activity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.Toast;

import org.javenstudio.cocoka.widget.Constants;
import org.javenstudio.cocoka.widget.PopupMenu;
import org.javenstudio.cocoka.widget.PopupMenuListener;
import org.javenstudio.cocoka.widget.model.ActivityInitializer;
import org.javenstudio.cocoka.widget.model.ActivityListener;
import org.javenstudio.cocoka.android.ActivityHelper;
import org.javenstudio.cocoka.android.ResourceContext;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Log;

public abstract class BaseActivity extends Activity 
		implements ActivityInitializer, PopupMenuListener {

	public static final int ALERT_DIALOG = 1; 
	public static final int IMAGE_DIALOG = 2; 
	
	public class BasePopupMenuListener implements PopupMenuListener { 
		@Override 
		public void showPopupMenuAt(int id, PopupMenu menu, final View view) { 
			BaseActivity.this.showPopupMenuAt(id, menu, view); 
		}
		
		@Override 
		public PopupMenu createPopupMenu(int id, final View view) { 
			return BaseActivity.this.createPopupMenu(id, view); 
		}
		
		@Override 
		public void onPopupMenuCreated(int id, PopupMenu menu, final View view) { 
			BaseActivity.this.onPopupMenuCreated(id, menu, view); 
		}
		
		@Override 
		public void onPopupMenuShow(int id, PopupMenu menu, final View view) { 
			BaseActivity.this.onPopupMenuShow(id, menu, view); 
		}
		
		@Override 
		public void onPopupMenuDismiss(int id, PopupMenu menu, final View view) { 
			BaseActivity.this.onPopupMenuDismiss(id, menu, view); 
		}
	}
	
	private final Handler mHandler = new Handler();
	private ActivityListener mListener = null; 
	private Map<Integer, PopupMenu> mPopupMenus = null; 
	private BaseActivityGroup mActivityGroup = null; 
	private Object mDialogData = null; 
	private boolean mFinishRequested = false;
	
	public void setActivityGroup(BaseActivityGroup activityGroup) { 
		mActivityGroup = activityGroup; 
	}
	
	@Override 
	public void initializeActivityListener(ActivityListener listener) { 
		mListener = listener; 
	}
	
	public View getContentView() { 
		return getWindow().getDecorView(); 
	}
	
	public ResourceContext getResourceContext() { 
		return ResourceHelper.getResourceContext(); 
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        final ActivityListener listener = mListener; 
        if (listener != null) 
        	listener.onActivityCreate(this); 
        
        addActivity(this);
	}
	
	@Override
    public void onStart() {
        super.onStart();
        
        final ActivityListener listener = mListener; 
        if (listener != null) 
        	listener.onActivityStart(this); 
        
	}
	
	@Override
    public void onResume() {
        super.onResume();
        
        final ActivityListener listener = mListener; 
        if (listener != null) 
        	listener.onActivityResume(this); 
        
	}
	
	@Override
    public void onBackPressed() {
        final ActivityListener listener = mListener; 
        if (listener != null && listener.onActivityBackPressed(this)) 
        	return; 
        
		final BaseActivityGroup activityGroup = mActivityGroup; 
		if (activityGroup != null) { 
			activityGroup.onBackPressed(); 
			return;
		}
        
        super.onBackPressed();
	}
	
	@Override
    public void onStop() {
        final ActivityListener listener = mListener; 
        if (listener != null) 
        	listener.onActivityStop(this); 
        
        super.onStop();
	}
	
	@Override
    public void onDestroy() {
		removePopupMenus(); 
		
        final ActivityListener listener = mListener; 
        if (listener != null) 
        	listener.onActivityDestroy(this); 
        
        removeActivity(this);
        super.onDestroy();
	}
	
	public void requestFinish() { 
		if (!mFinishRequested && !isFinishing()) 
			finish();
	}
	
	@Override
	public void finish() { 
		mFinishRequested = true;
		super.finish();
	}
	
	public void finishDelay(long delayMillis) { 
		mHandler.postDelayed(new Runnable() { 
				public void run() { 
					finish();
				}
			}, delayMillis);
	}
	
	public void reload() {
		reload(getIntent()); 
	}
	
	public void reload(Intent intent) {
		if (intent == null) intent = getIntent();
		intent.setClass(getApplicationContext(), getClass()); 
		
		overridePendingTransition(0, 0);
		intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		onFinishMe();
		
		overridePendingTransition(0, 0);
		onStartMe(intent);
	}
	
	protected void onFinishMe() { 
		finish(); 
	}
	
	protected void onStartMe(Intent intent) {
		startActivity(intent); 
	}
	
	protected View createViewById(ViewGroup root, int id) { 
		return ResourceHelper.getResourceContext().inflateView(id, root); 
	}
	
	public void fullscreen(boolean enable) {
		if (enable) {
	    	// go full screen
	    	WindowManager.LayoutParams attrs = getWindow().getAttributes();
	    	attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
	    	getWindow().setAttributes(attrs);
	    	getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

    	} else {
	    	// go non-full screen
	    	WindowManager.LayoutParams attrs = getWindow().getAttributes();
	    	attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
	    	getWindow().setAttributes(attrs);
	    	getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    	}
	}
	
	public int getScreenWidth() { 
		return getResources().getDisplayMetrics().widthPixels; 
	}
	
	public int getScreenHeight() { 
		return getResources().getDisplayMetrics().heightPixels; 
	}
	
	public final void showDialog(int id, Object data) { 
		mDialogData = data; 
		showDialog(id);
	}
	
	public final Object getDialogData() { 
		return mDialogData;
	}
	
	@Override 
    protected Dialog onCreateDialog(int id) {
    	Dialog dialog = null; 
    	
    	switch (id) {
    	case ALERT_DIALOG: 
    		dialog = createAlertDialog(id, getDialogData()); 
    		break; 
    	case IMAGE_DIALOG: 
    		dialog = createImageDialog(id, getDialogData()); 
    		break; 
    	}
    	
    	if (dialog != null) 
    		return dialog; 
    	
    	return super.onCreateDialog(id); 
	}
	
	@Override
    public void onPrepareDialog(final int id, Dialog dialog) {
		switch (id) {
		case ALERT_DIALOG: {
    		onPrepareAlertDialog(dialog, getDialogData()); 
    		break; 
    	}
    	case IMAGE_DIALOG: {
    		final ImageDialog imageDialog = (ImageDialog)dialog; 
    		onPrepareImageDialog(imageDialog, getDialogData()); 
    		break; 
    	}
		}
	}
	
	protected Dialog createAlertDialog(int id, Object data) { 
		return null; 
	}
	
	protected Dialog createImageDialog(int id, Object data) { 
		return null; 
	}
	
	protected void onPrepareAlertDialog(Dialog dialog, Object data) { 
		// do nothing
	}
	
	protected void onPrepareImageDialog(ImageDialog dialog, Object data) { 
		// do nothing
	}
	
	@Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // Close the menu
        if (Intent.ACTION_MAIN.equals(intent.getAction())) {
            // also will cancel mWaitingForResult.
            closeSystemDialogs();
        }
    }
	
	protected void removeSystemDialogs() {
		removeDialog(ALERT_DIALOG); 
    	removeDialog(IMAGE_DIALOG); 
    	//removeDialog(IMAGEMENU_DIALOG); 
    }
    
    protected void closeSystemDialogs() {
        getWindow().closeAllPanels();

        try {
            dismissDialog(IMAGE_DIALOG);
            // Unlock the workspace if the dialog was showing
        } catch (Exception e) {
            // An exception is thrown if the dialog is not visible, which is fine
        }

        try {
            dismissDialog(ALERT_DIALOG);
            // Unlock the workspace if the dialog was showing
        } catch (Exception e) {
            // An exception is thrown if the dialog is not visible, which is fine
        }
    }
	
    private synchronized PopupMenu getPopupMenu(int id) { 
    	if (mPopupMenus == null) 
    		mPopupMenus = new HashMap<Integer, PopupMenu>(); 
    	return mPopupMenus.get(id); 
    }
    
    private synchronized void setPopupMenu(int id, PopupMenu menu) { 
    	PopupMenu old = getPopupMenu(id); 
    	if (old != null) { 
    		if (old == menu) return; 
    		old.dismiss(); 
    	}
    	if (mPopupMenus != null) { 
    		if (menu != null) 
    			mPopupMenus.put(id, menu); 
    		else 
    			mPopupMenus.remove(id); 
    	}
    }
    
    private synchronized void removePopupMenus() {
    	if (mPopupMenus != null) { 
    		for (int id : mPopupMenus.keySet()) { 
    			PopupMenu menu = mPopupMenus.get(id); 
    			if (menu != null && menu.isShowing()) 
    				menu.dismiss(); 
    			
    			onPopupMenuRemoved(id, menu); 
    		}
    		
    		mPopupMenus.clear(); 
    	}
    }
    
    public final void showPopupMenu(final int id, final View view) { 
    	showPopupMenu(id, view, null); 
    }
    
    public final void showPopupMenu(final int id, final View view, final PopupMenuListener listener) { 
		PopupMenu menu = getPopupMenu(id); 
		if (menu == null) { 
			final PopupMenu newmenu = listener != null ? 
					listener.createPopupMenu(id, view) : createPopupMenu(id, view); 
			if (newmenu == null) 
				return; 
			
			newmenu.setOnDismissListener(new PopupWindow.OnDismissListener() {
					@Override
					public void onDismiss() {
						if (listener != null) 
							listener.onPopupMenuDismiss(id, newmenu, view); 
						else 
							onPopupMenuDismiss(id, newmenu, view); 
					}
				});
			
			if (listener != null) 
				listener.onPopupMenuCreated(id, newmenu, view); 
			else 
				onPopupMenuCreated(id, newmenu, view); 
			
			menu = newmenu;
			setPopupMenu(id, newmenu); 
		}
		
		if (menu != null && !menu.isShowing()) {
			if (listener != null) 
				listener.onPopupMenuShow(id, menu, view); 
			else 
				onPopupMenuShow(id, menu, view); 
			
			if (listener != null) 
				listener.showPopupMenuAt(id, menu, view); 
			else 
				showPopupMenuAt(id, menu, view); 
		}
	}
	
    public AlertMenuBuilder getPopupMenuBuilder() { 
    	return null;
    }
    
    @Override
    public void showPopupMenuAt(int id, PopupMenu menu, final View view) { 
    	AlertMenuBuilder builder = getPopupMenuBuilder(); 
    	if (builder != null) { 
    		builder.showPopupMenuAt(id, menu, view); 
    		return;
    	}
    	
    	menu.showAtBottom(getContentView()); 
    }
    
    @Override
    public PopupMenu createPopupMenu(int id, final View view) { 
    	AlertMenuBuilder builder = getPopupMenuBuilder(); 
    	if (builder != null) { 
    		return builder.createPopupMenu(id, view);
    	}
    	
		return null; 
	}
	
    @Override
    public void onPopupMenuCreated(int id, PopupMenu menu, final View view) { 
    	AlertMenuBuilder builder = getPopupMenuBuilder(); 
    	if (builder != null) { 
    		builder.onPopupMenuCreated(id, menu, view);
    	}
	}
	
    @Override
    public void onPopupMenuShow(int id, PopupMenu menu, final View view) { 
    	AlertMenuBuilder builder = getPopupMenuBuilder(); 
    	if (builder != null) { 
    		builder.onPopupMenuShow(id, menu, view);
    	}
	}
	
    @Override
    public void onPopupMenuDismiss(int id, PopupMenu menu, final View view) { 
    	AlertMenuBuilder builder = getPopupMenuBuilder(); 
    	if (builder != null) { 
    		builder.onPopupMenuDismiss(id, menu, view);
    	}
	}
    
    public void onPopupMenuRemoved(int id, PopupMenu menu) { 
    	AlertMenuBuilder builder = getPopupMenuBuilder(); 
    	if (builder != null) { 
    		builder.onPopupMenuRemoved(id, menu);
    	}
	}
	
    public void showMessage(final String msg) {
    	if (msg == null || msg.length() == 0) 
    		return; 
    	
    	final Context context = getApplicationContext(); 
    	ActivityHelper.getHandler().post(new Runnable() { 
	    		public void run() { 
	    			Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
	    		}
	    	});
    }
    
    public void showMessage(final int msgid) {
    	final Context context = getApplicationContext(); 
    	ActivityHelper.getHandler().post(new Runnable() { 
	    		public void run() { 
	    			Toast.makeText(context, msgid, Toast.LENGTH_SHORT).show();
	    		}
	    	});
    }
    
    public void startActivitySafely(Intent intent) {
    	startActivitySafely(intent, null); 
    }
    
    public void startActivitySafely(Intent intent, Object tag) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Activity not found", Toast.LENGTH_SHORT).show();
            Log.e(Constants.getTag(), "Unable to launch. tag=" + tag + " intent=" + intent, e);
        } catch (SecurityException e) {
            Toast.makeText(this, "Activity not found", Toast.LENGTH_SHORT).show();
            Log.e(Constants.getTag(), "I does not have the permission to launch " + intent +
                    ". Make sure to create a MAIN intent-filter for the corresponding activity " +
                    "or use the exported attribute for this activity. "
                    + "tag="+ tag + " intent=" + intent, e);
        }
    }
    
    public void startActivityForResultSafely(Intent intent, int requestCode) {
        try {
            startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Activity not found", Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Toast.makeText(this, "Activity not found", Toast.LENGTH_SHORT).show();
            Log.e(Constants.getTag(), "I does not have the permission to launch " + intent +
                    ". Make sure to create a MAIN intent-filter for the corresponding activity " +
                    "or use the exported attribute for this activity.", e);
        }
    }
    
    private static final List<WeakReference<BaseActivity>> sActivities = 
    		new ArrayList<WeakReference<BaseActivity>>();
    
    private static void addActivity(BaseActivity activity) { 
    	if (activity == null) return;
    	
    	synchronized (sActivities) { 
    		boolean found = false;
    		
    		for (int i=0; i < sActivities.size(); ) { 
    			WeakReference<BaseActivity> ref = sActivities.get(i);
    			BaseActivity act = (ref != null) ? ref.get() : null;
    			if (act == null) { 
    				sActivities.remove(i);
    				continue;
    			}
    			if (act == activity) 
    				found = true;
    			i ++;
    		}
    		
    		if (!found) 
    			sActivities.add(new WeakReference<BaseActivity>(activity));
    	}
    }
    
    private static void removeActivity(BaseActivity activity) { 
    	if (activity == null) return;
    	
    	synchronized (sActivities) { 
    		for (int i=0; i < sActivities.size(); ) { 
    			WeakReference<BaseActivity> ref = sActivities.get(i);
    			BaseActivity act = (ref != null) ? ref.get() : null;
    			if (act == null || act == activity) { 
    				sActivities.remove(i);
    				continue;
    			}
    			i ++;
    		}
    	}
    }
    
    public static BaseActivity[] getActivities() { 
    	synchronized (sActivities) { 
    		List<BaseActivity> list = new ArrayList<BaseActivity>();
    		
    		for (int i=0; i < sActivities.size(); ) { 
    			WeakReference<BaseActivity> ref = sActivities.get(i);
    			BaseActivity act = (ref != null) ? ref.get() : null;
    			if (act == null) { 
    				sActivities.remove(i);
    				continue;
    			}
    			list.add(act);
    			i ++;
    		}
    		
    		return list.toArray(new BaseActivity[list.size()]);
    	}
    }
    
}
