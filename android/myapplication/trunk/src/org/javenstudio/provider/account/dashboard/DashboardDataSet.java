package org.javenstudio.provider.account.dashboard;

import android.view.View;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;

public class DashboardDataSet extends AbstractDataSet<DashboardItem> {

	public DashboardDataSet(DashboardDataSets dataSets, DashboardItem data) {
		super(dataSets, data); 
	}
	
	@Override 
	public boolean isEnabled() { 
		return false; //getDashboardItem().getProvider().getOnItemClickListener() != null; 
	}
	
	@Override
	public void setBindedView(View view) {
	}

	@Override
	public View getBindedView() {
		return null;
	}
	
	public DashboardItem getDashboardItem() { 
		return (DashboardItem)getObject(); 
	}
	
}
