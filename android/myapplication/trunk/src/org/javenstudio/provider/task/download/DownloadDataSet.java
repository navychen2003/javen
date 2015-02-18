package org.javenstudio.provider.task.download;

import android.view.View;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;

public class DownloadDataSet extends AbstractDataSet<DownloadItem> {

	public DownloadDataSet(DownloadDataSets dataSets, DownloadItem data) {
		super(dataSets, data); 
	}
	
	@Override 
	public boolean isEnabled() { 
		return false; //getDownloadItem().getProvider().getOnItemClickListener() != null; 
	}
	
	@Override
	public void setBindedView(View view) {
	}

	@Override
	public View getBindedView() {
		return null;
	}
	
	public DownloadItem getDownloadItem() { 
		return (DownloadItem)getObject(); 
	}
	
}
