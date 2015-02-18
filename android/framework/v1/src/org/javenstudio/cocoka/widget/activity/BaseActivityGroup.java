package org.javenstudio.cocoka.widget.activity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.app.ActivityGroup;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import org.javenstudio.cocoka.android.OptionsMenu;
import org.javenstudio.cocoka.android.ResourceContext;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.widget.model.ActivityInitializer;
import org.javenstudio.cocoka.widget.model.ActivityListener;

public abstract class BaseActivityGroup extends ActivityGroup 
		implements ActivityInitializer {

	private ActivityListener mListener; 
	private OptionsMenu mMenu = null; 
	private boolean mFinishRequested = false;
	
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
        
	}
	
	@Override
    public void onStart() {
        super.onStart();
        
        final ActivityListener listener = mListener; 
        if (listener != null) 
        	listener.onActivityStart(this); 
        
        addActivityGroup(this);
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
        final ActivityListener listener = mListener; 
        if (listener != null) 
        	listener.onActivityDestroy(this); 
        
        removeActivityGroup(this);
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
	
	protected void setOptionsMenu(OptionsMenu menu) { 
		mMenu = menu; 
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    super.onCreateOptionsMenu(menu);

	    OptionsMenu tabMenu = mMenu; 
	    if (tabMenu != null) { 
	    	menu.add("menu"); // must have one
	    	return true;
	    }
	    
	    return false;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
	    super.onPrepareOptionsMenu(menu);
		
	    OptionsMenu tabMenu = mMenu; 
		if (tabMenu != null) {
			if (!tabMenu.isShowing()) 
				tabMenu.showMenuAt(getContentView());
			else 
				tabMenu.hideMenu();
		}
	    
	    return false; 
	}
	
	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		return false; // not display system menu
	}
	

    private static final List<WeakReference<BaseActivityGroup>> sActivityGroups = 
    		new ArrayList<WeakReference<BaseActivityGroup>>();
    
    private static void addActivityGroup(BaseActivityGroup activity) { 
    	if (activity == null) return;
    	
    	synchronized (sActivityGroups) { 
    		boolean found = false;
    		
    		for (int i=0; i < sActivityGroups.size(); ) { 
    			WeakReference<BaseActivityGroup> ref = sActivityGroups.get(i);
    			BaseActivityGroup act = (ref != null) ? ref.get() : null;
    			if (act == null) { 
    				sActivityGroups.remove(i);
    				continue;
    			}
    			if (act == activity) 
    				found = true;
    			i ++;
    		}
    		
    		if (!found) 
    			sActivityGroups.add(new WeakReference<BaseActivityGroup>(activity));
    	}
    }
    
    private static void removeActivityGroup(BaseActivityGroup activity) { 
    	if (activity == null) return;
    	
    	synchronized (sActivityGroups) { 
    		for (int i=0; i < sActivityGroups.size(); ) { 
    			WeakReference<BaseActivityGroup> ref = sActivityGroups.get(i);
    			BaseActivityGroup act = (ref != null) ? ref.get() : null;
    			if (act == null || act == activity) { 
    				sActivityGroups.remove(i);
    				continue;
    			}
    			i ++;
    		}
    	}
    }
    
    public static BaseActivityGroup[] getActivityGroups() { 
    	synchronized (sActivityGroups) { 
    		List<BaseActivityGroup> list = new ArrayList<BaseActivityGroup>();
    		
    		for (int i=0; i < sActivityGroups.size(); ) { 
    			WeakReference<BaseActivityGroup> ref = sActivityGroups.get(i);
    			BaseActivityGroup act = (ref != null) ? ref.get() : null;
    			if (act == null) { 
    				sActivityGroups.remove(i);
    				continue;
    			}
    			list.add(act);
    			i ++;
    		}
    		
    		return list.toArray(new BaseActivityGroup[list.size()]);
    	}
    }
	
}
