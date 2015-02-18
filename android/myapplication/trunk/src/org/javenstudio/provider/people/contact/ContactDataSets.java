package org.javenstudio.provider.people.contact;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.AbstractDataSets;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;
import org.javenstudio.cocoka.widget.adapter.IDataSetObject;
import org.javenstudio.common.util.Logger;

public class ContactDataSets extends AbstractDataSets<ContactItem> {
	private static final Logger LOG = Logger.getLogger(ContactDataSets.class);
	
	private int mFirstVisibleItem = -1;
	
	public ContactDataSets(ContactCursorFactory factory) {
		super(factory); 
	}
	
	@Override 
	protected AbstractDataSets<ContactItem> createDataSets(IDataSetCursorFactory<ContactItem> factory) { 
		return new ContactDataSets((ContactCursorFactory)factory); 
	}
	
	@Override 
	protected AbstractDataSet<ContactItem> createDataSet(IDataSetObject data) { 
		return new ContactDataSet(this, (ContactItem)data); 
	}
	
	public ContactDataSet getContactDataSet(int position) { 
		return (ContactDataSet)getDataSet(position); 
	}
	
	public ContactCursor getContactItemCursor() { 
		return (ContactCursor)getCursor(); 
	}
	
	public ContactItem getContactItemAt(int position) { 
		ContactDataSet dataSet = getContactDataSet(position); 
		if (dataSet != null) 
			return dataSet.getContactItem(); 
		
		return null; 
	}
	
	public void addContactItem(ContactItem item, boolean notify) { 
		addData(item, notify);
	}
	
	@Override
	public void clear() {
		for (int i=0; i < getCount(); i++) { 
			ContactItem item = getContactItemAt(i);
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
