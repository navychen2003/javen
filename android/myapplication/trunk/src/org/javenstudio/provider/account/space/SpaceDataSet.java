package org.javenstudio.provider.account.space;

import android.view.View;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;

public class SpaceDataSet extends AbstractDataSet<SpaceItem> {

	public SpaceDataSet(SpaceDataSets dataSets, SpaceItem data) {
		super(dataSets, data); 
	}
	
	@Override 
	public boolean isEnabled() { 
		return false; //getSpaceItem().getProvider().getOnItemClickListener() != null; 
	}
	
	@Override
	public void setBindedView(View view) {
	}

	@Override
	public View getBindedView() {
		return null;
	}
	
	public SpaceItem getSpaceItem() { 
		return (SpaceItem)getObject(); 
	}
	
}
