package org.javenstudio.provider.people.group;

import android.view.View;

public class GroupInfoItem extends GroupItem {

	private final GroupInfoProvider mProvider;
	
	public GroupInfoItem(GroupInfoProvider p, IGroupData data) { 
		super(data);
		mProvider = p;
	}
	
	public GroupInfoProvider getProvider() { return mProvider; }
	
	@Override
	protected void onUpdateViewsOnVisible(boolean restartSlide) { 
		GroupBinder binder = getGroupBinder();
		if (binder != null) binder.onUpdateViews(this);
	}
	
	private GroupBinder getGroupBinder() { 
		final GroupBinder binder = (GroupBinder)getProvider().getBinder();
		final View view = getBindView();
		
		if (binder == null || view == null) 
			return null;
		
		return binder;
	}
	
}
