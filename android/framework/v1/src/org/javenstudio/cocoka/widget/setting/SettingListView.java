package org.javenstudio.cocoka.widget.setting;

import android.content.Context;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ListView;

import org.javenstudio.cocoka.widget.ViewHelper;

public class SettingListView extends ListView {

	public SettingListView(Context context) {
		super(context); 
		initViews(); 
	}
	
	public SettingListView(Context context, AttributeSet attrs) {
		super(context, attrs); 
		initViews(); 
	}
	
	public SettingListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle); 
		initViews(); 
	}
	
	protected void initViews() {
		super.setChoiceMode(ListView.CHOICE_MODE_NONE);
	}
	
	@Override 
	public void setBackgroundResource(int resid) {
		ViewHelper.setBackgroundResource(this, resid); 
	}
	
	@Override 
	public int getChoiceMode() {
		return ListView.CHOICE_MODE_NONE;
	}
	
	@Override 
	public void setChoiceMode(int choiceMode) {
		super.setChoiceMode(ListView.CHOICE_MODE_NONE);
	}
	
	@Override
    public boolean performItemClick(View view, int position, long id) {
		boolean result = super.performItemClick(view, position, id); 
		clearChoices(); 
		
		return result; 
	}
	
	@Override
	public void setItemChecked(int position, boolean value) {
		// disable
	}
	
	@Override
	public boolean isItemChecked(int position) {
		return false;
	}
	
	@Override
	public int getCheckedItemPosition() {
		return INVALID_POSITION;
	}
	
	@Override
	public SparseBooleanArray getCheckedItemPositions() {
		return null;
	}
	
	@Override
	public long[] getCheckItemIds() {
		return new long[0];
	}
	
	//@Override
	public long[] getCheckedItemIds() {
		return new long[0];
	}
	
}
