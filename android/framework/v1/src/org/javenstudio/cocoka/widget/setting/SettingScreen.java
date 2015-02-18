package org.javenstudio.cocoka.widget.setting;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;

import org.javenstudio.cocoka.widget.model.ActivityListener;
import org.javenstudio.cocoka.widget.activity.BaseActivity;
import org.javenstudio.cocoka.widget.activity.AlertMenuBuilder;

/**
 * Represents a top-level {@link Preference} that
 * is the root of a Preference hierarchy. A {@link PreferenceActivity}
 * points to an instance of this class to show the preferences. To instantiate
 * this class, use {@link PreferenceManager#createPreferenceScreen(Context)}.
 * <ul>
 * This class can appear in two places:
 * <li> When a {@link PreferenceActivity} points to this, it is used as the root
 * and is not shown (only the contained preferences are shown).
 * <li> When it appears inside another preference hierarchy, it is shown and
 * serves as the gateway to another screen of preferences (either by showing
 * another screen of preferences as a {@link Dialog} or via a
 * {@link Context#startActivity(android.content.Intent)} from the
 * {@link Preference#getIntent()}). The children of this {@link PreferenceScreen}
 * are NOT shown in the screen that this {@link PreferenceScreen} is shown in.
 * Instead, a separate screen will be shown when this preference is clicked.
 * </ul>
 * <p>Here's an example XML layout of a PreferenceScreen:</p>
 * <pre>
	&lt;PreferenceScreen
	        xmlns:android="http://schemas.android.com/apk/res/android"
	        android:key="first_preferencescreen"&gt;
	    &lt;CheckBoxPreference
	            android:key="wifi enabled"
	            android:title="WiFi" /&gt;
	    &lt;PreferenceScreen
	            android:key="second_preferencescreen"
	            android:title="WiFi settings"&gt;
	        &lt;CheckBoxPreference
	                android:key="prefer wifi"
	                android:title="Prefer WiFi" /&gt;
	        ... other preferences here ...
	    &lt;/PreferenceScreen&gt;
	&lt;/PreferenceScreen&gt; </pre>
 * <p>
 * In this example, the "first_preferencescreen" will be used as the root of the
 * hierarchy and given to a {@link PreferenceActivity}. The first screen will
 * show preferences "WiFi" (which can be used to quickly enable/disable WiFi)
 * and "WiFi settings". The "WiFi settings" is the "second_preferencescreen" and when
 * clicked will show another screen of preferences such as "Prefer WiFi" (and
 * the other preferences that are children of the "second_preferencescreen" tag).
 * 
 * @see PreferenceCategory
 */
public class SettingScreen extends SettingGroup 
		implements AdapterView.OnItemClickListener, DialogInterface.OnDismissListener {

	public static interface OnBindListener { 
		public void onBindScreenViews(SettingScreen screen, View view); 
		public void onBindScreenAction(SettingScreen screen, View actionView); 
	}
	
    public interface LayoutBinder { 
    	public Dialog createSettingScreenDialog(SettingScreen screen); 
    	public View inflateSettingScreenView(SettingScreen screen);
    	public ListView getSettingListView(View view); 
    	
    	public int getSettingItemResource(Setting setting); 
    	public void bindSettingItemView(Setting setting, View view); 
    }
	
	private OnSettingTreeClickListener mOnSettingTreeClickListener = null; 
	private OnSettingIntentClickListener mOnSettingIntentClickListener = null; 
	private OnBindListener mOnBindListener = null; 
	
	private ListAdapter mAdapter = null;
	private Dialog mDialog = null;
	private Activity mActivity = null;
	private ActivityListener mActivityListener = null;
	private LayoutBinder mLayoutBinder = null; 
    
    public SettingScreen(SettingManager manager) {
    	this(manager, null); 
    }
	
    /**
     * Do NOT use this constructor, use {@link SettingManager#createSettingScreen(Context)}.
     * @hide-
     */
    public SettingScreen(SettingManager manager, AttributeSet attrs) {
        super(manager, attrs); 
    }

    public void init(Activity activity) { 
    	mActivity = activity;
    	
    	if (activity != null && activity instanceof BaseActivity) { 
    		BaseActivity activityBase = (BaseActivity)activity; 
    		activityBase.initializeActivityListener(getActivityListener());
    	}
    	
    	onActivityInitialized(activity);
    }
    
    protected void onActivityInitialized(Activity activity) { 
    	// do nothing
    }
    
    public void setActivityListener(ActivityListener listener) { 
    	mActivityListener = listener;
    }
    
    public ActivityListener getActivityListener() { 
    	return mActivityListener;
    }
    
    /**
     * Returns the activity that shows the settings. This is useful for doing
     * managed queries, but in most cases the use of {@link #getContext()} is
     * preferred.
     * <p>
     * This will return null if this class was instantiated with a Context
     * instead of Activity. For example, when setting the default values.
     * 
     * @return The activity that shows the settings.
     * @see #mContext
     */
    public final Activity getActivity() {
        return mActivity;
    }
    
    public AlertMenuBuilder getPopupMenuBuilder() { 
    	Activity activity = getActivity();
		if (activity != null && activity instanceof BaseActivity) { 
			BaseActivity activityBase = (BaseActivity)activity; 
			return activityBase.getPopupMenuBuilder();
		}
		return null;
    }
    
    public final void setLayoutBinder(LayoutBinder binder) { 
    	mLayoutBinder = binder; 
    }
    
    public final LayoutBinder getLayoutBinder() { 
    	return mLayoutBinder; 
    }
    
    protected View inflateSettingScreenView(SettingScreen screen) { 
    	LayoutBinder binder = getLayoutBinder(); 
    	if (binder != null) 
    		return binder.inflateSettingScreenView(screen); 
    	else 
    		return null; 
    }
    
    protected ListView getSettingListView(View view) { 
    	LayoutBinder binder = getLayoutBinder(); 
    	if (binder != null) 
    		return binder.getSettingListView(view); 
    	else 
    		return null; 
    }
    
    protected Dialog createSettingScreenDialog(SettingScreen screen) { 
    	LayoutBinder binder = getLayoutBinder(); 
    	if (binder != null) 
    		return binder.createSettingScreenDialog(screen); 
    	else 
    		return null; 
    }
    
    protected int getSettingItemResource(Setting setting) { 
    	LayoutBinder binder = getLayoutBinder(); 
    	if (binder != null) 
    		return binder.getSettingItemResource(setting); 
    	else 
    		return 0; 
    }
    
    protected void bindSettingItemView(Setting setting, View view) { 
		LayoutBinder binder = getLayoutBinder(); 
    	if (binder != null) 
    		binder.bindSettingItemView(setting, view); 
	}
    
    /**
     * Returns an adapter that can be attached to a {@link SettingActivity}
     * to show the settings contained in this {@link SettingScreen}.
     * <p>
     * This {@link SettingScreen} will NOT appear in the returned adapter, instead
     * it appears in the hierarchy above this {@link SettingScreen}.
     * <p>
     * This adapter's {@link Adapter#getItem(int)} should always return a
     * subclass of {@link Setting}.
     * 
     * @return An adapter that provides the {@link Setting} contained in this
     *         {@link SettingScreen}.
     */
    public synchronized ListAdapter getAdapter() {
        if (mAdapter == null) 
            mAdapter = onCreateAdapter();
        
        return mAdapter;
    }
    
    public synchronized void setScreenAdapter(ListAdapter adapter) { 
    	mAdapter = adapter; 
    }
    
    /**
     * Creates the root adapter.
     * 
     * @return An adapter that contains the settings contained in this {@link SettingScreen}.
     * @see #getAdapter()
     */
    protected ListAdapter onCreateAdapter() {
        return new SettingGroupAdapter(this);
    }

    /**
     * Binds a {@link ListView} to the settings contained in this {@link SettingScreen} via
     * {@link #getAdapter()}. It also handles passing list item clicks to the corresponding
     * {@link Setting} contained by this {@link SettingScreen}.
     * 
     * @param listView The list view to attach to.
     */
    public void bind(ListView listView) {
    	if (listView == null) 
    		return;
    	
        listView.setOnItemClickListener(this);
        listView.setAdapter(getAdapter());
        
        onAttachedToActivity();
    }
    
    @Override
    protected void onClick() {
        if (getIntent() != null || getSettingCount() == 0) 
            return;
        
        showSettingDialog(null);
    }
    
    public void showActivityDialog(final int id) { 
    	Activity activity = getActivity(); 
    	if (activity != null)
    		activity.showDialog(id);
    }
    
    public Dialog onCreateActivityDialog(final int id) {
    	return null;
    }
    
    public boolean onPrepareActivityDialog(final int id, Dialog dialog) {
    	return false;
    }
    
    public final void showSettingDialog(Bundle state) {
    	Dialog dialog = mDialog; 
    	if (dialog != null) return; 
    	
    	View view = inflateSettingScreenView(this); 
        if (view != null) 
	        bind(getSettingListView(view)); 
        
        showSettingDialog(state, view);
    }
    
    public final void showSettingDialog(Bundle state, View view) {
    	Dialog dialog = mDialog; 
    	if (dialog != null) return; 
    	
    	dialog = mDialog = createSettingScreenDialog(this); 
        if (dialog == null) 
        	return;
    	
        dialog.setOnDismissListener(this);
        
        if (view != null) 
	        dialog.setContentView(view);
        
        if (state != null) 
            dialog.onRestoreInstanceState(state);

        onSettingDialogShow(dialog);
        
        dialog.show();
    }
    
    protected void onSettingDialogShow(Dialog dialog) { 
    	if (dialog == null) return;
    	
    	// Add the screen to the list of preferences screens opened as dialogs
        getSettingManager().addSettingsScreen(dialog);
    }
    
    @Override 
    public void onDismiss(DialogInterface dialog) {
        mDialog = null;
        getSettingManager().removeSettingsScreen(dialog);
    }
    
    public final void dismissSettingDialog() { 
    	Dialog dialog = mDialog; 
    	if (dialog != null) 
    		dialog.dismiss(); 
    }
    
    /**
     * Used to get a handle to the dialog. 
     * This is useful for cases where we want to manipulate the dialog
     * as we would with any other activity or view.
     */
    public final Dialog getSettingDialog() {
        return mDialog;
    }
    
    @Override 
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Object item = getAdapter().getItem(position);
        if (item != null && item instanceof Setting) { 
	        final Setting setting = (Setting) item; 
	        setting.performClick(this);
        }
    }

    @Override
    protected boolean isOnSameScreenAsChildren() {
        return false;
    }
    
    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        final Dialog dialog = mDialog;
        if (dialog == null || !dialog.isShowing()) {
            return superState;
        }
        
        final SavedState myState = new SavedState(superState);
        myState.isDialogShowing = true;
        myState.dialogBundle = dialog.onSaveInstanceState();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }
         
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        if (myState.isDialogShowing) {
            showSettingDialog(myState.dialogBundle);
        }
    }
    
    private static class SavedState extends BaseSavedState {
        boolean isDialogShowing;
        Bundle dialogBundle;
        
        public SavedState(Parcel source) {
            super(source);
            isDialogShowing = source.readInt() == 1;
            dialogBundle = source.readBundle();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(isDialogShowing ? 1 : 0);
            dest.writeBundle(dialogBundle);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @SuppressWarnings("unused")
		public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
    
    /**
     * Sets the callback to be invoked when a {@link Setting} in the
     * hierarchy rooted at this {@link SettingManager} is clicked.
     * 
     * @param listener The callback to be invoked.
     */
    public void setOnSettingTreeClickListener(OnSettingTreeClickListener listener) {
        mOnSettingTreeClickListener = listener;
    }

    public OnSettingTreeClickListener getOnSettingTreeClickListener() {
        return mOnSettingTreeClickListener;
    }
    
    public void setOnSettingIntentClickListener(OnSettingIntentClickListener listener) { 
    	mOnSettingIntentClickListener = listener;
    }
    
    public OnSettingIntentClickListener getOnSettingIntentClickListener() {
    	return mOnSettingIntentClickListener;
    }
    
    public void setOnBindListener(OnBindListener listener) { 
    	mOnBindListener = listener; 
    }
    
    public OnBindListener getOnBindListener() { 
    	return mOnBindListener; 
    }
    
    public SettingCategory createSettingCategory(String key) { 
    	SettingCategory category = new SettingCategory(getSettingManager()); 
    	category.setKey(key); 
    	return category; 
    }
    
}
