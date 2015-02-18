package org.javenstudio.provider.account.dashboard;

import org.javenstudio.cocoka.widget.adapter.IDataSetCursor;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;

public class DashboardCursorFactory implements IDataSetCursorFactory<DashboardItem> {

	public DashboardCursorFactory() {}
	
	public DashboardCursor createCursor() { 
		return new DashboardCursor(); 
	}
	
	@Override 
	public final IDataSetCursor<DashboardItem> create() { 
		return createCursor(); 
	}
	
}
