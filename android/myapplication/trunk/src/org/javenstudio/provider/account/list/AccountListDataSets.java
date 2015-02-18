package org.javenstudio.provider.account.list;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.AbstractDataSets;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;
import org.javenstudio.cocoka.widget.adapter.IDataSetObject;
import org.javenstudio.common.util.Logger;

public class AccountListDataSets extends AbstractDataSets<AccountListItem> {
	private static final Logger LOG = Logger.getLogger(AccountListDataSets.class);
	
	private int mFirstVisibleItem = -1;
	
	public AccountListDataSets() {
		this(new AccountListCursorFactory());
	}
	
	public AccountListDataSets(AccountListCursorFactory factory) {
		super(factory); 
	}
	
	@Override 
	protected AbstractDataSets<AccountListItem> createDataSets(IDataSetCursorFactory<AccountListItem> factory) { 
		return new AccountListDataSets((AccountListCursorFactory)factory); 
	}
	
	@Override 
	protected AbstractDataSet<AccountListItem> createDataSet(IDataSetObject data) { 
		return new AccountListDataSet(this, (AccountListItem)data); 
	}
	
	public AccountListDataSet getAccountListDataSet(int position) { 
		return (AccountListDataSet)getDataSet(position); 
	}
	
	public AccountListCursor getAccountListItemCursor() { 
		return (AccountListCursor)getCursor(); 
	}
	
	public AccountListItem getAccountListItemAt(int position) { 
		AccountListDataSet dataSet = getAccountListDataSet(position); 
		if (dataSet != null) 
			return dataSet.getAccountListItem(); 
		
		return null; 
	}
	
	public void addAccountListItem(AccountListItem... items) { 
		addDataList(items);
	}
	
	@Override
	public void clear() {
		for (int i=0; i < getCount(); i++) { 
			AccountListItem item = getAccountListItemAt(i);
			if (item != null) 
				item.onRemove();
		}
		
		mFirstVisibleItem = -1;
		super.clear();
	}
	
	final void setFirstVisibleItem(int item) { 
		if (LOG.isDebugEnabled())
			LOG.debug("setFirstVisibleItem: firstItem=" + item);
		
		mFirstVisibleItem = item; 
	}
	
	final int getFirstVisibleItem() { return mFirstVisibleItem; }
	
}
