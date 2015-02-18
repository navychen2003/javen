package org.javenstudio.provider.account.dashboard;

public abstract class DashboardFactory {
	
	public DashboardDataSets createDashboardDataSets(DashboardProvider p) { 
		return new DashboardDataSets(new DashboardCursorFactory());
	}
	
	public abstract DashboardBinder createDashboardBinder(DashboardProvider p);
	public abstract DashboardItem createEmptyItem(DashboardProvider p);
	
}
