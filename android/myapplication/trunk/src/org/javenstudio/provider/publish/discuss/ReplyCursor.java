package org.javenstudio.provider.publish.discuss;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.ListDataSetCursor;

public class ReplyCursor extends ListDataSetCursor<ReplyItem> {

	public ReplyCursor() {}
	
	@Override 
	protected void onDataSetted(AbstractDataSet<ReplyItem> data, int position) { 
		super.onDataSetted(data, position); 
		
		if (data != null && data instanceof ReplyDataSet) 
			addReplyDataSet((ReplyDataSet)data); 
	}
	
	@Override 
	protected void onDataAdded(AbstractDataSet<ReplyItem> data) { 
		super.onDataAdded(data); 
		
		if (data != null && data instanceof ReplyDataSet) 
			addReplyDataSet((ReplyDataSet)data); 
	}
	
	private void addReplyDataSet(ReplyDataSet dataSet) { 
		if (dataSet != null) { 
			ReplyItem data = dataSet.getReplyItem(); 
			if (data != null) { 
				
			} 
		}
	}
	
}
