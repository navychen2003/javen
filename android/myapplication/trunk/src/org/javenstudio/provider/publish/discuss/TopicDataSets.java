package org.javenstudio.provider.publish.discuss;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.AbstractDataSets;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;
import org.javenstudio.cocoka.widget.adapter.IDataSetObject;
import org.javenstudio.common.util.Logger;

public class TopicDataSets extends AbstractDataSets<TopicItem> {
	private static final Logger LOG = Logger.getLogger(TopicDataSets.class);
	
	private int mFirstVisibleItem = -1;
	
	public TopicDataSets(TopicCursorFactory factory) {
		super(factory); 
	}
	
	@Override 
	protected AbstractDataSets<TopicItem> createDataSets(IDataSetCursorFactory<TopicItem> factory) { 
		return new TopicDataSets((TopicCursorFactory)factory); 
	}
	
	@Override 
	protected AbstractDataSet<TopicItem> createDataSet(IDataSetObject data) { 
		return new TopicDataSet(this, (TopicItem)data); 
	}
	
	public TopicDataSet getTopicDataSet(int position) { 
		return (TopicDataSet)getDataSet(position); 
	}
	
	public TopicCursor getTopicItemCursor() { 
		return (TopicCursor)getCursor(); 
	}
	
	public TopicItem getTopicItemAt(int position) { 
		TopicDataSet dataSet = getTopicDataSet(position); 
		if (dataSet != null) 
			return dataSet.getTopicItem(); 
		
		return null; 
	}
	
	public void addTopicItem(TopicItem item, boolean notify) { 
		addData(item, notify);
	}
	
	@Override
	public void clear() {
		for (int i=0; i < getCount(); i++) { 
			TopicItem item = getTopicItemAt(i);
			if (item != null) 
				item.onRemove();
		}
		
		mFirstVisibleItem = -1;
		super.clear();
	}
	
	final void setFirstVisibleItem(int item) { 
		if (LOG.isDebugEnabled())
			LOG.debug("setFirstVisibleItem: firstItem=" + item);
		
		mFirstVisibleItem = item; 
	}
	
	final int getFirstVisibleItem() { return mFirstVisibleItem; }
	
}
