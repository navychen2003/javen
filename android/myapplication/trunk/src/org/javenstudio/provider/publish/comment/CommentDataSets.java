package org.javenstudio.provider.publish.comment;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.AbstractDataSets;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;
import org.javenstudio.cocoka.widget.adapter.IDataSetObject;
import org.javenstudio.common.util.Logger;

public class CommentDataSets extends AbstractDataSets<CommentItem> {
	private static final Logger LOG = Logger.getLogger(CommentDataSets.class);
	
	private int mFirstVisibleItem = -1;
	
	public CommentDataSets(CommentCursorFactory factory) {
		super(factory); 
	}
	
	@Override 
	protected AbstractDataSets<CommentItem> createDataSets(IDataSetCursorFactory<CommentItem> factory) { 
		return new CommentDataSets((CommentCursorFactory)factory); 
	}
	
	@Override 
	protected AbstractDataSet<CommentItem> createDataSet(IDataSetObject data) { 
		return new CommentDataSet(this, (CommentItem)data); 
	}
	
	public CommentDataSet getCommentDataSet(int position) { 
		return (CommentDataSet)getDataSet(position); 
	}
	
	public CommentCursor getCommentItemCursor() { 
		return (CommentCursor)getCursor(); 
	}
	
	public CommentItem getCommentItemAt(int position) { 
		CommentDataSet dataSet = getCommentDataSet(position); 
		if (dataSet != null) 
			return dataSet.getCommentItem(); 
		
		return null; 
	}
	
	public void addCommentItem(CommentItem item, boolean notify) { 
		addData(item, notify);
	}
	
	@Override
	public void clear() {
		for (int i=0; i < getCount(); i++) { 
			CommentItem item = getCommentItemAt(i);
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
