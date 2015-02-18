package org.javenstudio.cocoka.widget;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;

public class HeaderExpandableAdapter implements ExpandableListAdapter {

	private final ExpandableListAdapter mAdapter; 
	private final View mHeaderView; 
	
	public HeaderExpandableAdapter(ExpandableListAdapter adapter, View view) { 
		mAdapter = adapter; 
		mHeaderView = view; 
	}
	
	@Override 
	public void registerDataSetObserver(DataSetObserver observer) { 
		mAdapter.registerDataSetObserver(observer); 
	}
	
	@Override 
	public void unregisterDataSetObserver(DataSetObserver observer) { 
		mAdapter.unregisterDataSetObserver(observer); 
	}
	
	public int getHeadersCount() { 
		return mHeaderView != null ? 1 : 0; 
	}
	
	@Override 
	public int getGroupCount() { 
		return getHeadersCount() + mAdapter.getGroupCount(); 
	}
	
	@Override 
	public int getChildrenCount(int groupPosition) { 
		final int headersCount = getHeadersCount(); 
		
		if (groupPosition < headersCount) 
			return 0; 
		
		return mAdapter.getChildrenCount(groupPosition - headersCount); 
	}
	
	@Override 
	public Object getGroup(int groupPosition) { 
		final int headersCount = getHeadersCount(); 
		
		if (groupPosition < headersCount) 
			return null; 
		
		return mAdapter.getGroup(groupPosition - headersCount); 
	}
	
	@Override 
	public Object getChild(int groupPosition, int childPosition) { 
		final int headersCount = getHeadersCount(); 
		
		if (groupPosition < headersCount) 
			return null; 
		
		return mAdapter.getChild(groupPosition - headersCount, childPosition); 
	}
	
	@Override 
	public long getGroupId(int groupPosition) { 
		return groupPosition; 
	}
	
	@Override 
	public long getChildId(int groupPosition, int childPosition) { 
		final int headersCount = getHeadersCount(); 
		
		if (groupPosition < headersCount) 
			return childPosition; 
		
		return mAdapter.getChildId(groupPosition - headersCount, childPosition); 
	}
	
	@Override 
	public boolean hasStableIds() { 
		return mAdapter.hasStableIds(); 
	}
	
	@Override 
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) { 
		final int headersCount = getHeadersCount(); 
		
		if (groupPosition < headersCount) 
			return mHeaderView; 
		
		return mAdapter.getGroupView(groupPosition - headersCount, isExpanded, convertView, parent); 
	}
	
	@Override 
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
            View convertView, ViewGroup parent) { 
		final int headersCount = getHeadersCount(); 
		
		if (groupPosition < headersCount) 
			return null; 
		
		return mAdapter.getChildView(groupPosition - headersCount, 
				childPosition, isLastChild, convertView, parent); 
	}
	
	@Override 
	public boolean isChildSelectable(int groupPosition, int childPosition) { 
		final int headersCount = getHeadersCount(); 
		
		if (groupPosition < headersCount) 
			return false; 
		
		return mAdapter.isChildSelectable(groupPosition - headersCount, childPosition); 
	}
	
	@Override 
	public boolean areAllItemsEnabled() { 
		return mAdapter.areAllItemsEnabled(); 
	}
	
	@Override 
	public boolean isEmpty() { 
		return mAdapter.isEmpty(); 
	}
	
	@Override 
	public void onGroupExpanded(int groupPosition) { 
		final int headersCount = getHeadersCount(); 
		
		if (groupPosition < headersCount) 
			return; 
		
		mAdapter.onGroupExpanded(groupPosition - headersCount); 
	}
	
	@Override 
	public void onGroupCollapsed(int groupPosition) { 
		final int headersCount = getHeadersCount(); 
		
		if (groupPosition < headersCount) 
			return; 
		
		mAdapter.onGroupCollapsed(groupPosition - headersCount); 
	}
	
	@Override 
	public long getCombinedChildId(long groupId, long childId) { 
		return 0x8000000000000000L | ((groupId & 0x7FFFFFFF) << 32) | (childId & 0xFFFFFFFF);
	}
	
	@Override 
	public long getCombinedGroupId(long groupId) { 
		return (groupId & 0x7FFFFFFF) << 32;
	}
	
}
