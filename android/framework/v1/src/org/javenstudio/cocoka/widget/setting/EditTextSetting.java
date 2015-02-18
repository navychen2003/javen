package org.javenstudio.cocoka.widget.setting;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

public class EditTextSetting extends Setting {

	public static interface OnTextChangeListener extends TextWatcher { 
		public void afterTextChanged(Editable s); 
		public void beforeTextChanged(CharSequence s, int start, int count, int after); 
		public void onTextChanged(CharSequence s, int start, int before, int count); 
	}
	
	public static interface EditTextBinder { 
		public boolean bindEditText(EditTextSetting setting, EditText view); 
	}
	
	private OnTextChangeListener mOnTextChangeListener = null; 
	private EditTextBinder mEditTextBinder = null; 
	private String mText = null;
	private int mInputType = 0;
	
	public EditTextSetting(SettingManager manager) {
    	super(manager); 
    }
    
    public EditTextSetting(SettingManager manager, AttributeSet attrs) {
        super(manager, attrs);
    }
	
    public void setOnTextChangeListener(OnTextChangeListener listener) { 
    	mOnTextChangeListener = listener;
    }
    
    public OnTextChangeListener getOnTextChangeListener() { 
    	return mOnTextChangeListener;
    }
    
    public void setEditTextBinder(EditTextBinder binder) { 
    	mEditTextBinder = binder;
    }
    
    public EditTextBinder getEditTextBinder() { 
    	return mEditTextBinder;
    }
    
    public int getInputType() { 
    	return mInputType; 
    }
    
    public void setInputType(int type) { 
    	mInputType = type; 
    }
    
    @Override
    public boolean isEmptyValue() { 
    	return TextUtils.isEmpty(getText());
    }
    
    @Override 
    public View getView(View convertView, ViewGroup parent) {
    	return super.getView(null, parent); // not cache view
    }
    
    /**
     * Gets the text from the {@link SharedPreferences}.
     * 
     * @return The current preference value.
     */
    public String getText() {
        return mText;
    }
    
    /**
     * Saves the text to the {@link SharedPreferences}.
     * 
     * @param text The text to save
     */
    public void setText(String text) {
        final boolean wasBlocking = shouldDisableDependents();
        
        mText = text;
        
        persistString(text);
        
        final boolean isBlocking = shouldDisableDependents(); 
        if (isBlocking != wasBlocking) {
            notifyDependencyChange(isBlocking);
        }
    }

    @Override
    protected void onChangeValue(Object newValue) { 
    	setText(newValue != null ? newValue.toString() : null);
    }
    
    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }
        
        final SavedState myState = new SavedState(superState);
        myState.text = getText();
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
        setText(myState.text);
    }
    
    private static class SavedState extends BaseSavedState {
        String text;
        
        public SavedState(Parcel source) {
            super(source);
            text = source.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(text);
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
