package org.javenstudio.cocoka.widget.setting;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ListView;

import org.javenstudio.cocoka.widget.activity.BaseActivity;

public abstract class SettingActivity extends BaseActivity implements SettingScreen.LayoutBinder {

	private static final String SETTINGS_TAG = "cocoka:settings";
	
	private Bundle mSavedInstanceState;
	
	private SettingManager mSettingManager = null;
	
	private static final int MSG_BIND_SETTINGS = 0;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_BIND_SETTINGS:
                    bindSettings();
                    break;
            }
        }
    };
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSettingManager = getSettingManager();
    }
	
	@Override
    public void onResume() {
        super.onResume();
        
        postBindSettings();
	}
	
    @Override
    public void onStop() {
        super.onStop();
        
        mSettingManager.dispatchActivityStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        mSettingManager.dispatchActivityDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        final SettingScreen settingScreen = getSettingScreen();
        if (settingScreen != null) {
            Bundle container = new Bundle();
            settingScreen.saveHierarchyState(container);
            outState.putBundle(SETTINGS_TAG, container);
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        Bundle container = state.getBundle(SETTINGS_TAG);
        if (container != null) {
            final SettingScreen settingScreen = getSettingScreen();
            if (settingScreen != null) {
                settingScreen.restoreHierarchyState(container);
                mSavedInstanceState = state;
                return;
            }
        }

        // Only call this if we didn't save the instance state for later.
        // If we did save it, it will be restored when we bind the adapter.
        super.onRestoreInstanceState(state);
    }
	
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        mSettingManager.dispatchActivityResult(requestCode, resultCode, data);
    }
	
	@Override
    public void onContentChanged() {
        super.onContentChanged();
        postBindSettings();
    }
	
	public abstract Dialog createSettingScreenDialog(SettingScreen screen); 
	public abstract View inflateSettingScreenView(SettingScreen screen); 
	public abstract ListView getSettingListView(View view); 
	
	public abstract int getSettingItemResource(Setting setting); 
	public abstract void bindSettingItemView(Setting setting, View view); 
	
	protected String getSettingsTag() { 
		return SETTINGS_TAG; 
	}
	
	protected SettingManager createSettingManager() {
		return new SettingManager(getApplicationContext());
	}
	
	public final synchronized SettingManager getSettingManager() { 
		if (mSettingManager == null) { 
			SettingManager settingManager = createSettingManager();
			mSettingManager = settingManager; 
		}
		return mSettingManager; 
	}
	
	private void requireSettingManager() {
        if (mSettingManager == null) {
            throw new RuntimeException("This should be called after super.onCreate.");
        }
    }
	
    /**
     * Inflates the given XML resource and adds the preference hierarchy to the current
     * preference hierarchy.
     * 
     * @param settingResId The XML resource ID to inflate.
     */
	public void addSettingFromResource(int settingResId) {
		requireSettingManager();
        
        setRootSettingScreen(mSettingManager.inflateFromResource(settingResId,
                getSettingScreen()));
    }
	
    /**
     * Gets the root of the preference hierarchy that this activity is showing.
     * 
     * @return The {@link PreferenceScreen} that is the root of the preference
     *         hierarchy.
     */
	public SettingScreen getSettingScreen() {
		requireSettingManager();
		
        return mSettingManager.getSettingScreen();
    }
	
    /**
     * Sets the root of the preference hierarchy that this activity is showing.
     * 
     * @param preferenceScreen The root {@link PreferenceScreen} of the preference hierarchy.
     */
	public void setRootSettingScreen(SettingScreen settingScreen) {
		requireSettingManager();
		
		if (mSettingManager.setSettings(settingScreen) && settingScreen != null) {
            postBindSettings();
		}
	}
	
	public final SettingScreen getRootSettingScreen() { 
		requireSettingManager();
		
        return mSettingManager.getSettingScreen();
	}
	
	protected void postBindSettings() {
        if (mHandler.hasMessages(MSG_BIND_SETTINGS)) return;
        mHandler.obtainMessage(MSG_BIND_SETTINGS).sendToTarget();
    }
    
    protected final void bindSettings() {
        final SettingScreen settingScreen = getSettingScreen();
        if (settingScreen != null) {
        	settingScreen.setLayoutBinder(this);
        	settingScreen.init(this);
        	
        	ListView listView = getSettingListView(null);
        	
        	SettingScreen.OnBindListener listener = settingScreen.getOnBindListener(); 
        	if (listener != null)
        		listener.onBindScreenViews(settingScreen, listView); 
        	
            settingScreen.bind(listView);
            if (mSavedInstanceState != null) {
                super.onRestoreInstanceState(mSavedInstanceState);
                mSavedInstanceState = null;
            }
        }
    }
	
    @Override    
    protected Dialog onCreateDialog(int id) {
    	final SettingScreen settingScreen = getSettingScreen();
        if (settingScreen != null) {
        	Dialog dialog = settingScreen.onCreateActivityDialog(id); 
        	if (dialog != null) 
        		return dialog;
        }
        
        return super.onCreateDialog(id);
    }
    
    @Override
    public void onPrepareDialog(final int id, Dialog dialog) {
    	final SettingScreen settingScreen = getSettingScreen();
        if (settingScreen != null) {
        	if (settingScreen.onPrepareActivityDialog(id, dialog)) 
        		return;
        }
        
    	super.onPrepareDialog(id, dialog);
    }
    
}
