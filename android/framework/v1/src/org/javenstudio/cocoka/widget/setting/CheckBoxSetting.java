package org.javenstudio.cocoka.widget.setting;

import android.app.Service;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.Checkable;
import android.widget.TextView;

/**
 * A {@link Setting} that provides checkbox widget
 * functionality.
 * <p>
 * This preference will store a boolean into the SharedPreferences.
 * 
 * @attr ref android.R.styleable#CheckBoxSetting_summaryOff
 * @attr ref android.R.styleable#CheckBoxSetting_summaryOn
 * @attr ref android.R.styleable#CheckBoxSetting_disableDependentsState
 */
public class CheckBoxSetting extends Setting implements CheckBoxButton.CheckBoxAdapter {

    private CharSequence mSummaryOn = null;
    private CharSequence mSummaryOff = null;
    
    private boolean mCheckable = true;
    private boolean mChecked = false;
    private boolean mIsRadio = false;
    private boolean mSendAccessibilityEventViewClickedType = false;
    private boolean mChangeCheckedOnClick = false;

    private AccessibilityManager mAccessibilityManager = null;
    private boolean mDisableDependentsState = false;
    
    public CheckBoxSetting(SettingManager manager) {
    	this(manager, null); 
    }
    
    public CheckBoxSetting(SettingManager manager, AttributeSet attrs) {
        super(manager, attrs);
        
        mAccessibilityManager =
            (AccessibilityManager) getContext().getSystemService(Service.ACCESSIBILITY_SERVICE);
    }

    public void bindCheckboxView(View checkboxView) { 
        if (checkboxView != null && checkboxView instanceof Checkable) {
            ((Checkable) checkboxView).setChecked(mChecked);

            // send an event to announce the value change of the CheckBox and is done here
            // because clicking a preference does not immediately change the checked state
            // for example when enabling the WiFi
            if (mSendAccessibilityEventViewClickedType &&
                    mAccessibilityManager.isEnabled() &&
                    checkboxView.isEnabled()) {
                mSendAccessibilityEventViewClickedType = false;

                int eventType = AccessibilityEvent.TYPE_VIEW_CLICKED;
                checkboxView.sendAccessibilityEventUnchecked(AccessibilityEvent.obtain(eventType));
            }
        }
    }
    
    public void bindSummaryView(TextView summaryView) { 
        // Sync the summary view
        if (summaryView != null) {
            boolean useDefaultSummary = true;
            if (isChecked() && mSummaryOn != null) {
                summaryView.setText(mSummaryOn);
                useDefaultSummary = false;
            } else if (!isChecked() && mSummaryOff != null) {
                summaryView.setText(mSummaryOff);
                useDefaultSummary = false;
            }

            if (useDefaultSummary) {
                final CharSequence summary = getSummary();
                if (summary != null) {
                    summaryView.setText(summary);
                    useDefaultSummary = false;
                }
            }
            
            int newVisibility = View.GONE;
            if (!useDefaultSummary) {
                // Someone has written to it
                newVisibility = View.VISIBLE;
            }
            if (newVisibility != summaryView.getVisibility()) {
                summaryView.setVisibility(newVisibility);
            }
        }
    }
    
    @Override
    protected void onClick() {
        super.onClick();
        
        if (mChangeCheckedOnClick) { 
	        boolean newValue = !isChecked();
	        
	        // in onBindView() an AccessibilityEventViewClickedType is sent to announce the change
	        // not sending
	        mSendAccessibilityEventViewClickedType = true;
	
	        changeValue(newValue); 
        }
    }
    
    @Override
    protected void onChangeValue(Object newValue) { 
    	if (newValue != null && newValue instanceof Boolean)
    		setChecked(((Boolean)newValue).booleanValue()); 
    	else 
    		setChecked(false);
    }
    
    /**
     * Sets the checked state and saves it to the {@link SharedPreferences}.
     * 
     * @param checked The checked state.
     */
    public void setChecked(boolean checked) {
        if (isCheckable() && mChecked != checked) {
            mChecked = checked;
            persistBoolean(checked);
            notifyDependencyChange(shouldDisableDependents());
            notifyChanged();
        }
    }

    /**
     * Returns the checked state.
     * 
     * @return The checked state.
     */
    @Override
    public boolean isChecked() {
        return mChecked;
    }
    
    @Override
    public boolean isCheckable() { 
    	return mCheckable;
    }
    
    public void setCheckable(boolean checkable) { 
    	mCheckable = checkable;
    }
    
    public void setChangeCheckedOnClick(boolean change) { 
    	mChangeCheckedOnClick = change; 
    }
    
    public void setRadio(boolean radio) { 
    	mIsRadio = radio;
    }
    
    public boolean isRadio() { 
    	return mIsRadio;
    }
    
    @Override
    public boolean isEmptyValue() { 
    	return false;
    }
    
    @Override
    public boolean shouldDisableDependents() {
        boolean shouldDisable = mDisableDependentsState ? isChecked() : !isChecked();
        return shouldDisable || super.shouldDisableDependents();
    }

    /**
     * Sets the summary to be shown when checked.
     * 
     * @param summary The summary to be shown when checked.
     */
    public void setSummaryOn(CharSequence summary) {
        mSummaryOn = summary;
        if (isChecked()) {
            notifyChanged();
        }
    }

    /**
     * @see #setSummaryOn(CharSequence)
     * @param summaryResId The summary as a resource.
     */
    public void setSummaryOn(int summaryResId) {
        setSummaryOn(getResourceContext().getString(summaryResId));
    }
    
    /**
     * Returns the summary to be shown when checked.
     * @return The summary.
     */
    public CharSequence getSummaryOn() {
        return mSummaryOn;
    }
    
    /**
     * Sets the summary to be shown when unchecked.
     * 
     * @param summary The summary to be shown when unchecked.
     */
    public void setSummaryOff(CharSequence summary) {
        mSummaryOff = summary;
        if (!isChecked()) {
            notifyChanged();
        }
    }

    /**
     * @see #setSummaryOff(CharSequence)
     * @param summaryResId The summary as a resource.
     */
    public void setSummaryOff(int summaryResId) {
        setSummaryOff(getResourceContext().getString(summaryResId));
    }
    
    /**
     * Returns the summary to be shown when unchecked.
     * @return The summary.
     */
    public CharSequence getSummaryOff() {
        return mSummaryOff;
    }

    /**
     * Returns whether dependents are disabled when this preference is on ({@code true})
     * or when this preference is off ({@code false}).
     * 
     * @return Whether dependents are disabled when this preference is on ({@code true})
     *         or when this preference is off ({@code false}).
     */
    public boolean getDisableDependentsState() {
        return mDisableDependentsState;
    }

    /**
     * Sets whether dependents are disabled when this preference is on ({@code true})
     * or when this preference is off ({@code false}).
     * 
     * @param disableDependentsState The preference state that should disable dependents.
     */
    public void setDisableDependentsState(boolean disableDependentsState) {
        mDisableDependentsState = disableDependentsState;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }
        
        final SavedState myState = new SavedState(superState);
        myState.checked = isChecked();
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
        setChecked(myState.checked);
    }
    
    private static class SavedState extends BaseSavedState {
        boolean checked;
        
        public SavedState(Parcel source) {
            super(source);
            checked = source.readInt() == 1;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(checked ? 1 : 0);
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
    
}
