package org.javenstudio.provider.account.dashboard;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.ListDataSetCursor;

public class DashboardCursor extends ListDataSetCursor<DashboardItem> {

	public DashboardCursor() {}
	
	@Override 
	protected void onDataSetted(AbstractDataSet<DashboardItem> data, int position) { 
		super.onDataSetted(data, position); 
		
		if (data != null && data instanceof DashboardDataSet) 
			addDashboardDataSet((DashboardDataSet)data); 
	}
	
	@Override 
	protected void onDataAdded(AbstractDataSet<DashboardItem> data) { 
		super.onDataAdded(data); 
		
		if (data != null && data instanceof DashboardDataSet) 
			addDashboardDataSet((DashboardDataSet)data); 
	}
	
	private void addDashboardDataSet(DashboardDataSet dataSet) { 
		if (dataSet != null) { 
			DashboardItem data = dataSet.getDashboardItem(); 
			if (data != null) { 
				
			} 
		}
	}
	
}
