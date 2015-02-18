package org.javenstudio.provider.account.host;

import android.view.View;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;

public class HostListDataSet extends AbstractDataSet<HostListItem> {

	public HostListDataSet(HostListDataSets dataSets, HostListItem data) {
		super(dataSets, data); 
	}
	
	@Override 
	public boolean isEnabled() { 
		return getData().isEnabled();
	}

	@Override
	public void setBindedView(View view) {
	}

	@Override
	public View getBindedView() {
		return null;
	}
	
	public HostListItem getHostListItem() { 
		return (HostListItem)getObject(); 
	}
	
}
