package org.javenstudio.provider.account.space;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.ListDataSetCursor;

public class SpaceCursor extends ListDataSetCursor<SpaceItem> {

	public SpaceCursor() {}
	
	@Override 
	protected void onDataSetted(AbstractDataSet<SpaceItem> data, int position) { 
		super.onDataSetted(data, position); 
		
		if (data != null && data instanceof SpaceDataSet) 
			addSpaceDataSet((SpaceDataSet)data); 
	}
	
	@Override 
	protected void onDataAdded(AbstractDataSet<SpaceItem> data) { 
		super.onDataAdded(data); 
		
		if (data != null && data instanceof SpaceDataSet) 
			addSpaceDataSet((SpaceDataSet)data); 
	}
	
	private void addSpaceDataSet(SpaceDataSet dataSet) { 
		if (dataSet != null) { 
			SpaceItem data = dataSet.getSpaceItem(); 
			if (data != null) { 
				
			} 
		}
	}
	
}
