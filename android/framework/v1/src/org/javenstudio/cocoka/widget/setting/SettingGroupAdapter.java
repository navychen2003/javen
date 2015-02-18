package org.javenstudio.cocoka.widget.setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

/**
 * An adapter that returns the {@link Preference} contained in this group.
 * In most cases, this adapter should be the base class for any custom
 * adapters from {@link Preference#getAdapter()}.
 * <p>
 * This adapter obeys the
 * {@link Preference}'s adapter rule (the
 * {@link Adapter#getView(int, View, ViewGroup)} should be used instead of
 * {@link Preference#getView(ViewGroup)} if a {@link Preference} has an
 * adapter via {@link Preference#getAdapter()}).
 * <p>
 * This adapter also propagates data change/invalidated notifications upward.
 * <p>
 * This adapter does not include this {@link PreferenceGroup} in the returned
 * adapter, use {@link PreferenceCategoryAdapter} instead.
 * 
 * @see PreferenceCategoryAdapter
 */
public class SettingGroupAdapter extends BaseAdapter implements Setting.OnSettingChangeInternalListener {

	/**
     * The group that we are providing data from.
     */
    private SettingGroup mSettingGroup;
    
    /**
     * Maps a position into this adapter -> {@link Setting}. These
     * {@link Setting}s don't have to be direct children of this
     * {@link SettingGroup}, they can be grand children or younger)
     */
    private List<Setting> mSettingList;
    
    /**
     * List of unique Setting and its subclasses' names. This is used to find
     * out how many types of views this adapter can return. Once the count is
     * returned, this cannot be modified (since the ListView only checks the
     * count once--when the adapter is being set). We will not recycle views for
     * Setting subclasses seen after the count has been returned.
     */
    private ArrayList<SettingLayout> mSettingLayouts;

    private SettingLayout mTempSettingLayout = new SettingLayout();

    /**
     * Blocks the mSettingClassNames from being changed anymore.
     */
    private boolean mHasReturnedViewTypeCount = false;
    
    private volatile boolean mIsSyncing = false;
    
    private Handler mHandler = new Handler(); 
    
    private Runnable mSyncRunnable = new Runnable() {
        public void run() {
            syncMySettings();
        }
    };

    private static class SettingLayout implements Comparable<SettingLayout> {
        private int resId;
        private String name;
        private String key;

        public int compareTo(SettingLayout other) {
            int compareNames = name.compareTo(other.name);
            if (compareNames == 0) {
            	if (key != null || other.key != null) { 
            		if (key == null) 
            			return -1; 
            		else if (other.key == null) 
            			return 1;
            		int compareKeys = key.compareTo(other.key); 
            		if (compareKeys != 0) 
            			return compareKeys; 
            	}
                if (resId == other.resId) 
                    return 0;
                else 
                    return resId - other.resId;
            }
        	return compareNames;
        }
    }

    public SettingGroupAdapter(SettingGroup settingGroup) {
        mSettingGroup = settingGroup;
        // If this group gets or loses any children, let us know
        mSettingGroup.setOnSettingChangeInternalListener(this);

        mSettingList = new ArrayList<Setting>();
        mSettingLayouts = new ArrayList<SettingLayout>();

        syncMySettings();
    }

    protected void syncMySettings() {
        synchronized(this) {
            if (mIsSyncing) return;
            mIsSyncing = true;
        }

        List<Setting> newSettingList = new ArrayList<Setting>(mSettingList.size());
        flattenSettingGroup(newSettingList, mSettingGroup);
        mSettingList = newSettingList;
        
        notifyDataSetChanged();

        synchronized(this) {
            mIsSyncing = false;
            notifyAll();
        }
    }
    
    private void flattenSettingGroup(List<Setting> settings, SettingGroup group) {
        // TODO: shouldn't always?
        group.sortSettings();

        final int groupSize = group.getSettingCount();
        for (int i = 0; i < groupSize; i++) {
            final Setting setting = group.getSetting(i);
            final int layoutRes = setting.getSettingItemResource(); 
            
            if (layoutRes != 0) { 
            	if (!setting.isHidden())
            		settings.add(setting);
	            
	            if (!mHasReturnedViewTypeCount) 
	                addSettingClassName(setting, layoutRes);
            }
            
            if (setting instanceof SettingGroup) {
                final SettingGroup settingAsGroup = (SettingGroup) setting;
                if (settingAsGroup.isOnSameScreenAsChildren()) 
                    flattenSettingGroup(settings, settingAsGroup);
            }

            setting.setOnSettingChangeInternalListener(this);
        }
    }

    /**
     * Creates a string that includes the setting name, layout id and widget layout id.
     * If a particular setting type uses 2 different resources, they will be treated as
     * different view types.
     */
    private SettingLayout createSettingLayout(Setting setting, SettingLayout in, int layoutRes) {
        SettingLayout pl = in != null? in : new SettingLayout();
        pl.name = setting.getClass().getName();
        pl.key = setting.getKey();
        pl.resId = layoutRes;
        return pl;
    }

    private SettingLayout createSettingLayout(Setting setting, SettingLayout in) { 
    	return createSettingLayout(setting, in, setting.getSettingItemResource()); 
    }
    
    private void addSettingClassName(Setting setting, int layoutRes) {
        final SettingLayout pl = createSettingLayout(setting, null, layoutRes);
        int insertPos = Collections.binarySearch(mSettingLayouts, pl);

        // Only insert if it doesn't exist (when it is negative).
        if (insertPos < 0) {
            // Convert to insert index
            insertPos = insertPos * -1 - 1;
            mSettingLayouts.add(insertPos, pl);
        }
    }
    
    @Override 
    public int getCount() {
        return mSettingList.size();
    }

    @Override 
    public Setting getItem(int position) {
        return position >= 0 && position < mSettingList.size() ? 
        		mSettingList.get(position) : null;
    }

    @Override 
    public long getItemId(int position) {
        if (position < 0 || position >= getCount()) 
        	return ListView.INVALID_ROW_ID;
        
        Setting item = getItem(position); 
        return item != null ? item.getId() : 0; 
    }

    @Override 
    public View getView(int position, View convertView, ViewGroup parent) {
        final Setting setting = getItem(position);
        if (setting == null) 
        	return null;
        
        // Build a SettingLayout to compare with known ones that are cacheable.
        mTempSettingLayout = createSettingLayout(setting, mTempSettingLayout);

        // If it's not one of the cached ones, set the convertView to null so that 
        // the layout gets re-created by the Setting.
        if (Collections.binarySearch(mSettingLayouts, mTempSettingLayout) < 0) 
            convertView = null;

        return setting.getView(convertView, parent);
    }

    @Override
    public boolean isEnabled(int position) {
        Setting item = getItem(position); 
        return item != null ? item.isSelectable() : true;
    }

    @Override
    public boolean areAllItemsEnabled() {
        // There should always be a setting group, and these groups are always
        // disabled
        return false;
    }

    @Override 
    public void onSettingChange(Setting setting) {
        notifyDataSetChanged();
    }

    @Override 
    public void onSettingHierarchyChange(Setting setting) {
        mHandler.removeCallbacks(mSyncRunnable);
        mHandler.post(mSyncRunnable);
    }

    @Override
    public boolean hasStableIds() {
        return false; 
    }

    @Override
    public int getItemViewType(int position) {
        if (!mHasReturnedViewTypeCount) 
            mHasReturnedViewTypeCount = true;
        
        final Setting setting = this.getItem(position);
        if (setting == null) //setting.hasSpecifiedLayout()) 
            return IGNORE_ITEM_VIEW_TYPE;

        mTempSettingLayout = createSettingLayout(setting, mTempSettingLayout);

        int viewType = Collections.binarySearch(mSettingLayouts, mTempSettingLayout);
        if (viewType < 0) {
            // This is a class that was seen after we returned the count, so
            // don't recycle it.
            return IGNORE_ITEM_VIEW_TYPE;
            
        }
        
        return viewType;
    }

    @Override
    public int getViewTypeCount() {
        if (!mHasReturnedViewTypeCount) 
            mHasReturnedViewTypeCount = true;
        
        return Math.max(1, mSettingLayouts.size());
    }
    
}
