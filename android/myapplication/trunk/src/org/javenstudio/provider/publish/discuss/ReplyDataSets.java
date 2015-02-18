package org.javenstudio.provider.publish.discuss;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.AbstractDataSets;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;
import org.javenstudio.cocoka.widget.adapter.IDataSetObject;
import org.javenstudio.common.util.Logger;

public class ReplyDataSets extends AbstractDataSets<ReplyItem> {
	private static final Logger LOG = Logger.getLogger(ReplyDataSets.class);
	
	private int mFirstVisibleItem = -1;
	
	public ReplyDataSets(ReplyCursorFactory factory) {
		super(factory); 
	}
	
	@Override 
	protected AbstractDataSets<ReplyItem> createDataSets(IDataSetCursorFactory<ReplyItem> factory) { 
		return new ReplyDataSets((ReplyCursorFactory)factory); 
	}
	
	@Override 
	protected AbstractDataSet<ReplyItem> createDataSet(IDataSetObject data) { 
		return new ReplyDataSet(this, (ReplyItem)data); 
	}
	
	public ReplyDataSet getReplyDataSet(int position) { 
		return (ReplyDataSet)getDataSet(position); 
	}
	
	public ReplyCursor getReplyItemCursor() { 
		return (ReplyCursor)getCursor(); 
	}
	
	public ReplyItem getReplyItemAt(int position) { 
		ReplyDataSet dataSet = getReplyDataSet(position); 
		if (dataSet != null) 
			return dataSet.getReplyItem(); 
		
		return null; 
	}
	
	public void addReplyItem(ReplyItem item, boolean notify) { 
		addData(item, notify);
	}
	
	@Override
	public void clear() {
		for (int i=0; i < getCount(); i++) { 
			ReplyItem item = getReplyItemAt(i);
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
