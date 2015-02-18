package org.javenstudio.provider.account.notify;

import org.javenstudio.android.account.AccountUser;

public interface INotifySource {

	public AccountUser getAccountUser();
	public ISystemNotifyData getSystemNotifyData();
	public IInviteNotifyData getInviteNotifyData();
	public IMessageNotifyData getMessageNotifyData();
	public IAnnouncementNotifyData getAnnouncementNotifyData();
	
}
