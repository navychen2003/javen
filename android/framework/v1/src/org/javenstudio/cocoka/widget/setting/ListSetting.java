package org.javenstudio.cocoka.widget.setting;

import java.util.ArrayList;
import java.util.List;

import android.text.TextUtils;
import android.util.AttributeSet;

public class ListSetting extends SettingScreen {

	private final SettingCategory mCategory; 
	private CharSequence[] mCheckedValues = null; 
	private boolean mMultiCheckable = false;
	
    public ListSetting(SettingManager manager) {
    	this(manager, null); 
    }
    
    public ListSetting(SettingManager manager, AttributeSet attrs) {
        super(manager, attrs); 
        super.addSetting(mCategory = createCategory());
    }
	
    protected SettingCategory createCategory() { 
    	return new SettingCategory(getSettingManager());
    }
    
    public final SettingCategory getCategory() { 
    	return mCategory;
    }
    
    @Override
    protected final boolean removeSettingInternal(Setting setting) {
    	return false; // disable
    }
    
    @Override
    public final boolean addSetting(Setting setting) { 
    	return false; //addItem(setting);
    }
    
    protected boolean addItem(ListItemSetting setting) { 
    	return getCategory().addSetting(setting); 
    }
    
    protected ListItemSetting createItem(int index, CharSequence entry, CharSequence entryValue) { 
    	ListItemSetting item = new ListItemSetting(this); 
    	item.setKey(getKey() + ".entry_" + index); 
    	item.setEntry(entry); 
    	item.setEntryValue(entryValue); 
    	
    	return item;
    }
    
    public final int setEntries(int entriesRes, int entryValuesRes) { 
    	CharSequence[] entries = getResourceContext().getStringArray(entriesRes); 
    	CharSequence[] entryValues = getResourceContext().getStringArray(entryValuesRes); 
    	return setEntries(entries, entryValues); 
    }
    
    public final int setEntries(CharSequence[] entries, CharSequence[] entryValues) { 
    	int count = 0;
    	if (entries != null && entryValues != null && entries.length == entryValues.length) { 
    		for (int i=0; i < entries.length && i < entryValues.length; i++) { 
    			CharSequence entry = entries[i]; 
    			CharSequence entryValue = entryValues[i]; 
    			if (entry == null || entryValue == null) 
    				continue;
    			
    			if (addItem(createItem(i, entry, entryValue))) 
    				count ++;
    		}
    	}
    	return count;
    }
    
    @Override
    public boolean isEmptyValue() { 
    	return TextUtils.isEmpty(getCheckedValue());
    }
    
    public final void setMultiCheckable(boolean multi) { 
    	mMultiCheckable = multi;
    }
    
    public final boolean isMultiCheckable() { 
    	return mMultiCheckable;
    }
    
    public final void setCheckedValue(int valueRes) { 
    	setCheckedValue(getResourceContext().getString(valueRes));
    }
    
    public synchronized final void setCheckedValue(CharSequence value) { 
    	if (value != null)
    		mCheckedValues = new CharSequence[] {value};
    	else 
    		mCheckedValues = null;
    	
    	notifyDependencyChange(shouldDisableDependents());
    	onCheckedValuesChanged(); 
    }
    
    public synchronized final CharSequence getCheckedValue() { 
    	CharSequence[] values = mCheckedValues; 
    	return values != null && values.length > 0 ? values[0] : null;
    }
    
    public final void setCheckedValues(int valuesRes) { 
    	setCheckedValues(getResourceContext().getStringArray(valuesRes));
    }
    
    public synchronized final void setCheckedValues(CharSequence[] values) { 
    	if (isMultiCheckable()) { 
    		CharSequence value = values != null && values.length > 0 ? values[0] : null; 
    		setCheckedValue(value); 
    		return;
    	}
    	
    	mCheckedValues = values; 
    	
    	notifyDependencyChange(shouldDisableDependents());
    	onCheckedValuesChanged(); 
    }
    
    public synchronized final CharSequence[] getCheckedValues() { 
    	return mCheckedValues; 
    }
    
    protected void onCheckedValuesChanged() { 
    	ListItemSetting item = getCheckedItem();
		setSummary(item != null ? item.getTitle() : null);
    }
    
    @Override
    protected void onChangeValue(Object newValue) { 
		if (newValue != null && newValue instanceof CharSequence) 
			setCheckedValue((CharSequence)newValue); 
		else if (newValue != null && newValue instanceof CharSequence[]) 
			setCheckedValues((CharSequence[])newValue);
		else 
			setCheckedValue(null);
    }
    
    protected synchronized void changeValueAppend(CharSequence newValue) { 
    	if (newValue == null) return;
    	
    	CharSequence[] values = getCheckedValues(); 
    	
    	List<CharSequence> list = new ArrayList<CharSequence>(); 
    	boolean found = false;
    	
    	for (int i=0; values != null && i < values.length; i++) { 
    		CharSequence value = values[i]; 
    		if (value == null) continue;
    		if (newValue.equals(value)) 
    			found = true; 
    		list.add(value);
    	}
    	
    	if (!found) { 
    		if (!isMultiCheckable()) 
    			list.clear();
    		list.add(newValue); 
    		changeValue(list.toArray(new CharSequence[list.size()]));
    	}
    }
    
    protected synchronized void changeValueRemove(CharSequence removeValue) { 
    	if (removeValue == null) return; 
    	
    	CharSequence[] values = getCheckedValues(); 
    	if (values == null || values.length == 0) 
    		return;
    	
    	if (values.length == 1) { 
    		if (removeValue.equals(values[0])) 
    			setCheckedValues(null); 
    		return;
    	}
    	
    	List<CharSequence> list = new ArrayList<CharSequence>(); 
    	boolean found = false;
    	
    	for (int i=0; values != null && i < values.length; i++) { 
    		CharSequence value = values[i]; 
    		if (value == null) continue;
    		if (removeValue.equals(value)) 
    			found = true; 
    		else 
    			list.add(value);
    	}
    	
    	if (found) 
    		changeValue(list.toArray(new CharSequence[list.size()]));
    }
    
    public final ListItemSetting getCheckedItem() { 
    	for (int i=0; i < getSettingCount(); i++) { 
    		ListItemSetting item = lookupCheckedItem(getSetting(i)); 
    		if (item != null) 
    			return item;
    	}
    	return null;
    }
    
    public final ListItemSetting[] getCheckedItems() { 
    	List<ListItemSetting> items = new ArrayList<ListItemSetting>(); 
    	for (int i=0; i < getSettingCount(); i++) { 
    		lookupCheckedItems(items, getSetting(i)); 
    	}
    	return items.toArray(new ListItemSetting[items.size()]);
    }
    
    protected ListItemSetting lookupCheckedItem(Setting setting) { 
    	if (setting == null) return null; 
    	
    	if (setting instanceof SettingGroup) { 
    		SettingGroup group = (SettingGroup)setting;
    		for (int i=0; i < group.getSettingCount(); i++) { 
    			Setting item = group.getSetting(i); 
    			ListItemSetting listItem = lookupCheckedItem(item);
    			if (listItem != null) 
    				return listItem;
    		}
    		
    	} else if (setting instanceof ListItemSetting) { 
    		ListItemSetting item = (ListItemSetting)setting;
    		if (item.isChecked()) 
    			return item;
    	}
    	
    	return null;
    }
    
    protected void lookupCheckedItems(List<ListItemSetting> items, Setting setting) { 
    	if (setting == null) return; 
    	
    	if (setting instanceof SettingGroup) { 
    		SettingGroup group = (SettingGroup)setting;
    		for (int i=0; i < group.getSettingCount(); i++) { 
    			Setting item = group.getSetting(i); 
    			lookupCheckedItems(items, item);
    		}
    		return;
    		
    	} else if (setting instanceof ListItemSetting) { 
    		ListItemSetting item = (ListItemSetting)setting;
    		if (item.isChecked()) 
    			items.add(item);
    	}
    }
    
    protected boolean isListItemChecked(ListItemSetting item) { 
    	CharSequence[] values = getCheckedValues();
    	if (item != null && values != null && values.length > 0) { 
    		CharSequence itemValue = item.getEntryValue(); 
    		if (itemValue != null) { 
    			for (int i=0; i < values.length; i++) { 
    				CharSequence value = values[i]; 
    				if (value != null && value.equals(itemValue)) 
    					return true;
    			}
    		}
    	}
    	return false;
    }
    
    protected void setListItemChecked(ListItemSetting item, boolean newChecked) { 
    	if (item == null) return;
    	
    	boolean oldChecked = item.isChecked(); 
    	if (oldChecked == newChecked) 
    		return;
    	
    	if (newChecked) 
    		changeValueAppend(item.getEntryValue());
    	else 
    		changeValueRemove(item.getEntryValue());
    	
    	item.notifyChanged();
    }
    
}
