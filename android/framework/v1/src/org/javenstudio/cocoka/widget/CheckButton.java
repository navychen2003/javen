package org.javenstudio.cocoka.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RadioButton;

public class CheckButton extends RadioButton implements ToolBar.CheckedButton {

	private boolean mCheckable = true; 
	
	public CheckButton(Context context) {
        this(context, null);
    }
    
    public CheckButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CheckButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    public boolean isCheckable() { 
		return mCheckable; 
	} 
	
	public void setCheckable(boolean enable) {
		mCheckable = enable; 
	}
    
	public void setChecked(boolean checked) {
		if (mCheckable) 
			super.setChecked(checked); 
	}
	
	@Override 
	public void setBackgroundResource(int resid) {
		ViewHelper.setBackgroundResource(this, resid); 
	}
	
}
