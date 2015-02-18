package org.javenstudio.provider.publish.discuss;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.ListDataSetCursor;

public class TopicCursor extends ListDataSetCursor<TopicItem> {

	public TopicCursor() {}
	
	@Override 
	protected void onDataSetted(AbstractDataSet<TopicItem> data, int position) { 
		super.onDataSetted(data, position); 
		
		if (data != null && data instanceof TopicDataSet) 
			addTopicDataSet((TopicDataSet)data); 
	}
	
	@Override 
	protected void onDataAdded(AbstractDataSet<TopicItem> data) { 
		super.onDataAdded(data); 
		
		if (data != null && data instanceof TopicDataSet) 
			addTopicDataSet((TopicDataSet)data); 
	}
	
	private void addTopicDataSet(TopicDataSet dataSet) { 
		if (dataSet != null) { 
			TopicItem data = dataSet.getTopicItem(); 
			if (data != null) { 
				
			} 
		}
	}
	
}
