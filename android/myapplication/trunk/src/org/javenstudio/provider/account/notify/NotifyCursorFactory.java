package org.javenstudio.provider.account.notify;

import org.javenstudio.cocoka.widget.adapter.IDataSetCursor;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;

public class NotifyCursorFactory implements IDataSetCursorFactory<NotifyItem> {

	public NotifyCursorFactory() {}
	
	public NotifyCursor createCursor() { 
		return new NotifyCursor(); 
	}
	
	@Override 
	public final IDataSetCursor<NotifyItem> create() { 
		return createCursor(); 
	}
	
}
