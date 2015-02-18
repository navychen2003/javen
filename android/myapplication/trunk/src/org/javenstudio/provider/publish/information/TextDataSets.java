package org.javenstudio.provider.publish.information;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.AbstractDataSets;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;
import org.javenstudio.cocoka.widget.adapter.IDataSetObject;
import org.javenstudio.common.util.Logger;

public class TextDataSets extends AbstractDataSets<TextItem> {
	private static final Logger LOG = Logger.getLogger(TextDataSets.class);
	
	private int mFirstVisibleItem = -1;
	
	public TextDataSets(TextCursorFactory factory) {
		super(factory); 
	}
	
	@Override 
	protected AbstractDataSets<TextItem> createDataSets(IDataSetCursorFactory<TextItem> factory) { 
		return new TextDataSets((TextCursorFactory)factory); 
	}
	
	@Override 
	protected AbstractDataSet<TextItem> createDataSet(IDataSetObject data) { 
		return new TextDataSet(this, (TextItem)data); 
	}
	
	public TextDataSet getTextDataSet(int position) { 
		return (TextDataSet)getDataSet(position); 
	}
	
	public TextDataSet getTextDataSet(String location) { 
		return getTextCursor().getTextDataSet(location); 
	}
	
	public TextCursor getTextCursor() { 
		return (TextCursor)getCursor(); 
	}
	
	public TextItem getTextItemAt(int position) { 
		TextDataSet dataSet = getTextDataSet(position); 
		if (dataSet != null) 
			return dataSet.getTextItem(); 
		
		return null; 
	}
	
	public void addTextItem(TextItem item, boolean notify) { 
		addData(item, notify);
	}
	
	@Override
	public void clear() {
		for (int i=0; i < getCount(); i++) { 
			TextItem item = getTextItemAt(i);
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
