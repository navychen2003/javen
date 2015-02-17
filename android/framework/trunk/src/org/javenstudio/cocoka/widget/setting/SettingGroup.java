package org.javenstudio.cocoka.widget.setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;

import org.javenstudio.common.util.Logger;

/**
 * A container for multiple
 * {@link setting} objects. It is a base class for  setting objects that are
 * parents, such as {@link settingCategory} and {@link settingScreen}.
 * 
 * @attr ref android.R.styleable#settingGroup_orderingFromXml
 */
public class SettingGroup extends Setting implements GenericInflater.Parent<Setting> {
	private static final Logger LOG = Logger.getLogger(SettingGroup.class);
	
	public static interface GroupInitializer { 
		public boolean initialize(SettingGroup group); 
	}
	
	/**
     * The container for child {@link Setting}s. This is sorted based on the
     * ordering, please use {@link #addSetting(Setting)} instead of adding
     * to this directly.
     */
    private final List<Setting> mSettingList;
    private final Map<String, Setting> mSettingMap;

    private boolean mOrderingAsAdded = true;
    private boolean mAttachedToActivity = false;
    private int mCurrentSettingOrder = 0;

	private GroupInitializer mInitializer = null; 
	private boolean mInitialized = false; 
	private int mDisplayCount = -1;
    
    public SettingGroup(SettingManager manager) {
    	this(manager, null); 
    }
    
    public SettingGroup(SettingManager manager, AttributeSet attrs) {
        super(manager, attrs);

        mSettingList = new ArrayList<Setting>();
        mSettingMap = new HashMap<String, Setting>();
    }

    public synchronized void setInitializer(GroupInitializer initializer) { 
    	mInitializer = initializer; 
    	mInitialized = false; 
    }
    
    public synchronized GroupInitializer getInitializer() { 
    	return mInitializer; 
    }
    
    protected synchronized final void requireInitialize() { 
    	if (!mInitialized) { 
    		mInitialized = true; 
    		GroupInitializer initializer = getInitializer(); 
    		if (initializer != null) { 
    			if (LOG.isDebugEnabled()) {
    				LOG.debug("requireInitialize: initialize " + getClass().getName() 
    						+ " with key: " + getKey());
    			}
    			mInitialized = initializer.initialize(this); 
    		}
    	}
    }
    
    private synchronized List<Setting> getSettingList() { 
    	requireInitialize(); 
    	return mSettingList; 
    }
    
    private synchronized Map<String, Setting> getSettingMap() { 
    	requireInitialize(); 
    	return mSettingMap; 
    }
    
    /**
     * Whether to order the {@link Setting} children of this group as they
     * are added. If this is false, the ordering will follow each Setting
     * order and default to alphabetic for those without an order.
     * <p>
     * If this is called after settings are added, they will not be
     * re-ordered in the order they were added, hence call this method early on.
     * 
     * @param orderingAsAdded Whether to order according to the order added.
     * @see Setting#setOrder(int)
     */
    public void setOrderingAsAdded(boolean orderingAsAdded) {
        mOrderingAsAdded = orderingAsAdded;
    }

    /**
     * Whether this group is ordering settings in the order they are added.
     * 
     * @return Whether this group orders based on the order the children are added.
     * @see #setOrderingAsAdded(boolean)
     */
    public boolean isOrderingAsAdded() {
        return mOrderingAsAdded;
    }

    /**
     * Called by the inflater to add an item to this group.
     */
    public void addItemFromInflater(Setting setting, boolean replace) {
        addSetting(setting, replace);
    }

    /**
     * Returns the number of children {@link Setting}s.
     * @return The number of setting children in this group.
     */
    public synchronized int getSettingCount() {
        return getSettingList().size();
    }

    synchronized void setDisplayCountChanged() { 
    	if (mDisplayCount >= 0) { 
    		mDisplayCount = -1;
	    	
	    	List<Setting> list = getSettingList();
	    	for (int i=0; i < list.size(); i++) { 
	    		Setting st = list.get(i);
	    		st.setGroupDisplayIndex(-1);
	    	}
    	}
    }
    
    public synchronized int getSettingDisplayCount() { 
    	int count = mDisplayCount;
    	if (count >= 0) 
    		return count;
    	
    	List<Setting> list = getSettingList();
    	count = 0;
    	for (int i=0; i < list.size(); i++) { 
    		Setting st = list.get(i);
    		if (!st.isHidden()) 
    			count ++;
    	}
    	
    	mDisplayCount = count;
    	
    	return count;
    }
    
    public synchronized int getSettingDisplayIndex(Setting setting) { 
    	if (setting == null) return -1;
    	
    	int displayIndex = setting.getGroupDisplayIndex();
    	if (displayIndex >= 0) 
    		return displayIndex;
    	
    	getSettingDisplayCount();
    	
    	List<Setting> list = getSettingList();
    	int index = 0;
    	for (int i=0; i < list.size(); i++) { 
    		Setting st = list.get(i);
    		if (!st.isHidden()) {
	    		if (st == setting) { 
	    			setting.setGroupDisplayIndex(index);
	    			return index;
	    		}
	    		
	    		index ++;
    		}
    	}
    	
    	return -1;
    }
    
    /**
     * Returns the {@link Setting} at a particular index.
     * 
     * @param index The index of the {@link Setting} to retrieve.
     * @return The {@link Setting}.
     */
    public synchronized Setting getSetting(int index) {
        return getSettingList().get(index);
    }

    private synchronized boolean containsSetting(Setting setting) { 
    	final String key = setting.getKey(); 
    	
    	//for (Setting item : getSettingList()) { 
    	//	if (setting == item) 
    	//		return true;
    	//	if (key != null && key.equals(item.getKey())) 
    	//		return true;
    	//}
    	
    	if (getSettingList().contains(setting)) 
    		return true;
    	
    	if (key != null && getSettingMap().containsKey(key)) 
    		return true;
    	
    	return false;
    }
    
    /**
     * Adds a {@link Setting} at the correct position based on the
     * setting's order.
     * 
     * @param setting The setting to add.
     * @return Whether the setting is now in this group.
     */
    public boolean addSetting(Setting setting) {
    	return addSetting(setting, false);
    }
    
    public boolean addSetting(Setting setting, boolean replace) {
    	if (setting == null) return false; 
    	
        if (containsSetting(setting)) {
        	if (LOG.isDebugEnabled())
            	LOG.debug("addSetting: setting: " + setting + " already existed");
        	
        	if (!replace)
        		return true;
        	
        	removeSetting(setting);
        }
        
        if (setting.getOrder() == Setting.DEFAULT_ORDER) {
            if (mOrderingAsAdded) 
                setting.setOrder(mCurrentSettingOrder++);

            if (setting instanceof SettingGroup) {
                // TODO: fix (method is called tail recursively when inflating,
                // so we won't end up properly passing this flag down to children
                ((SettingGroup)setting).setOrderingAsAdded(mOrderingAsAdded);
            }
        }

        int insertionIndex = Collections.binarySearch(getSettingList(), setting);
        if (insertionIndex < 0) 
            insertionIndex = insertionIndex * -1 - 1;

        if (!onPrepareAddSetting(setting)) 
            return false;

        synchronized(this) {
        	setting.setParent(this); 
        	setting.setGroupIndex(insertionIndex); 
        	
            getSettingList().add(insertionIndex, setting);
            
            String key = setting.getKey();
            if (key != null)
            	getSettingMap().put(key, setting);
        }

        setting.onAttachedToHierarchy();
        
        if (mAttachedToActivity) 
            setting.onAttachedToActivity();
        
        notifyHierarchyChanged();
        setDisplayCountChanged();

        return true;
    }

    /**
     * Removes a {@link Setting} from this group.
     * 
     * @param setting The setting to remove.
     * @return Whether the setting was found and removed.
     */
    public boolean removeSetting(Setting setting) {
    	if (LOG.isDebugEnabled()) LOG.debug("removeSetting: setting=" + setting);
    	
        final boolean returnValue = removeSettingInternal(setting);
        notifyHierarchyChanged();
        
        return returnValue;
    }

    protected boolean removeSettingInternal(Setting setting) {
    	if (setting == null) return false;
    	
        synchronized(this) {
        	setDisplayCountChanged();
            setting.onPrepareForRemoval();
            
            String key = setting.getKey();
            boolean result1 = false;
            if (key != null) {
            	Setting removed = getSettingMap().remove(key);
            	if (removed != null) {
            		result1 = getSettingList().remove(removed);
            		
            		if (LOG.isDebugEnabled())
            			LOG.debug("removeSettingInternal: removed from map: " + removed);
            	}
            }
            
            boolean result2 = getSettingList().remove(setting);
            if (result2) {
            	if (LOG.isDebugEnabled())
        			LOG.debug("removeSettingInternal: removed from list: " + setting);
            }
            
            return result1 || result2;
        }
    }
    
    /**
     * Removes all {@link Setting Settings} from this group.
     */
    public void removeAll(boolean notifyChange) {
        synchronized(this) {
            List<Setting> settingList = getSettingList();
            for (int i = settingList.size() - 1; i >= 0; i--) {
            	removeSettingInternal(settingList.get(0));
            }
            getSettingList().clear();
            getSettingMap().clear();
            //mInitialized = false; 
        }
        
        if (notifyChange) notifyHierarchyChanged();
    }
    
    /**
     * Prepares a {@link Setting} to be added to the group.
     * 
     * @param setting The setting to add.
     * @return Whether to allow adding the setting (true), or not (false).
     */
    protected boolean onPrepareAddSetting(Setting setting) {
        if (!super.isEnabled()) 
            setting.setEnabled(false);
        
        return true;
    }

    /**
     * Finds a {@link Setting} based on its key. If two {@link Setting}
     * share the same key (not recommended), the first to appear will be
     * returned (to retrieve the other setting with the same key, call this
     * method on the first setting). If this setting has the key, it will
     * not be returned.
     * <p>
     * This will recursively search for the setting into children that are
     * also {@link SettingGroup SettingGroups}.
     * 
     * @param key The key of the setting to retrieve.
     * @return The {@link Setting} with the key, or null.
     */
    public Setting findSetting(String key) {
    	if (key == null) return null;
    	
        if (TextUtils.equals(getKey(), key)) 
            return this;
        
        synchronized (this) { 
        	Setting foundSetting = getSettingMap().get(key);
        	if (foundSetting != null) 
        		return foundSetting;
        }
        
        final int settingCount = getSettingCount();
        for (int i = 0; i < settingCount; i++) {
            final Setting setting = getSetting(i);
            if (setting == null) continue;

            if (setting instanceof SettingGroup) {
                final Setting foundSetting = ((SettingGroup)setting).findSetting(key);
                if (foundSetting != null) 
                    return foundSetting;
            }
        }
        
        return null;
    }
    
    protected Setting findSettingDeprecated(CharSequence key) {
    	if (key == null) return null;
    	
        if (TextUtils.equals(getKey(), key)) 
            return this;
        
        final int settingCount = getSettingCount();
        for (int i = 0; i < settingCount; i++) {
            final Setting setting = getSetting(i);
            final String curKey = setting.getKey();

            if (curKey != null && curKey.equals(key)) 
                return setting;
            
            if (setting instanceof SettingGroup) {
                final Setting returnedSetting = ((SettingGroup)setting).findSettingDeprecated(key);
                if (returnedSetting != null) 
                    return returnedSetting;
            }
        }

        return null;
    }

    /**
     * Whether this setting group should be shown on the same screen as its
     * contained settings.
     * 
     * @return True if the contained settings should be shown on the same
     *         screen as this setting.
     */
    protected boolean isOnSameScreenAsChildren() {
        return true;
    }
    
    @Override
    protected void onAttachedToActivity() {
        super.onAttachedToActivity();

        // Mark as attached so if a setting is later added to this group, we
        // can tell it we are already attached
        mAttachedToActivity = true;
        
        // Dispatch to all contained settings
        final int settingCount = getSettingCount();
        for (int i = 0; i < settingCount; i++) {
            getSetting(i).onAttachedToActivity();
        }
    }

    @Override
    protected void onPrepareForRemoval() {
        super.onPrepareForRemoval();
        
        // We won't be attached to the activity anymore
        mAttachedToActivity = false;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        
        // Dispatch to all contained settings
        final int settingCount = getSettingCount();
        for (int i = 0; i < settingCount; i++) {
            getSetting(i).setEnabled(enabled);
        }
    }
    
    protected void sortSettings() {
        synchronized (this) {
            Collections.sort(getSettingList());
        }
    }

    @Override
    protected void dispatchSaveInstanceState(Bundle container) {
        super.dispatchSaveInstanceState(container);

        // Dispatch to all contained settings
        final int settingCount = getSettingCount();
        for (int i = 0; i < settingCount; i++) {
            getSetting(i).dispatchSaveInstanceState(container);
        }
    }
    
    @Override
    protected void dispatchRestoreInstanceState(Bundle container) {
        super.dispatchRestoreInstanceState(container);

        // Dispatch to all contained settings
        final int settingCount = getSettingCount();
        for (int i = 0; i < settingCount; i++) {
            getSetting(i).dispatchRestoreInstanceState(container);
        }
    }
    
}
