package org.javenstudio.provider.library.select;

import android.view.View;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;

public class SelectListDataSet extends AbstractDataSet<SelectListItem> {

	public SelectListDataSet(SelectListDataSets dataSets, SelectListItem data) {
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
	
	public SelectListItem getSelectListItem() { 
		return (SelectListItem)getObject(); 
	}
	
}
