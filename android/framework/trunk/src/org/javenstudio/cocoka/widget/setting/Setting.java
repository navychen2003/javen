package org.javenstudio.cocoka.widget.setting;

import java.util.ArrayList;
import java.util.List;

import org.javenstudio.cocoka.android.ResourceContext;
import org.javenstudio.common.util.Logger;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.AbsSavedState;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Represents the basic Preference UI building
 * block displayed by a {@link PreferenceActivity} in the form of a
 * {@link ListView}. This class provides the {@link View} to be displayed in
 * the activity and associates with a {@link SharedPreferences} to
 * store/retrieve the preference data.
 * <p>
 * When specifying a preference hierarchy in XML, each element can point to a
 * subclass of {@link Preference}, similar to the view hierarchy and layouts.
 * <p>
 * This class contains a {@code key} that will be used as the key into the
 * {@link SharedPreferences}. It is up to the subclass to decide how to store
 * the value.
 * 
 */
public class Setting implements Comparable<Setting>, OnDependencyChangeListener {
	private static final Logger LOG = Logger.getLogger(Setting.class);
	
	/**
     * Specify for {@link #setOrder(int)} if a specific order is not required.
     */
    public static final int DEFAULT_ORDER = Integer.MAX_VALUE;

    private final SettingManager mSettingManager;
    
    /**
     * Set when added to hierarchy since we need a unique ID within that
     * hierarchy.
     */
    private final long mId;
    
    private OnSettingChangeListener mOnChangeListener;
    private OnSettingClickListener mOnClickListener;

    private int mOrder = DEFAULT_ORDER;
    private int mGroupIndex = 0; 
    private CharSequence mTitle = null;
    private CharSequence mSubTitle = null;
    private CharSequence mSummary = null;
    private CharSequence mMessage = null;
    private Drawable mIcon = null; 
    private String mKey = null;
    private Intent mIntent = null;
    private Object mData = null;
    private boolean mEnabled = true;
    private boolean mSelectable = true;
    private boolean mRequiresKey;
    private boolean mPersistent = false;
    private String mDependencyKey;
    private boolean mDependencyMet = true;
    
    private SettingGroup mParent = null; 
    private boolean mRequired = false;
    private boolean mHidden = false;
    private int mGroupDisplayIndex = -1;
    private int mFlag = 0;
    
    /**
     * @see #setShouldDisableView(boolean)
     */
    private boolean mShouldDisableView = true;
    
    private OnSettingChangeInternalListener mListener;
    
    private List<Setting> mDependents;
    
    private boolean mBaseMethodCalled;
    
    /**
     * Interface definition for a callback to be invoked when the value of this
     * {@link Setting} has been changed by the user and is
     * about to be set and/or persisted.  This gives the client a chance
     * to prevent setting and/or persisting the value.
     */
    public interface OnSettingChangeListener {
        /**
         * Called when a Setting has been changed by the user. This is
         * called before the state of the Setting is about to be updated and
         * before the state is persisted.
         * 
         * @param setting The changed Setting.
         * @param newValue The new value of the Setting.
         * @return True to update the state of the Setting with the new value.
         */
        boolean onSettingChange(Setting setting, Object newValue);
    }

    /**
     * Interface definition for a callback to be invoked when a {@link Setting} is
     * clicked.
     */
    public interface OnSettingClickListener {
        /**
         * Called when a Setting has been clicked.
         *
         * @param setting The Setting that was clicked.
         * @return True if the click was handled.
         */
        boolean onSettingClick(Setting setting);
    }

    /**
     * Interface definition for a callback to be invoked when this
     * {@link Setting} is changed or, if this is a group, there is an
     * addition/removal of {@link Setting}(s). This is used internally.
     */
    public interface OnSettingChangeInternalListener {
        /**
         * Called when this Setting has changed.
         * 
         * @param setting This setting.
         */
        void onSettingChange(Setting setting);
        
        /**
         * Called when this group has added/removed {@link Setting}(s).
         * 
         * @param setting This Setting.
         */
        void onSettingHierarchyChange(Setting setting);
    }

    public interface OnSettingDependencyChangeListener { 
    	public void onSettingDependencyChange(Setting dependency, boolean disableDependent);
    }
    
    public interface OnSettingFocusChangeListener { 
    	public void onSettingFocusChange(Setting setting, boolean hasFocus);
    }
    
    public interface OnSettingViewBindListener { 
    	public void onBindSettingView(Setting setting); 
    }
    
    public interface OnGetSettingViewListener { 
    	public void onGetSettingView(Setting setting); 
    }
    
    public interface ViewBinder { 
    	public int getViewResource(); 
    	public boolean bindSettingView(Setting setting, View view); 
    	public boolean bindSettingBackground(Setting setting, View view); 
    }
    
    private ViewBinder mViewBinder = null; 
    private OnSettingFocusChangeListener mFocusListener = null;
    private OnSettingViewBindListener mBindListener = null; 
    private OnGetSettingViewListener mGetViewListener = null;
    
    private OnSettingDependencyChangeListener mDependencyListener = null;
    
    public void setOnDependencyChangeListener(OnSettingDependencyChangeListener listener) { 
    	mDependencyListener = listener;
    }
    
    public OnSettingDependencyChangeListener getOnDependencyChangeListener() { 
    	return mDependencyListener;
    }
    
    public void setViewBinder(ViewBinder binder) { 
    	mViewBinder = binder;
    }
    
    public ViewBinder getViewBinder() { 
    	return mViewBinder; 
    }
    
    public void setOnGetSettingViewListener(OnGetSettingViewListener listener) { 
    	mGetViewListener = listener;
    }
    
    public void callGetSettingViewListener() { 
    	OnGetSettingViewListener listener = mGetViewListener;
    	if (listener != null) 
    		listener.onGetSettingView(this);
    }
    
    public void setOnSettingFocusChangeListener(OnSettingFocusChangeListener listener) { 
    	mFocusListener = listener; 
    }
    
    public OnSettingFocusChangeListener getOnSettingFocusChangeListener() { 
    	return mFocusListener; 
    }
    
    public void callFocusChangeListener(boolean hasFocus) { 
    	OnSettingFocusChangeListener listener = mFocusListener; 
    	if (listener != null) 
    		listener.onSettingFocusChange(this, hasFocus);
    }
    
    public void setOnSettingViewBindListener(OnSettingViewBindListener listener) { 
    	mBindListener = listener;
    }
    
    public OnSettingViewBindListener getOnSettingViewBindListener() { 
    	return mBindListener;
    }
    
    public void callSettingViewBindListener() { 
    	OnSettingViewBindListener listener = mBindListener; 
    	if (listener != null) 
    		listener.onBindSettingView(this);
    }
    
    public void setFlag(int flag) { 
    	if (flag != mFlag) { 
    		mFlag = flag;
    		notifyDependencyChange(shouldDisableDependents());
            notifyChanged();
    	}
    }
    
    public int getFlag() { 
    	return mFlag;
    }
    
    public boolean isRequired() { 
    	return mRequired;
    }
    
    public void setRequired(boolean required) { 
    	mRequired = required;
    }
    
    public boolean isEmptyValue() { 
    	return true;
    }
    
    public void setHidden(boolean hide) { 
    	if (hide != mHidden) {
    		mHidden = hide;
    		mGroupDisplayIndex = -1;
    		
    		SettingGroup group = getParent(); 
    		if (group != null) 
    			group.setDisplayCountChanged();
    	}
    }
    
    public boolean isHidden() { 
    	return mHidden;
    }
    
    public Setting(SettingManager manager) {
    	this(manager, null); 
    }
    
    /**
     * Perform inflation from XML and apply a class-specific base style. This
     * constructor of Setting allows subclasses to use their own base
     * style when they are inflating. For example, a {@link CheckBoxSetting}
     * constructor calls this version of the super class constructor and
     * supplies {@code android.R.attr.checkBoxSettingStyle} for <var>defStyle</var>.
     * This allows the theme's checkbox setting style to modify all of the base
     * setting attributes as well as the {@link CheckBoxSetting} class's
     * attributes.
     * 
     * @param context The Context this is associated with, through which it can
     *            access the current theme, resources, {@link SharedPreferences},
     *            etc.
     * @param attrs The attributes of the XML tag that is inflating the setting.
     * @param defStyle The default style to apply to this setting. If 0, no style
     *            will be applied (beyond what is included in the theme). This
     *            may either be an attribute resource, whose value will be
     *            retrieved from the current theme, or an explicit style
     *            resource.
     * @see #Setting(Context, AttributeSet)
     */
    public Setting(SettingManager manager, AttributeSet attrs) {
        mSettingManager = manager;
        mId = manager.getNextId(); 
    }
    
    public SettingGroup getParent() { 
    	return mParent; 
    }
    
    protected void setParent(SettingGroup group) { 
    	mParent = group; 
    }
    
    public SettingScreen getParentScreen() { 
    	SettingGroup group = getParent(); 
    	if (group == null) 
    		return null; 
    	
    	if (group instanceof SettingScreen) 
    		return (SettingScreen)group; 
    	
    	return group.getParentScreen(); 
    }
    
    /**
     * Sets an {@link Intent} to be used for
     * {@link Context#startActivity(Intent)} when this Setting is clicked.
     * 
     * @param intent The intent associated with this Setting.
     */
    public void setIntent(Intent intent) {
        mIntent = intent;
    }
    
    /**
     * Return the {@link Intent} associated with this Setting.
     * 
     * @return The {@link Intent} last set via {@link #setIntent(Intent)} or XML. 
     */
    public Intent getIntent() {
        return mIntent;
    }

    public void setData(Object data) { 
    	mData = data; 
    }
    
    public Object getData() { 
    	return mData; 
    }
    
    /**
     * Gets the View that will be shown in the {@link SettingActivity}.
     * 
     * @param convertView The old View to reuse, if possible. Note: You should
     *            check that this View is non-null and of an appropriate type
     *            before using. If it is not possible to convert this View to
     *            display the correct data, this method can create a new View.
     * @param parent The parent that this View will eventually be attached to.
     * @return Returns the same Setting object, for chaining multiple calls
     *         into a single statement.
     * @see #onCreateView(ViewGroup)
     * @see #onBindView(View)
     */
    public View getView(View convertView, ViewGroup parent) {
    	callGetSettingViewListener();
    	
        if (convertView == null) 
            convertView = onCreateView(parent);
        
        onBindView(convertView);
        return convertView;
    }
    
    /**
     * Creates the View to be shown for this Setting in the
     * {@link SettingActivity}. The default behavior is to inflate the main
     * layout of this Setting (see {@link #setLayoutResource(int)}. If
     * changing this behavior, please specify a {@link ViewGroup} with ID
     * {@link android.R.id#widget_frame}.
     * <p>
     * Make sure to call through to the superclass's implementation.
     * 
     * @param parent The parent that this View will eventually be attached to.
     * @return The View that displays this Setting.
     * @see #onBindView(View)
     */
    protected View onCreateView(ViewGroup parent) {
        final LayoutInflater layoutInflater =
            (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        final View layout = layoutInflater.inflate(getSettingItemResource(), parent, false); 
        
        return layout;
    }
    
    /**
     * Binds the created View to the data for this Setting.
     * <p>
     * This is a good place to grab references to custom Views in the layout and
     * set properties on them.
     * <p>
     * Make sure to call through to the superclass's implementation.
     * 
     * @param view The View that shows this Setting.
     * @see #onCreateView(ViewGroup)
     */
    protected void onBindView(View view) {
    	SettingScreen screen = getParentScreen();
    	if (screen != null) { 
        	screen.bindSettingItemView(this, view); 
        	return;
    	}
    	
    	if (this instanceof SettingScreen) { 
    		((SettingScreen)this).bindSettingItemView(this, view); 
    		return;
    	}
    }
    
    protected int getSettingItemResource() { 
    	SettingScreen screen = getParentScreen();
    	if (screen != null)
    		return screen.getSettingItemResource(this); 
    	
    	if (this instanceof SettingScreen) 
    		return ((SettingScreen)this).getSettingItemResource(this); 
    	
    	return 0;
    }
    
    /**
     * Makes sure the view (and any children) get the enabled state changed.
     */
    protected void setEnabledStateOnViews(View v, boolean enabled) {
        v.setEnabled(enabled);
        
        if (v instanceof ViewGroup) {
            final ViewGroup vg = (ViewGroup) v;
            for (int i = vg.getChildCount() - 1; i >= 0; i--) {
                setEnabledStateOnViews(vg.getChildAt(i), enabled);
            }
        }
    }
    
    /**
     * Sets the order of this Setting with respect to other
     * Setting objects on the same level. If this is not specified, the
     * default behavior is to sort alphabetically. The
     * {@link SettingGroup#setOrderingAsAdded(boolean)} can be used to order
     * Setting objects based on the order they appear in the XML.
     * 
     * @param order The order for this Setting. A lower value will be shown
     *            first. Use {@link #DEFAULT_ORDER} to sort alphabetically or
     *            allow ordering from XML.
     * @see SettingGroup#setOrderingAsAdded(boolean)
     * @see #DEFAULT_ORDER
     */
    public void setOrder(int order) {
        if (order != mOrder) {
            mOrder = order;

            // Reorder the list 
            notifyHierarchyChanged();
        }
    }
    
    /**
     * Gets the order of this Setting with respect to other Setting objects
     * on the same level.
     * 
     * @return The order of this Setting.
     * @see #setOrder(int)
     */
    public int getOrder() {
        return mOrder;
    }

    protected void setGroupIndex(int index) { 
    	if (index != mGroupIndex) { 
    		mGroupIndex = index; 
    		
    		//notifyHierarchyChanged(); 
    	}
    }
    
    public int getGroupIndex() { 
    	return mGroupIndex; 
    }
    
    void setGroupDisplayIndex(int index) { 
    	if (index != mGroupDisplayIndex) { 
    		mGroupDisplayIndex = index; 
    		
    		//notifyHierarchyChanged(); 
    	}
    }
    
    int getGroupDisplayIndex() { 
    	return mGroupDisplayIndex;
    }
    
    public void setIcon(Drawable icon) { 
    	if ((icon == null && mIcon != null) || (icon != null && icon != mIcon)) { 
    		mIcon = icon; 
    		notifyChanged(); 
    	}
    }
    
    public void setIcon(int iconRes) { 
    	setIcon(getResourceContext().getDrawable(iconRes)); 
    }
    
    public Drawable getIcon() { 
    	return mIcon; 
    }
    
    /**
     * Sets the title for this Preference with a CharSequence. 
     * This title will be placed into the ID
     * {@link android.R.id#title} within the View created by
     * {@link #onCreateView(ViewGroup)}.
     * 
     * @param title The title for this Preference.
     */
    public void setTitle(CharSequence title) {
        if ((title == null && mTitle != null) || (title != null && !title.equals(mTitle))) {
            mTitle = title;
            notifyChanged();
        }
    }
    
    /**
     * Sets the title for this Preference with a resource ID. 
     * 
     * @see #setTitle(CharSequence)
     * @param titleResId The title as a resource ID.
     */
    public void setTitle(int titleResId) {
        setTitle(getResourceContext().getString(titleResId));
    }
    
    /**
     * Returns the title of this Preference.
     * 
     * @return The title.
     * @see #setTitle(CharSequence)
     */
    public CharSequence getTitle() {
        return mTitle;
    }

    /**
     * Returns the summary of this Preference.
     * 
     * @return The summary.
     * @see #setSummary(CharSequence)
     */
    public CharSequence getSummary() {
        return mSummary;
    }

    /**
     * Sets the summary for this Preference with a CharSequence. 
     * 
     * @param summary The summary for the preference.
     */
    public void setSummary(CharSequence summary) {
        if ((summary == null && mSummary != null) || (summary != null && !summary.equals(mSummary))) {
            mSummary = summary;
            notifyChanged();
        }
    }

    /**
     * Sets the summary for this Preference with a resource ID. 
     * 
     * @see #setSummary(CharSequence)
     * @param summaryResId The summary as a resource.
     */
    public void setSummary(int summaryResId) {
        setSummary(getResourceContext().getString(summaryResId));
    }
    
    public CharSequence getMessage() { 
    	return mMessage;
    }
    
    public void setMessage(CharSequence message) {
        if ((message == null && mMessage != null) || (message != null && !message.equals(mMessage))) {
        	mMessage = message;
            notifyChanged();
        }
    }
    
    public void setMessage(int messageResId) { 
    	setMessage(getResourceContext().getString(messageResId));
    }
    
    public CharSequence getSubTitle() { 
    	return mSubTitle;
    }
    
    public void setSubTitle(CharSequence title) {
        if ((title == null && mSubTitle != null) || (title != null && !title.equals(mSubTitle))) {
        	mSubTitle = title;
            notifyChanged();
        }
    }
    
    public void setSubTitle(int titleResId) { 
    	setSubTitle(getResourceContext().getString(titleResId));
    }
    
    /**
     * Sets whether this Setting is enabled. If disabled, it will
     * not handle clicks.
     * 
     * @param enabled Set true to enable it.
     */
    public void setEnabled(boolean enabled) {
        if (mEnabled != enabled) {
            mEnabled = enabled;

            // Enabled state can change dependent settings' states, so notify
            notifyDependencyChange(shouldDisableDependents());

            notifyChanged();
        }
    }
    
    /**
     * Checks whether this Setting should be enabled in the list.
     * 
     * @return True if this Setting is enabled, false otherwise.
     */
    public boolean isEnabled() {
        return mEnabled && mDependencyMet;
    }

    /**
     * Sets whether this Setting is selectable.
     * 
     * @param selectable Set true to make it selectable.
     */
    public void setSelectable(boolean selectable) {
        if (mSelectable != selectable) {
            mSelectable = selectable;
            notifyChanged();
        }
    }
    
    /**
     * Checks whether this Setting should be selectable in the list.
     * 
     * @return True if it is selectable, false otherwise.
     */
    public boolean isSelectable() {
        return mSelectable && mDependencyMet;
    }

    /**
     * Sets whether this Setting should disable its view when it gets
     * disabled.
     * <p>
     * For example, set this and {@link #setEnabled(boolean)} to false for
     * settings that are only displaying information and 1) should not be
     * clickable 2) should not have the view set to the disabled state.
     * 
     * @param shouldDisableView Set true if this setting should disable its view
     *            when the setting is disabled.
     */
    public void setShouldDisableView(boolean shouldDisableView) {
        mShouldDisableView = shouldDisableView;
        notifyChanged();
    }
    
    /**
     * Checks whether this Setting should disable its view when it's action is disabled.
     * @see #setShouldDisableView(boolean)
     * @return True if it should disable the view. 
     */
    public boolean getShouldDisableView() {
        return mShouldDisableView;
    }

    /**
     * Returns a unique ID for this Setting.  This ID should be unique across all
     * Setting objects in a hierarchy.
     * 
     * @return A unique ID for this Setting.
     */
    public long getId() {
        return mId;
    }
    
    /**
     * Processes a click on the setting. This includes saving the value to
     * the {@link SharedPreferences}. However, the overridden method should
     * call {@link #callChangeListener(Object)} to make sure the client wants to
     * update the setting's state with the new value.
     */
    protected void onClick() {
    	// do nothing
    }
    
    /**
     * Sets the key for this Setting, which is used as a key to the
     * {@link SharedPreferences}. This should be unique for the package.
     * 
     * @param key The key for the setting.
     */
    public void setKey(String key) {
        mKey = key;
        
        if (mRequiresKey && !hasKey()) {
            requireKey();
        }
    }
    
    /**
     * Gets the key for this Setting, which is also the key used for storing
     * values into SharedPreferences.
     * 
     * @return The key.
     */
    public String getKey() {
        return mKey;
    }
    
    /**
     * Checks whether the key is present, and if it isn't throws an
     * exception. This should be called by subclasses that store settings in
     * the {@link SharedPreferences}.
     * 
     * @throws IllegalStateException If there is no key assigned.
     */
    protected void requireKey() {
        if (mKey == null) {
            throw new IllegalStateException("Setting does not have a key assigned.");
        }
        
        mRequiresKey = true;
    }
    
    /**
     * Checks whether this Setting has a valid key.
     * 
     * @return True if the key exists and is not a blank string, false otherwise.
     */
    public boolean hasKey() {
        return !TextUtils.isEmpty(mKey);
    }
    
    /**
     * Checks whether this Setting is persistent. If it is, it stores its value(s) into
     * the persistent {@link SharedPreferences} storage.
     * 
     * @return True if it is persistent.
     */
    public boolean isPersistent() {
        return mPersistent;
    }
    
    /**
     * Checks whether, at the given time this method is called,
     * this Setting should store/restore its value(s) into the
     * {@link SharedPreferences}. This, at minimum, checks whether this
     * Setting is persistent and it currently has a key. Before you
     * save/restore from the {@link SharedPreferences}, check this first.
     * 
     * @return True if it should persist the value.
     */
    protected boolean shouldPersist() {
        return mSettingManager != null && isPersistent() && hasKey();
    }
    
    /**
     * Sets whether this Setting is persistent. When persistent,
     * it stores its value(s) into the persistent {@link SharedPreferences}
     * storage.
     * 
     * @param persistent Set true if it should store its value(s) into the {@link SharedPreferences}.
     */
    public void setPersistent(boolean persistent) {
        mPersistent = persistent;
    }
    
    public final void changeValue(Object newValue) { 
    	if (!callChangeListener(newValue)) 
    		return;
    	
    	onChangeValue(newValue);
    }
    
    protected void onChangeValue(Object newValue) { 
    	// implemented at subclass
    }
    
    /**
     * Call this method after the user changes the setting, but before the
     * internal state is set. This allows the client to ignore the user value.
     * 
     * @param newValue The new value of this Setting.
     * @return True if the user value should be set as the setting
     *         value (and persisted).
     */
    protected boolean callChangeListener(Object newValue) {
        return mOnChangeListener == null ? true : mOnChangeListener.onSettingChange(this, newValue);
    }
    
    /**
     * Sets the callback to be invoked when this Setting is changed by the
     * user (but before the internal state has been updated).
     * 
     * @param onSettingChangeListener The callback to be invoked.
     */
    public void setOnSettingChangeListener(OnSettingChangeListener onSettingChangeListener) {
        mOnChangeListener = onSettingChangeListener;
    }

    /**
     * Returns the callback to be invoked when this Setting is changed by the
     * user (but before the internal state has been updated).
     * 
     * @return The callback to be invoked.
     */
    public OnSettingChangeListener getOnSettingChangeListener() {
        return mOnChangeListener;
    }

    /**
     * Sets the callback to be invoked when this Setting is clicked.
     * 
     * @param onSettingClickListener The callback to be invoked.
     */
    public void setOnSettingClickListener(OnSettingClickListener onSettingClickListener) {
        mOnClickListener = onSettingClickListener;
    }

    /**
     * Returns the callback to be invoked when this Setting is clicked.
     * 
     * @return The callback to be invoked.
     */
    public OnSettingClickListener getOnSettingClickListener() {
        return mOnClickListener;
    }

    /**
     * Called when a click should be performed.
     * 
     * @param settingScreen A {@link SettingScreen} whose hierarchy click
     *            listener should be called in the proper order (between other
     *            processing). May be null.
     */
    protected void performClick(SettingScreen settingScreen) {
        if (!isEnabled()) return;
        
        if (LOG.isDebugEnabled())
        	LOG.debug("performClick: setting=" + this);
        
        onClick();
        
        OnSettingClickListener clickListener = mOnClickListener;
        if (clickListener != null && clickListener.onSettingClick(this)) 
            return;
        
        SettingScreen screen = getParentScreen(); 
        if (screen != null) {
            OnSettingTreeClickListener listener = 
            		screen.getOnSettingTreeClickListener();
            
            if (settingScreen != null && listener != null && 
            		listener.onSettingTreeClick(settingScreen, this)) {
                return;
            }
        }
        
        Intent intent = mIntent;
        startActivity(settingScreen, intent);
    }
    
    protected void startActivity(SettingScreen settingScreen, Intent intent) { 
    	if (intent == null) return; 
    	
    	SettingScreen screen = getParentScreen(); 
    	OnSettingIntentClickListener listener = screen != null ? 
    			screen.getOnSettingIntentClickListener() : null; 
    	
    	if (listener == null || !listener.onSettingIntentClick(settingScreen, this, intent)) { 
            Context context = getContext();
            context.startActivity(intent);
    	}
    }
    
    /**
     * Returns the {@link android.content.Context} of this Setting. 
     * Each Setting in a Setting hierarchy can be
     * from different Context (for example, if multiple activities provide settings into a single
     * {@link SettingActivity}). This Context will be used to save the Setting values.
     * 
     * @return The Context of this Setting.
     */
    public final Context getContext() {
        return mSettingManager.getContext();
    }
    
    public Activity getActivity() { 
    	SettingScreen screen = getParentScreen();
    	if (screen != null) 
    		return screen.getActivity(); 
    	
    	return null;
    }
    
    public final ResourceContext getResourceContext() { 
    	return mSettingManager.getResourceContext(); 
    }
    
    /**
     * Returns the {@link SharedPreferences} where this Setting can read its
     * value(s). Usually, it's easier to use one of the helper read methods:
     * {@link #getPersistedBoolean(boolean)}, {@link #getPersistedFloat(float)},
     * {@link #getPersistedInt(int)}, {@link #getPersistedLong(long)},
     * {@link #getPersistedString(String)}. To save values, see
     * {@link #getEditor()}.
     * <p>
     * In some cases, writes to the {@link #getEditor()} will not be committed
     * right away and hence not show up in the returned
     * {@link SharedPreferences}, this is intended behavior to improve
     * performance.
     * 
     * @return The {@link SharedPreferences} where this Setting reads its
     *         value(s), or null if it isn't attached to a Setting hierarchy.
     * @see #getEditor()
     */
    public SharedPreferences getSharedPreferences() {
        if (mSettingManager == null) 
            return null;
        
        return mSettingManager.getSharedPreferences();
    }
    
    /**
     * Returns an {@link SharedPreferences.Editor} where this Setting can
     * save its value(s). Usually it's easier to use one of the helper save
     * methods: {@link #persistBoolean(boolean)}, {@link #persistFloat(float)},
     * {@link #persistInt(int)}, {@link #persistLong(long)},
     * {@link #persistString(String)}. To read values, see
     * {@link #getSharedPreferences()}. If {@link #shouldCommit()} returns
     * true, it is this Setting's responsibility to commit.
     * <p>
     * In some cases, writes to this will not be committed right away and hence
     * not show up in the SharedPreferences, this is intended behavior to
     * improve performance.
     * 
     * @return A {@link SharedPreferences.Editor} where this setting saves
     *         its value(s), or null if it isn't attached to a Setting
     *         hierarchy.
     * @see #shouldCommit()
     * @see #getSharedPreferences()
     */
    public SharedPreferences.Editor getEditor() {
        if (mSettingManager == null) 
            return null;
        
        return mSettingManager.getEditor();
    }
    
    /**
     * Returns whether the {@link Setting} should commit its saved value(s) in
     * {@link #getEditor()}. This may return false in situations where batch
     * committing is being done (by the manager) to improve performance.
     * 
     * @return Whether the Setting should commit its saved value(s).
     * @see #getEditor()
     */
    public boolean shouldCommit() {
        if (mSettingManager == null) 
            return false;
        
        return mSettingManager.shouldCommit();
    }
    
    /**
     * Compares Setting objects based on order (if set), otherwise alphabetically on the titles.
     * 
     * @param another The Setting to compare to this one.
     * @return 0 if the same; less than 0 if this Setting sorts ahead of <var>another</var>;
     *          greater than 0 if this Setting sorts after <var>another</var>.
     */
    public int compareTo(Setting another) {
        if (mOrder != DEFAULT_ORDER
                || (mOrder == DEFAULT_ORDER && another.mOrder != DEFAULT_ORDER)) {
            // Do order comparison
            return mOrder - another.mOrder; 
        } else {
            // Do id comparison
            return (int)(getId() - another.getId()); 
        }
    }
    
    /**
     * Sets the internal change listener.
     * 
     * @param listener The listener.
     * @see #notifyChanged()
     */
    public final void setOnSettingChangeInternalListener(OnSettingChangeInternalListener listener) {
        mListener = listener;
    }

    /**
     * Should be called when the data of this {@link Setting} has changed.
     */
    public void notifyChanged() {
        if (mListener != null) {
            mListener.onSettingChange(this);
        }
    }
    
    /**
     * Should be called when a Setting has been
     * added/removed from this group, or the ordering should be
     * re-evaluated.
     */
    public void notifyHierarchyChanged() {
        if (mListener != null) {
            mListener.onSettingHierarchyChange(this);
        }
    }

    /**
     * Gets the {@link SettingManager} that manages this Setting object's tree.
     * 
     * @return The {@link SettingManager}.
     */
    public SettingManager getSettingManager() {
        return mSettingManager;
    }
    
    /**
     * Called when this Setting has been attached to a Setting hierarchy.
     * Make sure to call the super implementation.
     * 
     * @param settingManager The SettingManager of the hierarchy.
     */
    protected void onAttachedToHierarchy() {
        //mSettingManager = settingManager;
        //mId = settingManager.getNextId();
        
        //dispatchSetInitialValue();
    }
    
    /**
     * Called when the Setting hierarchy has been attached to the
     * {@link SettingActivity}. This can also be called when this
     * Setting has been attached to a group that was already attached
     * to the {@link SettingActivity}.
     */
    protected void onAttachedToActivity() {
        // At this point, the hierarchy that this setting is in is connected
        // with all other settings.
        registerDependency();
    }

    protected void registerDependency() {
        if (TextUtils.isEmpty(mDependencyKey)) return;
        
        Setting setting = findSettingInHierarchy(mDependencyKey);
        if (setting != null) {
            setting.registerDependent(this);
        } else {
            throw new IllegalStateException("Dependency \"" + mDependencyKey
                    + "\" not found for setting \"" + mKey + "\"");
        }
    }

    protected void unregisterDependency() {
        if (mDependencyKey != null) {
            final Setting oldDependency = findSettingInHierarchy(mDependencyKey);
            if (oldDependency != null) 
                oldDependency.unregisterDependent(this);
        }
    }
    
    /**
     * Finds a Setting in this hierarchy (the whole thing,
     * even above/below your {@link SettingScreen} screen break) with the given
     * key.
     * <p>
     * This only functions after we have been attached to a hierarchy.
     * 
     * @param key The key of the Setting to find.
     * @return The Setting that uses the given key.
     */
    protected Setting findSettingInHierarchy(String key) {
        if (TextUtils.isEmpty(key) || mSettingManager == null) 
            return null;
        
        return mSettingManager.findSetting(key);
    }
    
    /**
     * Adds a dependent Setting on this Setting so we can notify it.
     * Usually, the dependent Setting registers itself (it's good for it to
     * know it depends on something), so please use
     * {@link Setting#setDependency(String)} on the dependent Setting.
     * 
     * @param dependent The dependent Setting that will be enabled/disabled
     *            according to the state of this Setting.
     */
    public void registerDependent(Setting dependent) {
        if (mDependents == null) 
            mDependents = new ArrayList<Setting>();
        
        mDependents.add(dependent);
        
        dependent.onDependencyChanged(this, shouldDisableDependents());
    }
    
    /**
     * Removes a dependent Setting on this Setting.
     * 
     * @param dependent The dependent Setting that will be enabled/disabled
     *            according to the state of this Setting.
     * @return Returns the same Setting object, for chaining multiple calls
     *         into a single statement.
     */
    public void unregisterDependent(Setting dependent) {
        if (mDependents != null) {
            mDependents.remove(dependent);
        }
    }
    
    /**
     * Notifies any listening dependents of a change that affects the
     * dependency.
     * 
     * @param disableDependents Whether this Setting should disable
     *            its dependents.
     */
    public void notifyDependencyChange(boolean disableDependents) {
        final List<Setting> dependents = mDependents;
        if (dependents == null) 
            return;
        
        final int dependentsCount = dependents.size();
        for (int i = 0; i < dependentsCount; i++) {
            dependents.get(i).onDependencyChanged(this, disableDependents);
        }
    }

    /**
     * Called when the dependency changes.
     * 
     * @param dependency The Setting that this Setting depends on.
     * @param disableDependent Set true to disable this Setting.
     */
    public void onDependencyChanged(Setting dependency, boolean disableDependent) {
    	OnSettingDependencyChangeListener listener = getOnDependencyChangeListener();
        if (listener != null) 
        	listener.onSettingDependencyChange(dependency, disableDependent);
        
        if (mDependencyMet == disableDependent) {
            mDependencyMet = !disableDependent;

            // Enabled state can change dependent settings' states, so notify
            notifyDependencyChange(shouldDisableDependents());

            notifyChanged();
        }
    }
    
    /**
     * Checks whether this setting's dependents should currently be
     * disabled.
     * 
     * @return True if the dependents should be disabled, otherwise false.
     */
    public boolean shouldDisableDependents() {
        return !isEnabled();
    }
    
    /**
     * Sets the key of a Setting that this Setting will depend on. If that
     * Setting is not set or is off, this Setting will be disabled.
     * 
     * @param dependencyKey The key of the Setting that this depends on.
     */
    public void setDependency(String dependencyKey) {
        // Unregister the old dependency, if we had one
        unregisterDependency();
        
        // Register the new
        mDependencyKey = dependencyKey;
        registerDependency();
    }
    
    /**
     * Returns the key of the dependency on this Setting.
     * 
     * @return The key of the dependency.
     * @see #setDependency(String)
     */
    public String getDependency() {
        return mDependencyKey;
    }
    
    /**
     * Called when this Setting is being removed from the hierarchy. You
     * should remove any references to this Setting that you know about. Make
     * sure to call through to the superclass implementation.
     */
    protected void onPrepareForRemoval() {
        unregisterDependency();
    }
    
    protected void tryCommit(SharedPreferences.Editor editor) {
        if (mSettingManager.shouldCommit()) {
            editor.commit();
        }
    }
    
    /**
     * Attempts to persist a String to the {@link android.content.SharedPreferences}.
     * <p>
     * This will check if this Setting is persistent, get an editor from
     * the {@link SettingManager}, put in the string, and check if we should commit (and
     * commit if so).
     * 
     * @param value The value to persist.
     * @return True if the Setting is persistent. (This is not whether the
     *         value was persisted, since we may not necessarily commit if there
     *         will be a batch commit later.)
     * @see #getPersistedString(String)
     */
    protected boolean persistString(String value) {
        if (shouldPersist()) {
            // Shouldn't store null
            if (value == getPersistedString(null)) {
                // It's already there, so the same as persisting
                return true;
            }
            
            SharedPreferences.Editor editor = mSettingManager.getEditor();
            editor.putString(mKey, value);
            tryCommit(editor);
            return true;
        }
        return false;
    }
    
    /**
     * Attempts to get a persisted String from the {@link android.content.SharedPreferences}.
     * <p>
     * This will check if this Setting is persistent, get the SharedPreferences
     * from the {@link SettingManager}, and get the value.
     * 
     * @param defaultReturnValue The default value to return if either the
     *            Setting is not persistent or the Setting is not in the
     *            shared settings.
     * @return The value from the SharedPreferences or the default return
     *         value.
     * @see #persistString(String)
     */
    protected String getPersistedString(String defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }
        
        return mSettingManager.getSharedPreferences().getString(mKey, defaultReturnValue);
    }
    
    /**
     * Attempts to persist an int to the {@link android.content.SharedPreferences}.
     * 
     * @param value The value to persist.
     * @return True if the Setting is persistent. (This is not whether the
     *         value was persisted, since we may not necessarily commit if there
     *         will be a batch commit later.)
     * @see #persistString(String)
     * @see #getPersistedInt(int)
     */
    protected boolean persistInt(int value) {
        if (shouldPersist()) {
            if (value == getPersistedInt(~value)) {
                // It's already there, so the same as persisting
                return true;
            }
            
            SharedPreferences.Editor editor = mSettingManager.getEditor();
            editor.putInt(mKey, value);
            tryCommit(editor);
            return true;
        }
        return false;
    }
    
    /**
     * Attempts to get a persisted int from the {@link android.content.SharedPreferences}.
     * 
     * @param defaultReturnValue The default value to return if either this
     *            Setting is not persistent or this Setting is not in the
     *            SharedPreferences.
     * @return The value from the SharedPreferences or the default return
     *         value.
     * @see #getPersistedString(String)
     * @see #persistInt(int)
     */
    protected int getPersistedInt(int defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }
        
        return mSettingManager.getSharedPreferences().getInt(mKey, defaultReturnValue);
    }
    
    /**
     * Attempts to persist a float to the {@link android.content.SharedPreferences}.
     * 
     * @param value The value to persist.
     * @return True if this Setting is persistent. (This is not whether the
     *         value was persisted, since we may not necessarily commit if there
     *         will be a batch commit later.)
     * @see #persistString(String)
     * @see #getPersistedFloat(float)
     */
    protected boolean persistFloat(float value) {
        if (shouldPersist()) {
            if (value == getPersistedFloat(Float.NaN)) {
                // It's already there, so the same as persisting
                return true;
            }

            SharedPreferences.Editor editor = mSettingManager.getEditor();
            editor.putFloat(mKey, value);
            tryCommit(editor);
            return true;
        }
        return false;
    }
    
    /**
     * Attempts to get a persisted float from the {@link android.content.SharedPreferences}.
     * 
     * @param defaultReturnValue The default value to return if either this
     *            Setting is not persistent or this Setting is not in the
     *            SharedPreferences.
     * @return The value from the SharedPreferences or the default return
     *         value.
     * @see #getPersistedString(String)
     * @see #persistFloat(float)
     */
    protected float getPersistedFloat(float defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }
        
        return mSettingManager.getSharedPreferences().getFloat(mKey, defaultReturnValue);
    }
    
    /**
     * Attempts to persist a long to the {@link android.content.SharedPreferences}.
     * 
     * @param value The value to persist.
     * @return True if this Setting is persistent. (This is not whether the
     *         value was persisted, since we may not necessarily commit if there
     *         will be a batch commit later.)
     * @see #persistString(String)
     * @see #getPersistedLong(long)
     */
    protected boolean persistLong(long value) {
        if (shouldPersist()) {
            if (value == getPersistedLong(~value)) {
                // It's already there, so the same as persisting
                return true;
            }
            
            SharedPreferences.Editor editor = mSettingManager.getEditor();
            editor.putLong(mKey, value);
            tryCommit(editor);
            return true;
        }
        return false;
    }
    
    /**
     * Attempts to get a persisted long from the {@link android.content.SharedPreferences}.
     * 
     * @param defaultReturnValue The default value to return if either this
     *            Setting is not persistent or this Setting is not in the
     *            SharedPreferences.
     * @return The value from the SharedPreferences or the default return
     *         value.
     * @see #getPersistedString(String)
     * @see #persistLong(long)
     */
    protected long getPersistedLong(long defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }
        
        return mSettingManager.getSharedPreferences().getLong(mKey, defaultReturnValue);
    }
    
    /**
     * Attempts to persist a boolean to the {@link android.content.SharedPreferences}.
     * 
     * @param value The value to persist.
     * @return True if this Setting is persistent. (This is not whether the
     *         value was persisted, since we may not necessarily commit if there
     *         will be a batch commit later.)
     * @see #persistString(String)
     * @see #getPersistedBoolean(boolean)
     */
    protected boolean persistBoolean(boolean value) {
        if (shouldPersist()) {
            if (value == getPersistedBoolean(!value)) {
                // It's already there, so the same as persisting
                return true;
            }
            
            SharedPreferences.Editor editor = mSettingManager.getEditor();
            editor.putBoolean(mKey, value);
            tryCommit(editor);
            return true;
        }
        return false;
    }
    
    /**
     * Attempts to get a persisted boolean from the {@link android.content.SharedPreferences}.
     * 
     * @param defaultReturnValue The default value to return if either this
     *            Setting is not persistent or this Setting is not in the
     *            SharedPreferences.
     * @return The value from the SharedPreferences or the default return
     *         value.
     * @see #getPersistedString(String)
     * @see #persistBoolean(boolean)
     */
    protected boolean getPersistedBoolean(boolean defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }
        
        return mSettingManager.getSharedPreferences().getBoolean(mKey, defaultReturnValue);
    }
    
    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName()); 
        sb.append("-" + mId);
        if (mKey != null) { 
        	sb.append("{key="); 
        	sb.append(mKey); 
        	sb.append("}");
        }
        return sb.toString();
    }
        
    /**
     * Returns the text that will be used to filter this Setting depending on
     * user input.
     * <p>
     * If overridding and calling through to the superclass, make sure to prepend
     * your additions with a space.
     * 
     * @return Text as a {@link StringBuilder} that will be used to filter this
     *         setting. By default, this is the title and summary
     *         (concatenated with a space).
     */
    @SuppressWarnings("unused")
	private StringBuilder getFilterableStringBuilder2() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName()); 
        sb.append("-" + mId);
        if (mKey != null) { 
        	sb.append("{key="); 
        	sb.append(mKey); 
        	sb.append("}");
        }
        return sb;
    }

    /**
     * Store this Setting hierarchy's frozen state into the given container.
     * 
     * @param container The Bundle in which to save the instance of this Setting.
     * 
     * @see #restoreHierarchyState
     * @see #onSaveInstanceState
     */
    public void saveHierarchyState(Bundle container) {
        dispatchSaveInstanceState(container);
    }

    /**
     * Called by {@link #saveHierarchyState} to store the instance for this Setting and its children.
     * May be overridden to modify how the save happens for children. For example, some
     * Setting objects may want to not store an instance for their children.
     * 
     * @param container The Bundle in which to save the instance of this Setting.
     * 
     * @see #saveHierarchyState
     * @see #onSaveInstanceState
     */
    protected void dispatchSaveInstanceState(Bundle container) {
        if (hasKey()) {
            mBaseMethodCalled = false;
            Parcelable state = onSaveInstanceState();
            if (!mBaseMethodCalled) {
                throw new IllegalStateException(
                        "Derived class did not call super.onSaveInstanceState()");
            }
            if (state != null) {
                container.putParcelable(mKey, state);
            }
        }
    }

    /**
     * Hook allowing a Setting to generate a representation of its internal
     * state that can later be used to create a new instance with that same
     * state. This state should only contain information that is not persistent
     * or can be reconstructed later.
     * 
     * @return A Parcelable object containing the current dynamic state of
     *         this Setting, or null if there is nothing interesting to save.
     *         The default implementation returns null.
     * @see #onRestoreInstanceState
     * @see #saveHierarchyState
     */
    protected Parcelable onSaveInstanceState() {
        mBaseMethodCalled = true;
        return BaseSavedState.EMPTY_STATE;
    }

    /**
     * Restore this Setting hierarchy's previously saved state from the given container.
     * 
     * @param container The Bundle that holds the previously saved state.
     * 
     * @see #saveHierarchyState
     * @see #onRestoreInstanceState
     */
    public void restoreHierarchyState(Bundle container) {
        dispatchRestoreInstanceState(container);
    }

    /**
     * Called by {@link #restoreHierarchyState} to retrieve the saved state for this
     * Setting and its children. May be overridden to modify how restoring
     * happens to the children of a Setting. For example, some Setting objects may
     * not want to save state for their children.
     * 
     * @param container The Bundle that holds the previously saved state.
     * @see #restoreHierarchyState
     * @see #onRestoreInstanceState
     */
    protected void dispatchRestoreInstanceState(Bundle container) {
        if (hasKey()) {
            Parcelable state = container.getParcelable(mKey);
            if (state != null) {
                mBaseMethodCalled = false;
                onRestoreInstanceState(state);
                if (!mBaseMethodCalled) {
                    throw new IllegalStateException(
                            "Derived class did not call super.onRestoreInstanceState()");
                }
            }
        }
    }

    /**
     * Hook allowing a Setting to re-apply a representation of its internal
     * state that had previously been generated by {@link #onSaveInstanceState}.
     * This function will never be called with a null state.
     * 
     * @param state The saved state that had previously been returned by
     *            {@link #onSaveInstanceState}.
     * @see #onSaveInstanceState
     * @see #restoreHierarchyState
     */
    protected void onRestoreInstanceState(Parcelable state) {
        mBaseMethodCalled = true;
        if (state != BaseSavedState.EMPTY_STATE && state != null) {
            throw new IllegalArgumentException("Wrong state class -- expecting Setting State");
        }
    }

    /**
     * A base class for managing the instance state of a {@link Setting}.
     */
    public static class BaseSavedState extends AbsSavedState {
        public BaseSavedState(Parcel source) {
            super(source);
        }

        public BaseSavedState(Parcelable superState) {
            super(superState);
        }
        
        public static final Parcelable.Creator<BaseSavedState> CREATOR =
                new Parcelable.Creator<BaseSavedState>() {
            public BaseSavedState createFromParcel(Parcel in) {
                return new BaseSavedState(in);
            }

            public BaseSavedState[] newArray(int size) {
                return new BaseSavedState[size];
            }
        };
    }
    
}
