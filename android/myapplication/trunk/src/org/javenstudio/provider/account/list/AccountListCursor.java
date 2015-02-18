package org.javenstudio.provider.account.list;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.ListDataSetCursor;

public class AccountListCursor extends ListDataSetCursor<AccountListItem> {

	public AccountListCursor() {}
	
	@Override 
	protected void onDataSetted(AbstractDataSet<AccountListItem> data, int position) { 
		super.onDataSetted(data, position); 
		
		if (data != null && data instanceof AccountListDataSet) 
			addAccountListDataSet((AccountListDataSet)data); 
	}
	
	@Override 
	protected void onDataAdded(AbstractDataSet<AccountListItem> data) { 
		super.onDataAdded(data); 
		
		if (data != null && data instanceof AccountListDataSet) 
			addAccountListDataSet((AccountListDataSet)data); 
	}
	
	private void addAccountListDataSet(AccountListDataSet dataSet) { 
		if (dataSet != null) { 
			AccountListItem data = dataSet.getAccountListItem(); 
			if (data != null) { 
				
			} 
		}
	}
	
}
