package org.javenstudio.provider.account.notify;

import android.view.View;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;

public class NotifyDataSet extends AbstractDataSet<NotifyItem> {

	public NotifyDataSet(NotifyDataSets dataSets, NotifyItem data) {
		super(dataSets, data); 
	}
	
	@Override 
	public boolean isEnabled() { 
		return false; //getNotifyItem().getProvider().getOnItemClickListener() != null; 
	}
	
	@Override
	public void setBindedView(View view) {
	}

	@Override
	public View getBindedView() {
		return null;
	}
	
	public NotifyItem getNotifyItem() { 
		return (NotifyItem)getObject(); 
	}
	
}
