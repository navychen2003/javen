package org.javenstudio.provider.account.notify;

import org.javenstudio.android.account.AccountUser;

public interface OnNotifyChangeListener {

	public void onNotifyChanged(AccountUser user, int count);
	
}
