package org.javenstudio.provider.library.section;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.ListDataSetCursor;

public class SectionListCursor extends ListDataSetCursor<SectionListItem> {

	public SectionListCursor() {}
	
	@Override 
	protected void onDataSetted(AbstractDataSet<SectionListItem> data, int position) { 
		super.onDataSetted(data, position); 
		
		if (data != null && data instanceof SectionListDataSet) 
			addSectionListDataSet((SectionListDataSet)data); 
	}
	
	@Override 
	protected void onDataAdded(AbstractDataSet<SectionListItem> data) { 
		super.onDataAdded(data); 
		
		if (data != null && data instanceof SectionListDataSet) 
			addSectionListDataSet((SectionListDataSet)data); 
	}
	
	private void addSectionListDataSet(SectionListDataSet dataSet) { 
		if (dataSet != null) { 
			SectionListItem data = dataSet.getSectionListItem(); 
			if (data != null) { 
				
			} 
		}
	}
	
}
