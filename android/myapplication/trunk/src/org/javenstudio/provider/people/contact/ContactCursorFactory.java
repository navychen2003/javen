package org.javenstudio.provider.people.contact;

import org.javenstudio.cocoka.widget.adapter.IDataSetCursor;
import org.javenstudio.cocoka.widget.adapter.IDataSetCursorFactory;

public class ContactCursorFactory implements IDataSetCursorFactory<ContactItem> {

	public ContactCursorFactory() {}
	
	public ContactCursor createCursor() { 
		return new ContactCursor(); 
	}
	
	@Override 
	public final IDataSetCursor<ContactItem> create() { 
		return createCursor(); 
	}
	
}
