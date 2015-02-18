package org.javenstudio.provider.publish.information;

import java.util.HashMap;
import java.util.Map;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.ListDataSetCursor;

public class TextCursor extends ListDataSetCursor<TextItem> {

	private final Map<String, TextDataSet> mTextItems; 
	
	public TextCursor() {
		mTextItems = new HashMap<String, TextDataSet>(); 
	} 
	
	@Override 
	protected void onDataSetted(AbstractDataSet<TextItem> data, int position) { 
		super.onDataSetted(data, position); 
		
		if (data != null && data instanceof TextDataSet) 
			addTextDataSet((TextDataSet)data); 
	}
	
	@Override 
	protected void onDataAdded(AbstractDataSet<TextItem> data) { 
		super.onDataAdded(data); 
		
		if (data != null && data instanceof TextDataSet) 
			addTextDataSet((TextDataSet)data); 
	}
	
	private void addTextDataSet(TextDataSet dataSet) { 
		if (dataSet != null) { 
			TextItem data = dataSet.getTextItem(); 
			if (data != null) { 
				//IImageList list = album.getImageList(); 
				//if (list != null) { 
				//	synchronized (this) { 
				//		mTextItems.put(list.getLocation(), dataSet); 
				//	}
				//} 
			} 
		}
	}
	
	public TextDataSet getTextDataSet(String location) { 
		synchronized (this) { 
			return location != null ? mTextItems.get(location) : null; 
		}
	}
	
	@Override 
	protected void onCleared() { 
		super.onCleared(); 
		
		synchronized (this) { 
			mTextItems.clear(); 
		}
	}
	
}
