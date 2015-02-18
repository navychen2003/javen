package org.javenstudio.provider.account.host;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.ListDataSetCursor;

public class HostListCursor extends ListDataSetCursor<HostListItem> {

	public HostListCursor() {}
	
	@Override 
	protected void onDataSetted(AbstractDataSet<HostListItem> data, int position) { 
		super.onDataSetted(data, position); 
		
		if (data != null && data instanceof HostListDataSet) 
			addHostListDataSet((HostListDataSet)data); 
	}
	
	@Override 
	protected void onDataAdded(AbstractDataSet<HostListItem> data) { 
		super.onDataAdded(data); 
		
		if (data != null && data instanceof HostListDataSet) 
			addHostListDataSet((HostListDataSet)data); 
	}
	
	private void addHostListDataSet(HostListDataSet dataSet) { 
		if (dataSet != null) { 
			HostListItem data = dataSet.getHostListItem(); 
			if (data != null) { 
				
			} 
		}
	}
	
}
