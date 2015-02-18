package org.javenstudio.provider.people.group;

import android.view.View;

public class GroupListItem extends GroupItem {

	private final GroupListProvider mProvider;
	
	public GroupListItem(GroupListProvider p, IGroupData data) { 
		super(data);
		mProvider = p;
	}
	
	public GroupListProvider getProvider() { return mProvider; }
	
	@Override
	protected void onUpdateViewsOnVisible(boolean restartSlide) { 
		GroupListBinder binder = getGroupListBinder();
		if (binder != null) binder.onUpdateViews(this);
	}
	
	private GroupListBinder getGroupListBinder() { 
		final GroupListBinder binder = (GroupListBinder)getProvider().getBinder();
		final View view = getBindView();
		
		if (binder == null || view == null) 
			return null;
		
		return binder;
	}
	
}
