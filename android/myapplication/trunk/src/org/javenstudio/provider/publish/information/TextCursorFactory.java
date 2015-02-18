package org.javenstudio.provider.publish.information;

import org.javenstudio.cocoka.widget.adapter.IDataSetCursor;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;

public class TextCursorFactory implements IDataSetCursorFactory<TextItem> {

	public TextCursorFactory() {}
	
	public TextCursor createCursor() { 
		return new TextCursor(); 
	}
	
	@Override 
	public final IDataSetCursor<TextItem> create() { 
		return createCursor(); 
	}
	
}
