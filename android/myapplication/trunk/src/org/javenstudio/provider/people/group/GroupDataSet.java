package org.javenstudio.provider.people.group;

import android.view.View;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;

public class GroupDataSet extends AbstractDataSet<GroupItem> {

	public GroupDataSet(GroupDataSets dataSets, GroupItem data) {
		super(dataSets, data); 
	}
	
	@Override 
	public boolean isEnabled() { 
		return false; //getGroupItem().getProvider().getOnItemClickListener() != null; 
	}
	
	@Override
	public void setBindedView(View view) {
	}

	@Override
	public View getBindedView() {
		return null;
	}
	
	public GroupItem getGroupItem() { 
		return (GroupItem)getObject(); 
	}
	
}
