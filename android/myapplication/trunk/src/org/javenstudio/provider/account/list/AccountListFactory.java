package org.javenstudio.provider.account.list;

import java.util.Arrays;
import java.util.Comparator;

import org.javenstudio.android.account.AccountApp;
import org.javenstudio.android.entitydb.content.AccountData;

public abstract class AccountListFactory {
	
	public AccountListDataSets createAccountListDataSets(AccountListProvider provider) {
		return new AccountListDataSets(new AccountListCursorFactory());
	}
	
	public AccountListBinder createAccountListBinder(AccountListProvider provider) {
		return new AccountListBinder(provider);
	}
	
	public void updateAccountListDataSets(AccountApp app, AccountListDataSets dataSets) {
		if (app == null || dataSets == null) return;
		dataSets.clear();
		
		final AccountData[] accounts = app.getAccounts();
		if (accounts != null) {
			Arrays.sort(accounts, new Comparator<AccountData>() {
				@Override
				public int compare(AccountData lhs, AccountData rhs) {
					long ltm = lhs.getUpdateTime();
					long rtm = rhs.getUpdateTime();
					if (ltm > rtm) return -1;
					else if (ltm < rtm) return 1;
					
					String lname = lhs.getUserName();
					String rname = lhs.getUserName();
					if (lname == null || rname == null) {
						if (lname == null) return 1;
						else if (rname == null) return -1;
					}
					return lname.compareTo(rname);
				}
			});
			
			for (AccountData account : accounts) {
				if (account != null) {
					AccountListItem item = createAccountListItem(app, account);
					dataSets.addData(item, false);
					
					//if (LOG.isDebugEnabled())
					//	LOG.debug("getListAdapter: accountItem=" + item);
				}
			}
		}
	}
	
	public abstract AccountListItem createAccountListItem(AccountApp app, AccountData account);
	
}
