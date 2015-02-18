package org.javenstudio.provider.library.select;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.ListDataSetCursor;

public class SelectListCursor extends ListDataSetCursor<SelectListItem> {

	public SelectListCursor() {}
	
	@Override 
	protected void onDataSetted(AbstractDataSet<SelectListItem> data, int position) { 
		super.onDataSetted(data, position); 
		
		if (data != null && data instanceof SelectListDataSet) 
			addSelectListDataSet((SelectListDataSet)data); 
	}
	
	@Override 
	protected void onDataAdded(AbstractDataSet<SelectListItem> data) { 
		super.onDataAdded(data); 
		
		if (data != null && data instanceof SelectListDataSet) 
			addSelectListDataSet((SelectListDataSet)data); 
	}
	
	private void addSelectListDataSet(SelectListDataSet dataSet) { 
		if (dataSet != null) { 
			SelectListItem data = dataSet.getSelectListItem(); 
			if (data != null) { 
				
			} 
		}
	}
	
}
