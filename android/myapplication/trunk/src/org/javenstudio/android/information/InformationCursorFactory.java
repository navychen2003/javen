package org.javenstudio.android.information;

import org.javenstudio.cocoka.widget.adapter.IDataSetCursor;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;

public class InformationCursorFactory implements IDataSetCursorFactory<Information> {

	public InformationCursorFactory() {}
	
	public InformationCursor createCursor() { 
		return new InformationCursor(); 
	}
	
	@Override 
	public final IDataSetCursor<Information> create() { 
		return createCursor(); 
	}
	
}
