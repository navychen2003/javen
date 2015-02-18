package org.javenstudio.provider.task.upload;

import android.view.View;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;

public class UploadDataSet extends AbstractDataSet<UploadItem> {

	public UploadDataSet(UploadDataSets dataSets, UploadItem data) {
		super(dataSets, data); 
	}
	
	@Override 
	public boolean isEnabled() { 
		return false; //getUploadItem().getProvider().getOnItemClickListener() != null; 
	}
	
	@Override
	public void setBindedView(View view) {
	}

	@Override
	public View getBindedView() {
		return null;
	}
	
	public UploadItem getUploadItem() { 
		return (UploadItem)getObject(); 
	}
	
}
