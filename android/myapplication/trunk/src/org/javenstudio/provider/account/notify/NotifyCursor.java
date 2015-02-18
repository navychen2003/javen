package org.javenstudio.provider.account.notify;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.ListDataSetCursor;

public class NotifyCursor extends ListDataSetCursor<NotifyItem> {

	public NotifyCursor() {}
	
	@Override 
	protected void onDataSetted(AbstractDataSet<NotifyItem> data, int position) { 
		super.onDataSetted(data, position); 
		
		if (data != null && data instanceof NotifyDataSet) 
			addNotifyDataSet((NotifyDataSet)data); 
	}
	
	@Override 
	protected void onDataAdded(AbstractDataSet<NotifyItem> data) { 
		super.onDataAdded(data); 
		
		if (data != null && data instanceof NotifyDataSet) 
			addNotifyDataSet((NotifyDataSet)data); 
	}
	
	private void addNotifyDataSet(NotifyDataSet dataSet) { 
		if (dataSet != null) { 
			NotifyItem data = dataSet.getNotifyItem(); 
			if (data != null) { 
				
			} 
		}
	}
	
}
