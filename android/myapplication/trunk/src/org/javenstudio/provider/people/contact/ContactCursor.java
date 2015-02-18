package org.javenstudio.provider.people.contact;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;
import org.javenstudio.cocoka.widget.adapter.ListDataSetCursor;

public class ContactCursor extends ListDataSetCursor<ContactItem> {

	public ContactCursor() {}
	
	@Override 
	protected void onDataSetted(AbstractDataSet<ContactItem> data, int position) { 
		super.onDataSetted(data, position); 
		
		if (data != null && data instanceof ContactDataSet) 
			addContactDataSet((ContactDataSet)data); 
	}
	
	@Override 
	protected void onDataAdded(AbstractDataSet<ContactItem> data) { 
		super.onDataAdded(data); 
		
		if (data != null && data instanceof ContactDataSet) 
			addContactDataSet((ContactDataSet)data); 
	}
	
	private void addContactDataSet(ContactDataSet dataSet) { 
		if (dataSet != null) { 
			ContactItem data = dataSet.getContactItem(); 
			if (data != null) { 
				
			} 
		}
	}
	
}
