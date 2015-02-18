package org.javenstudio.provider.people.contact;

import android.view.View;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSet;

public class ContactDataSet extends AbstractDataSet<ContactItem> {

	public ContactDataSet(ContactDataSets dataSets, ContactItem data) {
		super(dataSets, data); 
	}
	
	@Override 
	public boolean isEnabled() { 
		return false; //getContactItem().getProvider().getOnItemClickListener() != null; 
	}
	
	@Override
	public void setBindedView(View view) {
	}

	@Override
	public View getBindedView() {
		return null;
	}
	
	public ContactItem getContactItem() { 
		return (ContactItem)getObject(); 
	}
	
}
