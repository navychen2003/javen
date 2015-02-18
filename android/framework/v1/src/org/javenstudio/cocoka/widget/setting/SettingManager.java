package org.javenstudio.cocoka.widget.setting;

import java.util.ArrayList;
import java.util.List;

import org.javenstudio.cocoka.android.ResourceContext;
import org.javenstudio.cocoka.android.ResourceHelper;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

/**
 * Used to help create {@link Preference} hierarchies
 * from activities or XML.
 * <p>
 * In most cases, clients should use
 * {@link PreferenceActivity#addPreferencesFromIntent} or
 * {@link PreferenceActivity#addPreferencesFromResource(int)}.
 * 
 * @see PreferenceActivity
 */
public class SettingManager {

    /**
     * The Activity meta-data key for its XML preference hierarchy.
     */
    public static final String METADATA_KEY_PREFERENCES = "cocoka.preference";
    
    public static final String KEY_HAS_SET_DEFAULT_VALUES = "_has_set_default_values";

    /**
     * The starting request code given out to preference framework.
     */
    public static final int FIRST_REQUEST_CODE = 100;
    
    /**
     * Interface definition for a class that will be called when the container's activity
     * receives an activity result.
     */
    public interface OnActivityResultListener {
        
        /**
         * See Activity's onActivityResult.
         * 
         * @return Whether the request code was handled (in which case
         *         subsequent listeners will not be called.
         */
        boolean onActivityResult(int requestCode, int resultCode, Intent data);
    }
    
    /**
     * Interface definition for a class that will be called when the container's activity
     * is stopped.
     */
    public interface OnActivityStopListener {
        
        /**
         * See Activity's onStop.
         */
        void onActivityStop();
    }

    /**
     * Interface definition for a class that will be called when the container's activity
     * is destroyed.
     */
    public interface OnActivityDestroyListener {
        
        /**
         * See Activity's onDestroy.
         */
        void onActivityDestroy();
    }
    
    /**
     * The context to use. This should always be set.
     * 
     * @see #mActivity
     */
    private final Context mContext;
    
    /**
     * The counter for unique IDs.
     */
    private long mNextId = 0;

    /**
     * The counter for unique request codes.
     */
    private int mNextRequestCode;
    
    /**
     * Cached shared settings.
     */
    private SharedPreferences mSharedPreferences;
    
    /**
     * The SharedPreferences name that will be used for all {@link Setting}s
     * managed by this instance.
     */
    private String mSharedPreferencesName;
    
    /**
     * The SharedPreferences mode that will be used for all {@link Setting}s
     * managed by this instance.
     */
    private int mSharedPreferencesMode;
    
    /**
     * If in no-commit mode, the shared editor to give out (which will be
     * committed when exiting no-commit mode).
     */
    private SharedPreferences.Editor mEditor;
    
    /**
     * Blocks commits from happening on the shared editor. This is used when
     * inflating the hierarchy. Do not set this directly, use {@link #setNoCommit(boolean)}
     */
    private boolean mNoCommit;
    
    /**
     * The {@link SettingScreen} at the root of the setting hierarchy.
     */
    private SettingScreen mSettingScreen = null;

    /**
     * List of activity result listeners.
     */
    private List<OnActivityResultListener> mActivityResultListeners;

    /**
     * List of activity stop listeners.
     */
    private List<OnActivityStopListener> mActivityStopListeners;

    /**
     * List of activity destroy listeners.
     */
    private List<OnActivityDestroyListener> mActivityDestroyListeners;

    /**
     * List of dialogs that should be dismissed when we receive onNewIntent in
     * our SettingActivity.
     */
    private List<DialogInterface> mSettingsScreens;
    
    /**
     * The constructor.
     */
	public SettingManager(Context context, int firstRequestCode) { 
		this(context, getDefaultSharedPreferencesName(context), firstRequestCode);
	}
	
	/**
     * This constructor should ONLY be used when getting default values from
     * an XML preference hierarchy.
     * <p>
     * The {@link PreferenceManager#PreferenceManager(Activity)}
     * should be used ANY time a preference will be displayed, since some preference
     * types need an Activity for managed queries.
     */
	public SettingManager(Context context) {
        this(context, FIRST_REQUEST_CODE);
    }

	public SettingManager(Context context, String preferencesName) {
		this(context, preferencesName, FIRST_REQUEST_CODE);
	}
	
    public SettingManager(Context context, String preferencesName, int firstRequestCode) {
        mContext = context;
        mNextRequestCode = firstRequestCode;
        
        setSharedPreferencesName(preferencesName);
    }
	
    /**
     * Returns the context. This is preferred over {@link #getActivity()} when
     * possible.
     * 
     * @return The context.
     */
    public final Context getContext() {
        return mContext;
    }
	
    public final ResourceContext getResourceContext() { 
    	return ResourceHelper.getResourceContext(); 
    }
    
	/**
     * Returns a list of {@link Activity} (indirectly) that match a given
     * {@link Intent}.
     * 
     * @param queryIntent The Intent to match.
     * @return The list of {@link ResolveInfo} that point to the matched
     *         activities.
     */
    public List<ResolveInfo> queryIntentActivities(Intent queryIntent) {
        return mContext.getPackageManager().queryIntentActivities(queryIntent,
                PackageManager.GET_META_DATA);
    }
	
    /**
     * Inflates a preference hierarchy from XML. If a preference hierarchy is
     * given, the new preference hierarchies will be merged in.
     * 
     * @param context The context of the resource.
     * @param resId The resource ID of the XML to inflate.
     * @param rootPreferences Optional existing hierarchy to merge the new
     *            hierarchies into.
     * @return The root hierarchy (if one was not provided, the new hierarchy's
     *         root).
     * @hide
     */
	public SettingScreen inflateFromResource(int resId, SettingScreen rootSettings) {
		// Block commits
        setNoCommit(true);

        final SettingInflater inflater = new SettingInflater(this);
        rootSettings = (SettingScreen) inflater.inflate(resId, rootSettings, true);
        rootSettings.onAttachedToHierarchy();

        // Unblock commits
        setNoCommit(false);

        return rootSettings;
	}
	
	public SettingScreen createSettingScreen(String key) {
        final SettingScreen settingScreen = new SettingScreen(this, null);
        settingScreen.setKey(key); 
        //settingScreen.onAttachedToHierarchy();
        return settingScreen;
    }
    
    protected long getNextId() {
        synchronized (this) {
            return mNextId++;
        }
    }
    
    /**
     * Returns the root of the setting hierarchy managed by this class.
     *  
     * @return The {@link SettingScreen} object that is at the root of the hierarchy.
     */
    public SettingScreen getSettingScreen() {
        return mSettingScreen;
    }
    
    /**
     * Sets the root of the setting hierarchy.
     * 
     * @param settingScreen The root {@link SettingScreen} of the setting hierarchy.
     * @return Whether the {@link SettingScreen} given is different than the previous. 
     */
    public boolean setSettings(SettingScreen settingScreen) {
        if (settingScreen != mSettingScreen) {
            mSettingScreen = settingScreen;
            return true;
        }
        
        return false;
    }
    
    public void addSettingsScreen(DialogInterface screen) {
        synchronized (this) {
            if (mSettingsScreens == null) 
                mSettingsScreens = new ArrayList<DialogInterface>();
            
            mSettingsScreens.add(screen);
        }
    }
    
    public void removeSettingsScreen(DialogInterface screen) {
        synchronized (this) {
            if (mSettingsScreens == null) 
                return;
            
            mSettingsScreens.remove(screen);
        }
    }
	
    /**
     * Finds a {@link Setting} based on its key.
     * 
     * @param key The key of the setting to retrieve.
     * @return The {@link Setting} with the key, or null.
     * @see SettingGroup#findSetting(CharSequence)
     */
	public Setting findSetting(String key) {
        if (mSettingScreen == null) 
            return null;
        
        return mSettingScreen.findSetting(key);
    }
	
    /**
     * Returns the current name of the SharedPreferences file that settings managed by
     * this will use.
     * 
     * @return The name that can be passed to {@link Context#getSharedPreferences(String, int)}.
     * @see Context#getSharedPreferences(String, int)
     */
    public String getSharedPreferencesName() {
        return mSharedPreferencesName;
    }

    /**
     * Sets the name of the SharedPreferences file that settings managed by this
     * will use.
     * 
     * @param sharedSettingsName The name of the SharedPreferences file.
     * @see Context#getSharedPreferences(String, int)
     */
    public void setSharedPreferencesName(String sharedSettingsName) {
        mSharedPreferencesName = sharedSettingsName;
        mSharedPreferences = null;
    }
	
    /**
     * Returns the current mode of the SharedPreferences file that settings managed by
     * this will use.
     * 
     * @return The mode that can be passed to {@link Context#getSharedPreferences(String, int)}.
     * @see Context#getSharedPreferences(String, int)
     */
    public int getSharedPreferencesMode() {
        return mSharedPreferencesMode;
    }

    /**
     * Sets the mode of the SharedPreferences file that settings managed by this
     * will use.
     * 
     * @param sharedSettingsMode The mode of the SharedPreferences file.
     * @see Context#getSharedPreferences(String, int)
     */
    public void setSharedPreferencesMode(int sharedSettingsMode) {
        mSharedPreferencesMode = sharedSettingsMode;
        mSharedPreferences = null;
    }
    
    /**
     * Gets a SharedPreferences instance that settings managed by this will
     * use.
     * 
     * @return A SharedPreferences instance pointing to the file that contains
     *         the values of settings that are managed by this.
     */
	public SharedPreferences getSharedPreferences() {
        if (mSharedPreferences == null) {
            mSharedPreferences = mContext.getSharedPreferences(mSharedPreferencesName,
                    mSharedPreferencesMode);
        }
        
        return mSharedPreferences;
    }
	
    /**
     * Gets a SharedPreferences instance that points to the default file that is
     * used by the setting framework in the given context.
     * 
     * @param context The context of the settings whose values are wanted.
     * @return A SharedPreferences instance that can be used to retrieve and
     *         listen to values of the settings.
     */
    public static SharedPreferences getDefaultSharedPreferences(Context context) {
        return context.getSharedPreferences(getDefaultSharedPreferencesName(context),
                getDefaultSharedPreferencesMode());
    }
	
    protected static String getDefaultSharedPreferencesName(Context context) {
        return context.getPackageName() + "_settings";
    }

    protected static int getDefaultSharedPreferencesMode() {
        return Context.MODE_PRIVATE;
    }
    
    /**
     * Sets the default values from a setting hierarchy in XML. This should
     * be called by the application's main activity.
     * <p>
     * If {@code readAgain} is false, this will only set the default values if this
     * method has never been called in the past (or the
     * {@link #KEY_HAS_SET_DEFAULT_VALUES} in the default value shared
     * settings file is false). To attempt to set the default values again
     * bypassing this check, set {@code readAgain} to true.
     * 
     * @param context The context of the shared settings.
     * @param resId The resource ID of the setting hierarchy XML file.
     * @param readAgain Whether to re-read the default values.
     *            <p>
     *            Note: this will NOT reset settings back to their default
     *            values. For that functionality, use
     *            {@link SettingManager#getDefaultSharedPreferences(Context)}
     *            and clear it followed by a call to this method with this
     *            parameter set to true.
     */
    public static void setDefaultValues(Context context, int resId, boolean readAgain) {
        // Use the default shared settings name and mode
        setDefaultValues(context, getDefaultSharedPreferencesName(context),
                getDefaultSharedPreferencesMode(), resId, readAgain);
    }
    
    /**
     * Similar to {@link #setDefaultValues(Context, int, boolean)} but allows
     * the client to provide the filename and mode of the shared settings
     * file.
     * 
     * @see #setDefaultValues(Context, int, boolean)
     * @see #setSharedPreferencesName(String)
     * @see #setSharedPreferencesMode(int)
     */
    public static void setDefaultValues(Context context, String sharedSettingsName,
            int sharedSettingsMode, int resId, boolean readAgain) {
        final SharedPreferences defaultValueSp = context.getSharedPreferences(
                KEY_HAS_SET_DEFAULT_VALUES, Context.MODE_PRIVATE);
        
        if (readAgain || !defaultValueSp.getBoolean(KEY_HAS_SET_DEFAULT_VALUES, false)) {
            final SettingManager pm = new SettingManager(context);
            pm.setSharedPreferencesName(sharedSettingsName);
            pm.setSharedPreferencesMode(sharedSettingsMode);
            pm.inflateFromResource(resId, null);

            defaultValueSp.edit().putBoolean(KEY_HAS_SET_DEFAULT_VALUES, true).commit();
        }
    }
    
    /**
     * Returns an editor to use when modifying the shared settings.
     * <p>
     * Do NOT commit unless {@link #shouldCommit()} returns true.
     * 
     * @return An editor to use to write to shared settings.
     * @see #shouldCommit()
     */
	public SharedPreferences.Editor getEditor() {
        if (mNoCommit) {
            if (mEditor == null) 
                mEditor = getSharedPreferences().edit();
            
            return mEditor;
        } else {
            return getSharedPreferences().edit();
        }
    }
	
    /**
     * Whether it is the client's responsibility to commit on the
     * {@link #getEditor()}. This will return false in cases where the writes
     * should be batched, for example when inflating settings from XML.
     * 
     * @return Whether the client should commit.
     */
    protected boolean shouldCommit() {
        return !mNoCommit;
    }
    
    protected void setNoCommit(boolean noCommit) {
        if (!noCommit && mEditor != null) {
            mEditor.commit();
        }
        
        mNoCommit = noCommit;
    }
	
    /**
     * Registers a listener.
     * 
     * @see OnActivityResultListener
     */
    public void registerOnActivityResultListener(OnActivityResultListener listener) {
        synchronized (this) {
            if (mActivityResultListeners == null) {
                mActivityResultListeners = new ArrayList<OnActivityResultListener>();
            }
            
            if (!mActivityResultListeners.contains(listener)) {
                mActivityResultListeners.add(listener);
            }
        }
    }

    /**
     * Unregisters a listener.
     * 
     * @see OnActivityResultListener
     */
    public void unregisterOnActivityResultListener(OnActivityResultListener listener) {
        synchronized (this) {
            if (mActivityResultListeners != null) {
                mActivityResultListeners.remove(listener);
            }
        }
    }
    
    /**
     * Called by the {@link SettingManager} to dispatch a subactivity result.
     */
    public void dispatchActivityResult(int requestCode, int resultCode, Intent data) {
        List<OnActivityResultListener> list;
        
        synchronized (this) {
            if (mActivityResultListeners == null) return;
            list = new ArrayList<OnActivityResultListener>(mActivityResultListeners);
        }

        final int N = list.size();
        for (int i = 0; i < N; i++) {
            if (list.get(i).onActivityResult(requestCode, resultCode, data)) {
                break;
            }
        }
    }

    /**
     * Registers a listener.
     * 
     * @see OnActivityStopListener
     */
    public void registerOnActivityStopListener(OnActivityStopListener listener) {
        synchronized (this) {
            if (mActivityStopListeners == null) {
                mActivityStopListeners = new ArrayList<OnActivityStopListener>();
            }
            
            if (!mActivityStopListeners.contains(listener)) {
                mActivityStopListeners.add(listener);
            }
        }
    }
    
    /**
     * Unregisters a listener.
     * 
     * @see OnActivityStopListener
     */
    public void unregisterOnActivityStopListener(OnActivityStopListener listener) {
        synchronized (this) {
            if (mActivityStopListeners != null) {
                mActivityStopListeners.remove(listener);
            }
        }
    }
    
    /**
     * Called by the {@link SettingManager} to dispatch the activity stop
     * event.
     */
    public void dispatchActivityStop() {
        List<OnActivityStopListener> list;
        
        synchronized (this) {
            if (mActivityStopListeners == null) return;
            list = new ArrayList<OnActivityStopListener>(mActivityStopListeners);
        }

        final int N = list.size();
        for (int i = 0; i < N; i++) {
            list.get(i).onActivityStop();
        }
    }

    /**
     * Registers a listener.
     * 
     * @see OnActivityDestroyListener
     */
    public void registerOnActivityDestroyListener(OnActivityDestroyListener listener) {
        synchronized (this) {
            if (mActivityDestroyListeners == null) {
                mActivityDestroyListeners = new ArrayList<OnActivityDestroyListener>();
            }

            if (!mActivityDestroyListeners.contains(listener)) {
                mActivityDestroyListeners.add(listener);
            }
        }
    }
    
    /**
     * Unregisters a listener.
     * 
     * @see OnActivityDestroyListener
     */
    public void unregisterOnActivityDestroyListener(OnActivityDestroyListener listener) {
        synchronized (this) {
            if (mActivityDestroyListeners != null) {
                mActivityDestroyListeners.remove(listener);
            }
        }
    }
    
    /**
     * Called by the {@link SettingManager} to dispatch the activity destroy
     * event.
     */
    public void dispatchActivityDestroy() {
        List<OnActivityDestroyListener> list = null;
        
        synchronized (this) {
            if (mActivityDestroyListeners != null) {
                list = new ArrayList<OnActivityDestroyListener>(mActivityDestroyListeners);
            }
        }

        if (list != null) {
            final int N = list.size();
            for (int i = 0; i < N; i++) {
                list.get(i).onActivityDestroy();
            }
        }

        // Dismiss any SettingScreens still showing
        dismissAllScreens();
    }
    
    /**
     * Returns a request code that is unique for the activity. Each subsequent
     * call to this method should return another unique request code.
     * 
     * @return A unique request code that will never be used by anyone other
     *         than the caller of this method.
     */
    protected int getNextRequestCode() {
        synchronized (this) {
            return mNextRequestCode++;
        }
    }
    
    /**
     * Called by {@link SettingActivity} to dispatch the new Intent event.
     * 
     * @param intent The new Intent.
     */
    public void dispatchNewIntent(Intent intent) {
        dismissAllScreens();
    }

    protected void dismissAllScreens() {
        // Remove any of the previously shown settings screens
        ArrayList<DialogInterface> screensToDismiss;

        synchronized (this) {
            
            if (mSettingsScreens == null) {
                return;
            }
            
            screensToDismiss = new ArrayList<DialogInterface>(mSettingsScreens);
            mSettingsScreens.clear();
        }
        
        for (int i = screensToDismiss.size() - 1; i >= 0; i--) {
            screensToDismiss.get(i).dismiss();
        }
    }
    
}
