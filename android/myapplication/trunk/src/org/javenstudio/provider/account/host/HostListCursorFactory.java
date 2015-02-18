package org.javenstudio.provider.account.host;

import org.javenstudio.cocoka.widget.adapter.IDataSetCursor;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;

public class HostListCursorFactory implements IDataSetCursorFactory<HostListItem> {

	public HostListCursorFactory() {}
	
	public HostListCursor createCursor() { 
		return new HostListCursor(); 
	}
	
	@Override 
	public final IDataSetCursor<HostListItem> create() { 
		return createCursor(); 
	}
	
}
