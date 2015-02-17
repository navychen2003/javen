package org.javenstudio.cocoka.widget.setting;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

/**
 * Used to group {@link Preference} objects
 * and provide a disabled title above the group.
 */
public class SettingCategory extends SettingGroup {

    public SettingCategory(SettingManager manager) {
    	this(manager, null); 
    }
	
	public SettingCategory(SettingManager manager, AttributeSet attrs) {
        super(manager, attrs);
    }

    @Override
    protected boolean onPrepareAddSetting(Setting setting) {
        if (setting instanceof SettingCategory) {
            throw new IllegalArgumentException(
                    "Cannot add a Setting directly to a SettingCategory");
        }
        
        return super.onPrepareAddSetting(setting);
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
    
    @Override
    public boolean isSelectable() {
        return false;
    }
    
    public Setting createSetting(String key, int titleRes) { 
    	return createSetting(key, titleRes, 0); 
    }
    
    public Setting createSetting(String key, int titleRes, int summaryRes) { 
    	return createSetting(key, titleRes, summaryRes, 0); 
    }
    
    public Setting createSetting(String key, int titleRes, int summaryRes, int iconRes) { 
    	return createSetting(key, titleRes, summaryRes, iconRes, null); 
    }
    
    public Setting createSetting(String key, int titleRes, int summaryRes, int iconRes, Intent intent) { 
    	Setting setting = new Setting(getSettingManager()); 
    	initSetting(setting, key, titleRes, summaryRes, iconRes, intent); 
    	return setting; 
    }
    
    public Setting createSetting(String key, CharSequence title) { 
    	return createSetting(key, title, null, null, null); 
    }
    
    public Setting createSetting(String key, CharSequence title, CharSequence summary) { 
    	return createSetting(key, title, summary, null, null); 
    }
    
    public Setting createSetting(String key, CharSequence title, CharSequence summary, Drawable icon) { 
    	return createSetting(key, title, summary, icon, null); 
    }
    
    public Setting createSetting(String key, CharSequence title, CharSequence summary, Drawable icon, Intent intent) { 
    	Setting setting = new Setting(getSettingManager()); 
    	initSetting(setting, key, title, summary, icon, intent); 
    	return setting; 
    }
    
    
    public CheckBoxSetting createCheckBoxSetting(String key, int titleRes) { 
    	return createCheckBoxSetting(key, titleRes, 0); 
    }
    
    public CheckBoxSetting createCheckBoxSetting(String key, int titleRes, int summaryRes) { 
    	return createCheckBoxSetting(key, titleRes, summaryRes, 0); 
    }
    
    public CheckBoxSetting createCheckBoxSetting(String key, int titleRes, int summaryRes, int iconRes) { 
    	return createCheckBoxSetting(key, titleRes, summaryRes, iconRes, null); 
    }
    
    public CheckBoxSetting createCheckBoxSetting(String key, int titleRes, int summaryRes, int iconRes, Intent intent) { 
    	CheckBoxSetting setting = new CheckBoxSetting(getSettingManager()); 
    	initSetting(setting, key, titleRes, summaryRes, iconRes, intent); 
    	return setting; 
    }
    
    public CheckBoxSetting createCheckBoxSetting(String key, CharSequence title) { 
    	return createCheckBoxSetting(key, title, null, null, null); 
    }
    
    public CheckBoxSetting createCheckBoxSetting(String key, CharSequence title, CharSequence summary) { 
    	return createCheckBoxSetting(key, title, summary, null, null); 
    }
    
    public CheckBoxSetting createCheckBoxSetting(String key, CharSequence title, CharSequence summary, Drawable icon) { 
    	return createCheckBoxSetting(key, title, summary, icon, null); 
    }
    
    public CheckBoxSetting createCheckBoxSetting(String key, CharSequence title, CharSequence summary, Drawable icon, Intent intent) { 
    	CheckBoxSetting setting = new CheckBoxSetting(getSettingManager()); 
    	initSetting(setting, key, title, summary, icon, intent); 
    	return setting; 
    }
    
    
    public EditTextSetting createEditTextSetting(String key, int titleRes) { 
    	return createEditTextSetting(key, titleRes, 0); 
    }
    
    public EditTextSetting createEditTextSetting(String key, int titleRes, int summaryRes) { 
    	return createEditTextSetting(key, titleRes, summaryRes, 0); 
    }
    
    public EditTextSetting createEditTextSetting(String key, int titleRes, int summaryRes, int iconRes) { 
    	return createEditTextSetting(key, titleRes, summaryRes, iconRes, null); 
    }
    
    public EditTextSetting createEditTextSetting(String key, int titleRes, int summaryRes, int iconRes, Intent intent) { 
    	EditTextSetting setting = new EditTextSetting(getSettingManager()); 
    	initSetting(setting, key, titleRes, summaryRes, iconRes, intent); 
    	return setting; 
    }
    
    public EditTextSetting createEditTextSetting(String key, CharSequence title) { 
    	return createEditTextSetting(key, title, null, null, null); 
    }
    
    public EditTextSetting createEditTextSetting(String key, CharSequence title, CharSequence summary) { 
    	return createEditTextSetting(key, title, summary, null, null); 
    }
    
    public EditTextSetting createEditTextSetting(String key, CharSequence title, CharSequence summary, Drawable icon) { 
    	return createEditTextSetting(key, title, summary, icon, null); 
    }
    
    public EditTextSetting createEditTextSetting(String key, CharSequence title, CharSequence summary, Drawable icon, Intent intent) { 
    	EditTextSetting setting = new EditTextSetting(getSettingManager()); 
    	initSetting(setting, key, title, summary, icon, intent); 
    	return setting; 
    }
    
    
    public ActionSetting createActionSetting(String key, int titleRes) { 
    	return createActionSetting(key, titleRes, 0); 
    }
    
    public ActionSetting createActionSetting(String key, int titleRes, int summaryRes) { 
    	return createActionSetting(key, titleRes, summaryRes, 0); 
    }
    
    public ActionSetting createActionSetting(String key, int titleRes, int summaryRes, int iconRes) { 
    	return createActionSetting(key, titleRes, summaryRes, iconRes, null); 
    }
    
    public ActionSetting createActionSetting(String key, int titleRes, int summaryRes, int iconRes, Intent intent) { 
    	ActionSetting setting = new ActionSetting(getSettingManager()); 
    	initSetting(setting, key, titleRes, summaryRes, iconRes, intent); 
    	return setting; 
    }
    
    public ActionSetting createActionSetting(String key, CharSequence title) { 
    	return createActionSetting(key, title, null, null, null); 
    }
    
    public ActionSetting createActionSetting(String key, CharSequence title, CharSequence summary) { 
    	return createActionSetting(key, title, summary, null, null); 
    }
    
    public ActionSetting createActionSetting(String key, CharSequence title, CharSequence summary, Drawable icon) { 
    	return createActionSetting(key, title, summary, icon, null); 
    }
    
    public ActionSetting createActionSetting(String key, CharSequence title, CharSequence summary, Drawable icon, Intent intent) { 
    	ActionSetting setting = new ActionSetting(getSettingManager()); 
    	initSetting(setting, key, title, summary, icon, intent); 
    	return setting; 
    }
    
    
    public RingtoneSetting createRingtoneSetting(String key, int titleRes) { 
    	return createRingtoneSetting(key, titleRes, 0); 
    }
    
    public RingtoneSetting createRingtoneSetting(String key, int titleRes, int summaryRes) { 
    	return createRingtoneSetting(key, titleRes, summaryRes, 0); 
    }
    
    public RingtoneSetting createRingtoneSetting(String key, int titleRes, int summaryRes, int iconRes) { 
    	return createRingtoneSetting(key, titleRes, summaryRes, iconRes, null); 
    }
    
    public RingtoneSetting createRingtoneSetting(String key, int titleRes, int summaryRes, int iconRes, Intent intent) { 
    	RingtoneSetting setting = new RingtoneSetting(getSettingManager()); 
    	initSetting(setting, key, titleRes, summaryRes, iconRes, intent); 
    	return setting; 
    }
    
    public RingtoneSetting createRingtoneSetting(String key, CharSequence title) { 
    	return createRingtoneSetting(key, title, null, null, null); 
    }
    
    public RingtoneSetting createRingtoneSetting(String key, CharSequence title, CharSequence summary) { 
    	return createRingtoneSetting(key, title, summary, null, null); 
    }
    
    public RingtoneSetting createRingtoneSetting(String key, CharSequence title, CharSequence summary, Drawable icon) { 
    	return createRingtoneSetting(key, title, summary, icon, null); 
    }
    
    public RingtoneSetting createRingtoneSetting(String key, CharSequence title, CharSequence summary, Drawable icon, Intent intent) { 
    	RingtoneSetting setting = new RingtoneSetting(getSettingManager()); 
    	initSetting(setting, key, title, summary, icon, intent); 
    	return setting; 
    }
    
    
    public ListSetting createListSetting(String key, int titleRes) { 
    	return createListSetting(key, titleRes, 0); 
    }
    
    public ListSetting createListSetting(String key, int titleRes, int summaryRes) { 
    	return createListSetting(key, titleRes, summaryRes, 0); 
    }
    
    public ListSetting createListSetting(String key, int titleRes, int summaryRes, int iconRes) { 
    	return createListSetting(key, titleRes, summaryRes, iconRes, null); 
    }
    
    public ListSetting createListSetting(String key, int titleRes, int summaryRes, int iconRes, Intent intent) { 
    	ListSetting setting = new ListSetting(getSettingManager()); 
    	initSetting(setting, key, titleRes, summaryRes, iconRes, intent); 
    	return setting; 
    }
    
    public ListSetting createListSetting(String key, CharSequence title) { 
    	return createListSetting(key, title, null, null, null); 
    }
    
    public ListSetting createListSetting(String key, CharSequence title, CharSequence summary) { 
    	return createListSetting(key, title, summary, null, null); 
    }
    
    public ListSetting createListSetting(String key, CharSequence title, CharSequence summary, Drawable icon) { 
    	return createListSetting(key, title, summary, icon, null); 
    }
    
    public ListSetting createListSetting(String key, CharSequence title, CharSequence summary, Drawable icon, Intent intent) { 
    	ListSetting setting = new ListSetting(getSettingManager()); 
    	initSetting(setting, key, title, summary, icon, intent); 
    	return setting; 
    }
    
    
    public SettingScreen createSettingScreen(String key, int titleRes) { 
    	return createSettingScreen(key, titleRes, 0); 
    }
    
    public SettingScreen createSettingScreen(String key, int titleRes, int summaryRes) { 
    	return createSettingScreen(key, titleRes, summaryRes, 0); 
    }
    
    public SettingScreen createSettingScreen(String key, int titleRes, int summaryRes, int iconRes) { 
    	return createSettingScreen(key, titleRes, summaryRes, iconRes, null); 
    }
    
    public SettingScreen createSettingScreen(String key, int titleRes, int summaryRes, int iconRes, Intent intent) { 
    	SettingScreen setting = new SettingScreen(getSettingManager()); 
    	initSetting(setting, key, titleRes, summaryRes, iconRes, intent); 
    	return setting; 
    }
    
    public SettingScreen createSettingScreen(String key, CharSequence title) { 
    	return createSettingScreen(key, title, null, null, null); 
    }
    
    public SettingScreen createSettingScreen(String key, CharSequence title, CharSequence summary) { 
    	return createSettingScreen(key, title, summary, null, null); 
    }
    
    public SettingScreen createSettingScreen(String key, CharSequence title, CharSequence summary, Drawable icon) { 
    	return createSettingScreen(key, title, summary, icon, null); 
    }
    
    public SettingScreen createSettingScreen(String key, CharSequence title, CharSequence summary, Drawable icon, Intent intent) { 
    	SettingScreen setting = new SettingScreen(getSettingManager()); 
    	initSetting(setting, key, title, summary, icon, intent); 
    	return setting; 
    }
    
    
    private void initSetting(Setting setting, String key, int titleRes, int summaryRes, int iconRes, Intent intent) { 
    	if (setting != null) { 
	    	setting.setKey(key); 
	    	setting.setIntent(intent); 
	    	
	    	if (titleRes != 0) setting.setTitle(titleRes); 
	    	if (summaryRes != 0) setting.setSummary(summaryRes); 
	    	if (iconRes != 0) setting.setIcon(iconRes); 
    	}
    }
    
    private void initSetting(Setting setting, String key, CharSequence title, CharSequence summary, Drawable icon, Intent intent) { 
    	if (setting != null) { 
	    	setting.setKey(key); 
	    	setting.setTitle(title); 
	    	setting.setSummary(summary); 
	    	setting.setIcon(icon); 
	    	setting.setIntent(intent); 
    	}
    }
    
}
