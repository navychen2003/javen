package org.javenstudio.provider.account.list;

import android.view.View;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;

public class AccountListDataSet extends AbstractDataSet<AccountListItem> {

	public AccountListDataSet(AccountListDataSets dataSets, AccountListItem data) {
		super(dataSets, data); 
	}
	
	@Override 
	public boolean isEnabled() { 
		return false; //getAccountListItem().getProvider().getOnItemClickListener() != null; 
	}
	
	@Override
	public void setBindedView(View view) {
	}

	@Override
	public View getBindedView() {
		return null;
	}
	
	public AccountListItem getAccountListItem() { 
		return (AccountListItem)getObject(); 
	}
	
}
