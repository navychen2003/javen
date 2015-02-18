package org.javenstudio.cocoka.widget.setting;

import android.content.Context;
import android.util.AttributeSet;

import org.javenstudio.cocoka.widget.ImageButton;

public class CheckBoxButton extends ImageButton {

	public static interface CheckBoxAdapter { 
		public boolean isCheckable(); 
		public boolean isChecked(); 
	}
	
	private CheckBoxAdapter mAdapter = null; 
	
	public CheckBoxButton(Context context) {
		super(context); 
	}
	
	public CheckBoxButton(Context context, AttributeSet attrs) {
		super(context, attrs); 
	}
	
	public CheckBoxButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle); 
	}
	
	public final void setAdapter(CheckBoxAdapter adapter) { 
		mAdapter = adapter;
	}
	
	@Override 
	public boolean isCheckable() { 
		CheckBoxAdapter adapter = mAdapter; 
		return adapter != null ? adapter.isCheckable() : false; 
	}
	
	@Override 
	public boolean isChecked() { 
		CheckBoxAdapter adapter = mAdapter; 
		return adapter != null ? adapter.isChecked() : false; 
	}
	
	@Override 
	public void setChecked(boolean checked) {
		// disable
	}
	@Override
    public int[] onCreateDrawableState(int extraSpace) {
		if (isCheckable() && isChecked()) 
			return CHECKED_STATE_SET; 
		return EMPTY_STATE_SET; 
	}
	
}
