package org.javenstudio.provider.account.notify;

public abstract class NotifyFactory {

	public NotifyDataSets createNotifyDataSets(NotifyProvider p) { 
		return new NotifyDataSets(new NotifyCursorFactory());
	}
	
	public abstract NotifyBinder createNotifyBinder(NotifyProvider p);
	
}
