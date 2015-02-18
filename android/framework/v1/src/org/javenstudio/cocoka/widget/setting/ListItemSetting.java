package org.javenstudio.cocoka.widget.setting;

import android.util.AttributeSet;

public class ListItemSetting extends Setting {

	private final ListSetting mListSetting; 
	private CharSequence mEntryValue = null;
	
	public ListItemSetting(ListSetting list) {
    	this(list, null); 
    }
    
    public ListItemSetting(ListSetting list, AttributeSet attrs) {
        super(list.getSettingManager(), attrs); 
        mListSetting = list;
    }
	
    public final ListSetting getListSetting() { 
    	return mListSetting;
    }
    
    public void setEntry(CharSequence entry) { 
    	setTitle(entry);
    }
    
    public CharSequence getEntry() { 
    	return getTitle(); 
    }
    
    public void setEntryValue(CharSequence entryValue) { 
    	mEntryValue = entryValue; 
    }
    
    public CharSequence getEntryValue() { 
    	return mEntryValue;
    }
    
    public boolean isChecked() { 
    	return getListSetting().isListItemChecked(this);
    }
    
    @Override
    protected void onClick() {
    	getListSetting().setListItemChecked(this, true);
    }
    
}
