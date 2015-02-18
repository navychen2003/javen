package org.javenstudio.provider.account.list;

import org.javenstudio.cocoka.widget.adapter.IDataSetCursor;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;

public class AccountListCursorFactory implements IDataSetCursorFactory<AccountListItem> {

	public AccountListCursorFactory() {}
	
	public AccountListCursor createCursor() { 
		return new AccountListCursor(); 
	}
	
	@Override 
	public final IDataSetCursor<AccountListItem> create() { 
		return createCursor(); 
	}
	
}
